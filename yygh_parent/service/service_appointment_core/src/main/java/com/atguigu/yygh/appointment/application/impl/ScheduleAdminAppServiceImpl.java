package com.atguigu.yygh.appointment.application.impl;

import com.atguigu.yygh.appointment.api.command.CreateScheduleCommand;
import com.atguigu.yygh.appointment.api.response.ScheduleResponse;
import com.atguigu.yygh.appointment.application.ScheduleAdminAppService;
import com.atguigu.yygh.appointment.common.exception.AppointmentBizException;
import com.atguigu.yygh.appointment.domain.schedule.model.ApSchedule;
import com.atguigu.yygh.appointment.domain.schedule.model.ApScheduleEvent;
import com.atguigu.yygh.appointment.domain.shared.enums.ScheduleStatus;
import com.atguigu.yygh.appointment.domain.shared.enums.SlotStatus;
import com.atguigu.yygh.appointment.domain.slot.model.ApSlot;
import com.atguigu.yygh.appointment.domain.token.TokenGateService;
import com.atguigu.yygh.appointment.infrastructure.mapper.ApScheduleMapper;
import com.atguigu.yygh.appointment.infrastructure.mapper.ApScheduleEventMapper;
import com.atguigu.yygh.appointment.infrastructure.mapper.ApSlotMapper;
import com.atguigu.yygh.common.trace.TraceContext;
import com.baomidou.mybatisplus.core.toolkit.IdWorker;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Slf4j
@RequiredArgsConstructor
public class ScheduleAdminAppServiceImpl implements ScheduleAdminAppService {

    private final ApScheduleMapper apScheduleMapper;
    private final ApScheduleEventMapper apScheduleEventMapper;
    private final ApSlotMapper apSlotMapper;
    private final TokenGateService tokenGateService;

    @Override
    @Transactional(rollbackFor = Exception.class)
    public ScheduleResponse createSchedule(CreateScheduleCommand command) {
        log.info("开始创建排班, traceId: {}, doctorId: {}, deptId: {}, hospitalId: {}, visitDate: {}, timePeriod: {}",
                TraceContext.getOrCreateTraceId(), command.getDoctorId(), command.getDeptId(),
                command.getHospitalId(), command.getVisitDate(), command.getTimePeriod());
        ApSchedule schedule = new ApSchedule();
        schedule.setScheduleId(IdWorker.getIdStr());
        schedule.setDoctorId(command.getDoctorId());
        schedule.setDeptId(command.getDeptId());
        schedule.setHospitalId(command.getHospitalId());
        schedule.setVisitDate(command.getVisitDate());
        schedule.setTimePeriod(command.getTimePeriod());
        schedule.setTotalCount(command.getTotalCount());
        schedule.setAvailableCount(0);
        schedule.setHeldCount(0);
        schedule.setConfirmedCount(0);
        schedule.setStatus(ScheduleStatus.DRAFT.name());
        schedule.setAllowCancel(command.getAllowCancel());
        schedule.setOpenTime(command.getOpenTime());
        schedule.setCloseTime(command.getCloseTime());
        schedule.setVersion(0);
        apScheduleMapper.insert(schedule);
        recordScheduleEvent(schedule.getScheduleId(), "CREATE", null, ScheduleStatus.DRAFT.name(), "创建排班");
        log.info("创建排班成功, traceId: {}, scheduleId: {}", TraceContext.getOrCreateTraceId(), schedule.getScheduleId());
        return toResponse(schedule);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public ScheduleResponse generateSlots(String scheduleId) {
        ApSchedule schedule = requireSchedule(scheduleId);
        if (!ScheduleStatus.DRAFT.name().equals(schedule.getStatus())) {
            throw new AppointmentBizException("只有草稿排班允许生成号位");
        }
        int existingCount = apSlotMapper.countBySchedule(scheduleId);
        if (existingCount > 0) {
            throw new AppointmentBizException("当前排班已生成号位，禁止重复生成");
        }

        //todo 这里是一个典型的 N+1 问题，后续需要修复
        for (int i = 1; i <= schedule.getTotalCount(); i++) {
            ApSlot slot = new ApSlot();
            slot.setSlotId(IdWorker.getIdStr());
            slot.setScheduleId(scheduleId);
            slot.setSequenceNo(i);
            slot.setStatus(SlotStatus.AVAILABLE.name());
            apSlotMapper.insert(slot);
        }
        apScheduleMapper.initCounts(scheduleId, schedule.getTotalCount());

        ApSchedule refreshed = requireSchedule(scheduleId);
        log.info("排班号位生成完成, traceId: {}, scheduleId: {}, totalCount: {}",
                TraceContext.getOrCreateTraceId(), scheduleId, refreshed.getTotalCount());
        recordScheduleEvent(scheduleId, "GENERATE_SLOTS", ScheduleStatus.DRAFT.name(), ScheduleStatus.DRAFT.name(), "批量生成号位");
        return toResponse(refreshed);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public ScheduleResponse publishSchedule(String scheduleId) {
        ApSchedule schedule = requireSchedule(scheduleId);
        if (!ScheduleStatus.DRAFT.name().equals(schedule.getStatus())) {
            throw new AppointmentBizException("只有草稿排班允许发布");
        }
        int slotCount = apSlotMapper.countBySchedule(scheduleId);
        if (slotCount != schedule.getTotalCount()) {
            throw new AppointmentBizException("号位未准备完成，不能发布排班");
        }

        int updated = apScheduleMapper.updateStatusIfCurrent(
                scheduleId,
                ScheduleStatus.DRAFT.name(),
                ScheduleStatus.OPEN.name()
        );
        if (updated == 0) {
            throw new AppointmentBizException("排班状态更新失败");
        }

        tokenGateService.initScheduleToken(scheduleId, schedule.getAvailableCount());
        ApSchedule refreshed = requireSchedule(scheduleId);
        log.info("排班发布成功, traceId: {}, scheduleId: {}, availableCount: {}",
                TraceContext.getOrCreateTraceId(), scheduleId, refreshed.getAvailableCount());
        recordScheduleEvent(scheduleId, "PUBLISH", ScheduleStatus.DRAFT.name(), ScheduleStatus.OPEN.name(), "发布排班");
        return toResponse(refreshed);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public ScheduleResponse suspendSchedule(String scheduleId) {
        ApSchedule schedule = requireSchedule(scheduleId);
        if (!ScheduleStatus.OPEN.name().equals(schedule.getStatus())) {
            throw new AppointmentBizException("只有已发布排班允许暂停");
        }
        int updated = apScheduleMapper.updateStatusIfCurrent(
                scheduleId,
                ScheduleStatus.OPEN.name(),
                ScheduleStatus.SUSPENDED.name()
        );
        if (updated == 0) {
            throw new AppointmentBizException("排班暂停失败");
        }
        tokenGateService.initScheduleToken(scheduleId, 0);
        recordScheduleEvent(scheduleId, "SUSPEND", ScheduleStatus.OPEN.name(), ScheduleStatus.SUSPENDED.name(), "暂停排班");
        ApSchedule refreshed = requireSchedule(scheduleId);
        log.info("排班暂停成功, traceId: {}, scheduleId: {}", TraceContext.getOrCreateTraceId(), scheduleId);
        return toResponse(refreshed);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public ScheduleResponse resumeSchedule(String scheduleId) {
        ApSchedule schedule = requireSchedule(scheduleId);
        if (!ScheduleStatus.SUSPENDED.name().equals(schedule.getStatus())) {
            throw new AppointmentBizException("只有暂停中的排班允许恢复");
        }
        int updated = apScheduleMapper.updateStatusIfCurrent(
                scheduleId,
                ScheduleStatus.SUSPENDED.name(),
                ScheduleStatus.OPEN.name()
        );
        if (updated == 0) {
            throw new AppointmentBizException("排班恢复失败");
        }
        ApSchedule refreshed = requireSchedule(scheduleId);
        tokenGateService.initScheduleToken(scheduleId, refreshed.getAvailableCount());
        recordScheduleEvent(scheduleId, "RESUME", ScheduleStatus.SUSPENDED.name(), ScheduleStatus.OPEN.name(), "恢复排班");
        log.info("排班恢复成功, traceId: {}, scheduleId: {}, availableCount: {}",
                TraceContext.getOrCreateTraceId(), scheduleId, refreshed.getAvailableCount());
        return toResponse(refreshed);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public ScheduleResponse cancelSchedule(String scheduleId) {
        ApSchedule schedule = requireSchedule(scheduleId);
        if (ScheduleStatus.CANCELLED.name().equals(schedule.getStatus())) {
            return toResponse(schedule);
        }
        if (schedule.getHeldCount() > 0 || schedule.getConfirmedCount() > 0) {
            throw new AppointmentBizException("当前排班存在预占或已确认号源，暂不允许直接停诊");
        }
        String currentStatus = schedule.getStatus();
        if (!ScheduleStatus.DRAFT.name().equals(currentStatus)
                && !ScheduleStatus.OPEN.name().equals(currentStatus)
                && !ScheduleStatus.SUSPENDED.name().equals(currentStatus)) {
            throw new AppointmentBizException("当前排班状态不允许停诊");
        }
        int updated = apScheduleMapper.updateStatusIfCurrent(
                scheduleId,
                currentStatus,
                ScheduleStatus.CANCELLED.name()
        );
        if (updated == 0) {
            throw new AppointmentBizException("排班停诊失败");
        }
        tokenGateService.initScheduleToken(scheduleId, 0);
        recordScheduleEvent(scheduleId, "CANCEL", currentStatus, ScheduleStatus.CANCELLED.name(), "停诊关闭排班");
        ApSchedule refreshed = requireSchedule(scheduleId);
        log.info("排班停诊成功, traceId: {}, scheduleId: {}", TraceContext.getOrCreateTraceId(), scheduleId);
        return toResponse(refreshed);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public ScheduleResponse initTestSchedule(CreateScheduleCommand command) {
        ScheduleResponse created = createSchedule(command);
        generateSlots(created.getScheduleId());
        return publishSchedule(created.getScheduleId());
    }

    private ApSchedule requireSchedule(String scheduleId) {
        ApSchedule schedule = apScheduleMapper.selectById(scheduleId);
        if (schedule == null) {
            throw new AppointmentBizException("排班不存在");
        }
        return schedule;
    }

    private ScheduleResponse toResponse(ApSchedule schedule) {
        return new ScheduleResponse(
                schedule.getScheduleId(),
                schedule.getStatus(),
                schedule.getTotalCount(),
                schedule.getAvailableCount(),
                schedule.getHeldCount(),
                schedule.getConfirmedCount(),
                schedule.getVisitDate(),
                schedule.getTimePeriod(),
                schedule.getOpenTime(),
                schedule.getCloseTime()
        );
    }

    private void recordScheduleEvent(String scheduleId,
                                     String eventType,
                                     String beforeStatus,
                                     String afterStatus,
                                     String reason) {
        ApScheduleEvent event = new ApScheduleEvent();
        event.setEventId(IdWorker.getIdStr());
        event.setScheduleId(scheduleId);
        event.setEventType(eventType);
        event.setBeforeStatus(beforeStatus);
        event.setAfterStatus(afterStatus);
        event.setReason(reason);
        event.setStatus("NEW");
        event.setRetryCount(0);
        apScheduleEventMapper.insert(event);
    }
}
