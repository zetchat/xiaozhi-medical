package com.atguigu.yygh.appointment.application.impl;

import com.atguigu.yygh.appointment.api.command.AppointmentCreateCommand;
import com.atguigu.yygh.appointment.api.response.AppointmentCreateResponse;
import com.atguigu.yygh.appointment.application.AppointmentAppService;
import com.atguigu.yygh.appointment.common.exception.AppointmentBizException;
import com.atguigu.yygh.appointment.domain.hold.model.ApHold;
import com.atguigu.yygh.appointment.domain.order.model.ApOrder;
import com.atguigu.yygh.appointment.domain.outbox.model.ApOutboxMessage;
import com.atguigu.yygh.appointment.domain.shared.enums.HoldStatus;
import com.atguigu.yygh.appointment.domain.shared.enums.OrderStatus;
import com.atguigu.yygh.appointment.domain.slot.AllocatedSlot;
import com.atguigu.yygh.appointment.domain.slot.SlotAllocationService;
import com.atguigu.yygh.appointment.domain.token.TokenGateService;
import com.atguigu.yygh.appointment.infrastructure.mapper.ApHoldMapper;
import com.atguigu.yygh.appointment.infrastructure.mapper.ApOrderMapper;
import com.atguigu.yygh.appointment.infrastructure.mapper.ApOutboxMessageMapper;
import com.atguigu.yygh.appointment.mq.OutboxMessagePublisher;
import com.atguigu.yygh.appointment.mq.TimeoutOrderMessage;
import com.atguigu.yygh.common.trace.TraceContext;
import com.baomidou.mybatisplus.core.toolkit.IdWorker;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Service
@Slf4j
@RequiredArgsConstructor
public class AppointmentAppServiceImpl implements AppointmentAppService {

    private static final int DEFAULT_HOLD_MINUTES = 1;

    private final TokenGateService tokenGateService;
    private final SlotAllocationService slotAllocationService;
    private final ApHoldMapper apHoldMapper;
    private final ApOrderMapper apOrderMapper;
    private final ApOutboxMessageMapper apOutboxMessageMapper;
    private final ObjectMapper objectMapper;
    private final OutboxMessagePublisher outboxMessagePublisher;

    @Override
    @Transactional(rollbackFor = Exception.class)
    public AppointmentCreateResponse createAppointment(AppointmentCreateCommand command) {
        log.info("开始预约创建, traceId: {}, requestNo: {}, patientId: {}, scheduleId: {}",
                TraceContext.getOrCreateTraceId(), command.getRequestNo(), command.getPatientId(), command.getScheduleId());

        ApHold activeHold = apHoldMapper.findActiveByScheduleAndPatient(
                command.getScheduleId(), command.getPatientId()
        );
        if (activeHold != null) {
            log.warn("预约创建命中有效预占单, traceId: {}, requestNo: {}, patientId: {}, scheduleId: {}, holdId: {}",
                    TraceContext.getOrCreateTraceId(), command.getRequestNo(), command.getPatientId(),
                    command.getScheduleId(), activeHold.getHoldId());
            throw new AppointmentBizException("当前患者在该排班下已存在有效预约");
        }

        boolean acquired = tokenGateService.tryAcquireScheduleToken(command.getScheduleId());
        if (!acquired) {
            log.warn("预约创建令牌获取失败, traceId: {}, requestNo: {}, patientId: {}, scheduleId: {}",
                    TraceContext.getOrCreateTraceId(), command.getRequestNo(), command.getPatientId(), command.getScheduleId());
            throw new AppointmentBizException("号源已满");
        }

        String holdId = IdWorker.getIdStr();
        String orderId = IdWorker.getIdStr();
        LocalDateTime expireTime = LocalDateTime.now().plusMinutes(DEFAULT_HOLD_MINUTES);

        try {
            AllocatedSlot allocatedSlot = slotAllocationService.allocate(
                    command.getScheduleId(), command.getPatientId(), holdId
            );

            ApHold hold = buildHold(command, holdId, allocatedSlot.getSlotId(), expireTime);
            apHoldMapper.insert(hold);

            ApOrder order = buildOrder(command, orderId, holdId, expireTime);
            apOrderMapper.insert(order);

            ApOutboxMessage outboxMessage = buildTimeoutOutboxMessage(command, orderId, holdId, expireTime);
            apOutboxMessageMapper.insert(outboxMessage);
            outboxMessagePublisher.registerTimeoutPublish(outboxMessage, expireTime);

            log.info("预约创建成功, traceId: {}, orderId: {}, holdId: {}, slotId: {}, sequenceNo: {}",
                    TraceContext.getOrCreateTraceId(), orderId, holdId, allocatedSlot.getSlotId(), allocatedSlot.getSequenceNo());

            return new AppointmentCreateResponse(
                    orderId,
                    holdId,
                    allocatedSlot.getSlotId(),
                    allocatedSlot.getSequenceNo(),
                    expireTime,
                    OrderStatus.UNPAID.name()
            );
        } catch (DuplicateKeyException ex) {
            tokenGateService.releaseScheduleToken(command.getScheduleId());
            log.warn("预约创建触发幂等冲突, traceId: {}, requestNo: {}, patientId: {}, scheduleId: {}",
                    TraceContext.getOrCreateTraceId(), command.getRequestNo(), command.getPatientId(), command.getScheduleId(), ex);
            throw new AppointmentBizException("请求重复或患者已存在有效预约");
        } catch (Exception ex) {
            tokenGateService.releaseScheduleToken(command.getScheduleId());
            log.error("预约创建异常, traceId: {}, requestNo: {}, patientId: {}, scheduleId: {}",
                    TraceContext.getOrCreateTraceId(), command.getRequestNo(), command.getPatientId(), command.getScheduleId(), ex);
            throw ex;
        }
    }

    private ApHold buildHold(AppointmentCreateCommand command,
                             String holdId,
                             String slotId,
                             LocalDateTime expireTime) {
        ApHold hold = new ApHold();
        hold.setHoldId(holdId);
        hold.setRequestNo(command.getRequestNo());
        hold.setScheduleId(command.getScheduleId());
        hold.setSlotId(slotId);
        hold.setPatientId(command.getPatientId());
        hold.setStatus(HoldStatus.HELD.name());
        hold.setActiveFlag(1);
        hold.setExpireTime(expireTime);
        hold.setSource(StringUtils.hasText(command.getSource()) ? command.getSource() : "APP");
        return hold;
    }

    private ApOrder buildOrder(AppointmentCreateCommand command,
                               String orderId,
                               String holdId,
                               LocalDateTime expireTime) {
        ApOrder order = new ApOrder();
        order.setOrderId(orderId);
        order.setRequestNo(command.getRequestNo());
        order.setHoldId(holdId);
        order.setPatientId(command.getPatientId());
        order.setScheduleId(command.getScheduleId());
        order.setAmount(BigDecimal.ZERO);
        order.setStatus(OrderStatus.UNPAID.name());
        order.setPayDeadline(expireTime);
        order.setSource(StringUtils.hasText(command.getSource()) ? command.getSource() : "APP");
        return order;
    }

    private ApOutboxMessage buildTimeoutOutboxMessage(AppointmentCreateCommand command,
                                                      String orderId,
                                                      String holdId,
                                                      LocalDateTime expireTime) {
        ApOutboxMessage message = new ApOutboxMessage();
        message.setMsgId(IdWorker.getIdStr());
        message.setBizType("ORDER_TIMEOUT");
        message.setBizKey(orderId);
        message.setPayload(toJson(buildOutboxPayload(message, command, orderId, holdId, expireTime)));
        message.setStatus("NEW");
        message.setRetryCount(0);
        message.setNextRetryTime(LocalDateTime.now());
        return message;
    }

    private String toJson(Object value) {
        try {
            return objectMapper.writeValueAsString(value);
        } catch (JsonProcessingException ex) {
            throw new AppointmentBizException("构建消息体失败");
        }
    }

    private TimeoutOrderMessage buildOutboxPayload(ApOutboxMessage message,
                                                   AppointmentCreateCommand command,
                                                   String orderId,
                                                   String holdId,
                                                   LocalDateTime expireTime) {
        TimeoutOrderMessage payload = new TimeoutOrderMessage();
        payload.setMsgId(message.getMsgId());
        payload.setOrderId(orderId);
        payload.setHoldId(holdId);
        payload.setScheduleId(command.getScheduleId());
        payload.setPatientId(command.getPatientId());
        payload.setRequestNo(command.getRequestNo());
        payload.setExpireTime(expireTime);
        payload.setTraceId(TraceContext.getOrCreateTraceId());
        return payload;
    }
}
