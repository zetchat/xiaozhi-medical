package com.atguigu.yygh.appointment.application.impl;

import com.atguigu.yygh.appointment.api.command.PaymentConfirmCommand;
import com.atguigu.yygh.appointment.api.response.PaymentConfirmResponse;
import com.atguigu.yygh.appointment.application.PaymentAppService;
import com.atguigu.yygh.appointment.common.exception.AppointmentBizException;
import com.atguigu.yygh.appointment.domain.hold.model.ApHold;
import com.atguigu.yygh.appointment.domain.order.model.ApOrder;
import com.atguigu.yygh.appointment.domain.payment.model.ApPaymentRecord;
import com.atguigu.yygh.appointment.domain.shared.enums.HoldStatus;
import com.atguigu.yygh.appointment.domain.shared.enums.OrderStatus;
import com.atguigu.yygh.appointment.infrastructure.mapper.ApHoldMapper;
import com.atguigu.yygh.appointment.infrastructure.mapper.ApOrderMapper;
import com.atguigu.yygh.appointment.infrastructure.mapper.ApPaymentRecordMapper;
import com.atguigu.yygh.appointment.infrastructure.mapper.ApScheduleMapper;
import com.atguigu.yygh.appointment.infrastructure.mapper.ApSlotMapper;
import com.atguigu.yygh.common.trace.TraceContext;
import com.baomidou.mybatisplus.core.toolkit.IdWorker;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;

@Service
@Slf4j
@RequiredArgsConstructor
public class PaymentAppServiceImpl implements PaymentAppService {

    private final ApOrderMapper apOrderMapper;
    private final ApHoldMapper apHoldMapper;
    private final ApSlotMapper apSlotMapper;
    private final ApScheduleMapper apScheduleMapper;
    private final ApPaymentRecordMapper apPaymentRecordMapper;

    @Override
    @Transactional(rollbackFor = Exception.class)
    public PaymentConfirmResponse confirmPayment(PaymentConfirmCommand command) {
        log.info("开始支付确认, traceId: {}, orderId: {}, channel: {}, tradeNo: {}",
                TraceContext.getOrCreateTraceId(), command.getOrderId(), command.getPayChannel(), command.getChannelTradeNo());

        ApPaymentRecord existedRecord = apPaymentRecordMapper.findByChannelTradeNo(
                command.getPayChannel(), command.getChannelTradeNo()
        );
        if (existedRecord != null) {
            ApOrder existedOrder = apOrderMapper.selectById(existedRecord.getOrderId());
            if (existedOrder == null) {
                throw new AppointmentBizException("支付流水存在，但订单不存在");
            }
            ApHold existedHold = apHoldMapper.selectById(existedOrder.getHoldId());
            log.info("支付确认命中幂等流水, traceId: {}, orderId: {}, holdId: {}, channel: {}, tradeNo: {}",
                    TraceContext.getOrCreateTraceId(), existedOrder.getOrderId(), existedOrder.getHoldId(),
                    command.getPayChannel(), command.getChannelTradeNo());
            return new PaymentConfirmResponse(
                    existedOrder.getOrderId(),
                    existedOrder.getHoldId(),
                    existedOrder.getStatus(),
                    existedHold == null ? null : existedHold.getStatus(),
                    existedOrder.getPayTime()
            );
        }

        ApOrder order = apOrderMapper.selectById(command.getOrderId());
        if (order == null) {
            throw new AppointmentBizException("订单不存在");
        }
        if (OrderStatus.CANCELLED.name().equals(order.getStatus())) {
            throw new AppointmentBizException("订单已取消，不能确认支付");
        }
        if (OrderStatus.PAID.name().equals(order.getStatus())) {
            ApHold paidHold = apHoldMapper.selectById(order.getHoldId());
            log.info("支付确认命中已支付订单, traceId: {}, orderId: {}, holdId: {}",
                    TraceContext.getOrCreateTraceId(), order.getOrderId(), order.getHoldId());
            return new PaymentConfirmResponse(
                    order.getOrderId(),
                    order.getHoldId(),
                    order.getStatus(),
                    paidHold == null ? null : paidHold.getStatus(),
                    order.getPayTime()
            );
        }

        LocalDateTime payTime = LocalDateTime.now();
        int orderUpdated = apOrderMapper.markPaid(order.getOrderId(), payTime);
        if (orderUpdated == 0) {
            throw new AppointmentBizException("订单状态异常，支付确认失败");
        }

        ApHold hold = apHoldMapper.selectById(order.getHoldId());
        if (hold == null) {
            throw new AppointmentBizException("预占单不存在");
        }
        if (!HoldStatus.HELD.name().equals(hold.getStatus())) {
            throw new AppointmentBizException("预占单状态异常，不能确认支付");
        }

        int holdUpdated = apHoldMapper.markConfirmed(hold.getHoldId());
        if (holdUpdated == 0) {
            throw new AppointmentBizException("预占单确认失败");
        }

        int slotUpdated = apSlotMapper.confirmHeld(hold.getSlotId(), hold.getHoldId());
        if (slotUpdated == 0) {
            throw new AppointmentBizException("号位确认失败");
        }

        int scheduleUpdated = apScheduleMapper.confirmFromHold(hold.getScheduleId());
        if (scheduleUpdated == 0) {
            throw new AppointmentBizException("排班聚合确认失败");
        }

        ApPaymentRecord paymentRecord = new ApPaymentRecord();
        paymentRecord.setPayRecordId(IdWorker.getIdStr());
        paymentRecord.setOrderId(order.getOrderId());
        paymentRecord.setPayChannel(command.getPayChannel());
        paymentRecord.setPayStatus("SUCCESS");
        paymentRecord.setChannelTradeNo(command.getChannelTradeNo());
        paymentRecord.setCallbackPayload(command.toString());
        paymentRecord.setPaidAt(payTime);
        apPaymentRecordMapper.insert(paymentRecord);

        log.info("支付确认成功, traceId: {}, orderId: {}, holdId: {}, channel: {}, tradeNo: {}",
                TraceContext.getOrCreateTraceId(), order.getOrderId(), hold.getHoldId(),
                command.getPayChannel(), command.getChannelTradeNo());

        return new PaymentConfirmResponse(
                order.getOrderId(),
                hold.getHoldId(),
                OrderStatus.PAID.name(),
                HoldStatus.CONFIRMED.name(),
                payTime
        );
    }
}
