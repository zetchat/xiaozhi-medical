package com.atguigu.yygh.appointment.application.impl;

import com.atguigu.yygh.appointment.application.OrderTimeoutService;
import com.atguigu.yygh.appointment.common.exception.AppointmentBizException;
import com.atguigu.yygh.appointment.domain.hold.model.ApHold;
import com.atguigu.yygh.appointment.domain.order.model.ApOrder;
import com.atguigu.yygh.appointment.domain.shared.enums.HoldStatus;
import com.atguigu.yygh.appointment.domain.shared.enums.OrderStatus;
import com.atguigu.yygh.appointment.domain.token.TokenGateService;
import com.atguigu.yygh.appointment.infrastructure.mapper.ApHoldMapper;
import com.atguigu.yygh.appointment.infrastructure.mapper.ApOrderMapper;
import com.atguigu.yygh.appointment.infrastructure.mapper.ApOutboxMessageMapper;
import com.atguigu.yygh.appointment.infrastructure.mapper.ApScheduleMapper;
import com.atguigu.yygh.appointment.infrastructure.mapper.ApSlotMapper;
import com.atguigu.yygh.common.trace.TraceContext;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Slf4j
@RequiredArgsConstructor
public class OrderTimeoutServiceImpl implements OrderTimeoutService {

    private final ApOrderMapper apOrderMapper;
    private final ApHoldMapper apHoldMapper;
    private final ApSlotMapper apSlotMapper;
    private final ApScheduleMapper apScheduleMapper;
    private final ApOutboxMessageMapper apOutboxMessageMapper;
    private final TokenGateService tokenGateService;

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void closeIfExpired(String orderId, String holdId, String msgId) {
        log.info("开始处理超时关单, traceId: {}, msgId: {}, orderId: {}, holdId: {}",
                TraceContext.getOrCreateTraceId(), msgId, orderId, holdId);
        ApOrder order = apOrderMapper.selectById(orderId);
        if (order == null) {
            markConsumed(msgId);
            log.warn("超时消息对应订单不存在，忽略处理, traceId: {}, msgId: {}, orderId: {}",
                    TraceContext.getOrCreateTraceId(), msgId, orderId);
            return;
        }

        if (!OrderStatus.UNPAID.name().equals(order.getStatus())) {
            markConsumed(msgId);
            log.info("订单状态无需超时关闭，忽略处理, traceId: {}, msgId: {}, orderId: {}, currentStatus: {}",
                    TraceContext.getOrCreateTraceId(), msgId, orderId, order.getStatus());
            return;
        }

        int orderUpdated = apOrderMapper.updateStatusIfCurrent(
                orderId,
                OrderStatus.UNPAID.name(),
                OrderStatus.CANCELLED.name(),
                "TIMEOUT"
        );
        if (orderUpdated == 0) {
            markConsumed(msgId);
            log.warn("订单已被其他流程处理，跳过重复关单, traceId: {}, msgId: {}, orderId: {}",
                    TraceContext.getOrCreateTraceId(), msgId, orderId);
            return;
        }

        ApHold hold = apHoldMapper.selectById(holdId);
        if (hold == null) {
            throw new AppointmentBizException("超时释放失败，未找到预占单: " + holdId);
        }
        if (HoldStatus.CONFIRMED.name().equals(hold.getStatus())) {
            throw new AppointmentBizException("超时释放失败，预占单已确认支付: " + hold.getHoldId());
        }

        if (HoldStatus.HELD.name().equals(hold.getStatus())) {
            int releasedSlot = apSlotMapper.releaseHeld(hold.getSlotId(), hold.getHoldId());
            if (releasedSlot == 0) {
                throw new AppointmentBizException("超时释放失败，号位状态异常: " + hold.getSlotId());
            }

            int releasedHold = apHoldMapper.updateStatusIfCurrent(
                    hold.getHoldId(),
                    HoldStatus.HELD.name(),
                    HoldStatus.EXPIRED.name(),
                    0,
                    "TIMEOUT"
            );
            if (releasedHold == 0) {
                throw new AppointmentBizException("超时释放失败，预占单状态异常: " + hold.getHoldId());
            }

            int releasedSchedule = apScheduleMapper.releaseFromHold(hold.getScheduleId());
            if (releasedSchedule == 0) {
                throw new AppointmentBizException("超时释放失败，排班聚合回滚异常: " + hold.getScheduleId());
            }

            tokenGateService.releaseScheduleToken(hold.getScheduleId());
        }

        markConsumed(msgId);
        log.info("超时关单完成, traceId: {}, msgId: {}, orderId: {}, holdId: {}",
                TraceContext.getOrCreateTraceId(), msgId, orderId, hold.getHoldId());
    }

    private void markConsumed(String msgId) {
        if (msgId == null) {
            return;
        }
        int publishedUpdated = apOutboxMessageMapper.updateStatusIfCurrent(msgId, "PUBLISHED", "CONSUMED");
        if (publishedUpdated == 0) {
            apOutboxMessageMapper.updateStatusIfCurrent(msgId, "NEW", "CONSUMED");
        }
    }
}
