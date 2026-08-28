package com.atguigu.yygh.appointment.application.impl;

import com.atguigu.yygh.appointment.api.response.OrderDetailResponse;
import com.atguigu.yygh.appointment.application.OrderCancelAppService;
import com.atguigu.yygh.appointment.application.OrderQueryAppService;
import com.atguigu.yygh.appointment.common.exception.AppointmentBizException;
import com.atguigu.yygh.appointment.domain.hold.model.ApHold;
import com.atguigu.yygh.appointment.domain.order.model.ApOrder;
import com.atguigu.yygh.appointment.domain.payment.model.ApPaymentRecord;
import com.atguigu.yygh.appointment.domain.shared.enums.HoldStatus;
import com.atguigu.yygh.appointment.domain.shared.enums.OrderStatus;
import com.atguigu.yygh.appointment.domain.token.TokenGateService;
import com.atguigu.yygh.appointment.infrastructure.mapper.ApHoldMapper;
import com.atguigu.yygh.appointment.infrastructure.mapper.ApOrderMapper;
import com.atguigu.yygh.appointment.infrastructure.mapper.ApPaymentRecordMapper;
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
public class OrderCancelAppServiceImpl implements OrderCancelAppService {

    private final ApOrderMapper apOrderMapper;
    private final ApHoldMapper apHoldMapper;
    private final ApSlotMapper apSlotMapper;
    private final ApScheduleMapper apScheduleMapper;
    private final ApPaymentRecordMapper apPaymentRecordMapper;
    private final TokenGateService tokenGateService;
    private final OrderQueryAppService orderQueryAppService;

    @Override
    @Transactional(rollbackFor = Exception.class)
    public OrderDetailResponse cancelOrder(String orderId) {
        log.info("开始取消订单, traceId: {}, orderId: {}", TraceContext.getOrCreateTraceId(), orderId);
        ApOrder order = apOrderMapper.selectById(orderId);
        if (order == null) {
            throw new AppointmentBizException("订单不存在");
        }
        if (OrderStatus.CANCELLED.name().equals(order.getStatus())) {
            log.info("取消订单命中已取消状态, traceId: {}, orderId: {}", TraceContext.getOrCreateTraceId(), orderId);
            return orderQueryAppService.getOrderDetail(orderId);
        }
        if (OrderStatus.PAID.name().equals(order.getStatus())) {
            throw new AppointmentBizException("已支付订单暂不支持直接取消");
        }
        ApPaymentRecord paymentRecord = apPaymentRecordMapper.findLatestByOrderId(orderId);
        if (paymentRecord != null && "SUCCESS".equals(paymentRecord.getPayStatus())) {
            throw new AppointmentBizException("订单已存在成功支付流水，不能直接取消");
        }

        int orderUpdated = apOrderMapper.updateStatusIfCurrent(
                orderId,
                OrderStatus.UNPAID.name(),
                OrderStatus.CANCELLED.name(),
                "USER_CANCEL"
        );
        if (orderUpdated == 0) {
            throw new AppointmentBizException("订单状态异常，无法取消");
        }

        ApHold hold = apHoldMapper.selectById(order.getHoldId());
        if (hold == null) {
            throw new AppointmentBizException("订单关联预占单不存在");
        }
        if (!HoldStatus.HELD.name().equals(hold.getStatus())) {
            throw new AppointmentBizException("预占单状态异常，无法取消");
        }

        int slotUpdated = apSlotMapper.releaseHeld(hold.getSlotId(), hold.getHoldId());
        if (slotUpdated == 0) {
            throw new AppointmentBizException("号位释放失败");
        }

        int holdUpdated = apHoldMapper.updateStatusIfCurrent(
                hold.getHoldId(),
                HoldStatus.HELD.name(),
                HoldStatus.RELEASED.name(),
                0,
                "USER_CANCEL"
        );
        if (holdUpdated == 0) {
            throw new AppointmentBizException("预占单释放失败");
        }

        int scheduleUpdated = apScheduleMapper.releaseFromHold(hold.getScheduleId());
        if (scheduleUpdated == 0) {
            throw new AppointmentBizException("排班聚合回滚失败");
        }

        tokenGateService.releaseScheduleToken(hold.getScheduleId());
        log.info("主动取消订单成功, traceId: {}, orderId: {}, holdId: {}",
                TraceContext.getOrCreateTraceId(), orderId, hold.getHoldId());
        return orderQueryAppService.getOrderDetail(orderId);
    }
}
