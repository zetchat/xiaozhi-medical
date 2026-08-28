package com.atguigu.yygh.appointment.job;

import com.atguigu.yygh.appointment.domain.reconcile.model.ApReconcileTask;
import com.atguigu.yygh.appointment.domain.schedule.model.ApSchedule;
import com.atguigu.yygh.appointment.domain.shared.enums.SlotStatus;
import com.atguigu.yygh.appointment.infrastructure.mapper.ApReconcileTaskMapper;
import com.atguigu.yygh.appointment.infrastructure.mapper.ApScheduleMapper;
import com.atguigu.yygh.appointment.infrastructure.mapper.ApSlotMapper;
import com.atguigu.yygh.common.trace.TraceContext;
import com.baomidou.mybatisplus.core.toolkit.IdWorker;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Component
@Slf4j
@RequiredArgsConstructor
public class ScheduleReconcileJob {

    private final ApScheduleMapper apScheduleMapper;
    private final ApSlotMapper apSlotMapper;
    private final ApReconcileTaskMapper apReconcileTaskMapper;
    private final ObjectMapper objectMapper;

    @Scheduled(cron = "0 */5 * * * ?")
    public void reconcileScheduleCounters() {
        String traceId = TraceContext.generateTraceId();
        try {
            TraceContext.setTraceId(traceId);
            List<ApSchedule> schedules = apScheduleMapper.findRecentSchedules(100);
            log.info("开始执行排班对账, traceId: {}, size: {}", traceId, schedules.size());
            for (ApSchedule schedule : schedules) {
                try {
                    int total = apSlotMapper.countBySchedule(schedule.getScheduleId());
                    int available = apSlotMapper.countByScheduleAndStatus(schedule.getScheduleId(), SlotStatus.AVAILABLE.name());
                    int held = apSlotMapper.countByScheduleAndStatus(schedule.getScheduleId(), SlotStatus.HELD.name());
                    int confirmed = apSlotMapper.countByScheduleAndStatus(schedule.getScheduleId(), SlotStatus.CONFIRMED.name());

                    boolean mismatch = total != schedule.getTotalCount()
                            || available != schedule.getAvailableCount()
                            || held != schedule.getHeldCount()
                            || confirmed != schedule.getConfirmedCount();

                    if (!mismatch) {
                        continue;
                    }

                    if (apReconcileTaskMapper.countOpenTask("SCHEDULE_COUNTER_MISMATCH", schedule.getScheduleId()) > 0) {
                        continue;
                    }

                    Map<String, Object> detail = new HashMap<>();
                    detail.put("scheduleTotal", schedule.getTotalCount());
                    detail.put("slotTotal", total);
                    detail.put("scheduleAvailable", schedule.getAvailableCount());
                    detail.put("slotAvailable", available);
                    detail.put("scheduleHeld", schedule.getHeldCount());
                    detail.put("slotHeld", held);
                    detail.put("scheduleConfirmed", schedule.getConfirmedCount());
                    detail.put("slotConfirmed", confirmed);

                    ApReconcileTask task = new ApReconcileTask();
                    task.setTaskId(IdWorker.getIdStr());
                    task.setTaskType("SCHEDULE_COUNTER_MISMATCH");
                    task.setBizKey(schedule.getScheduleId());
                    task.setDetailJson(objectMapper.writeValueAsString(detail));
                    task.setStatus("NEW");
                    task.setRetryCount(0);
                    apReconcileTaskMapper.insert(task);

                    log.warn("发现排班聚合计数不一致, traceId: {}, scheduleId: {}",
                            traceId, schedule.getScheduleId());
                } catch (Exception ex) {
                    log.error("排班对账失败, traceId: {}, scheduleId: {}", traceId, schedule.getScheduleId(), ex);
                }
            }
        } finally {
            TraceContext.clear();
        }
    }
}
