package com.atguigu.yygh.appointment.application.impl;

import com.atguigu.yygh.appointment.api.response.OrderDetailResponse;
import com.atguigu.yygh.appointment.application.OrderQueryAppService;
import com.atguigu.yygh.appointment.common.exception.AppointmentBizException;
import com.atguigu.yygh.appointment.domain.hold.model.ApHold;
import com.atguigu.yygh.appointment.domain.order.model.ApOrder;
import com.atguigu.yygh.appointment.domain.slot.model.ApSlot;
import com.atguigu.yygh.appointment.infrastructure.mapper.ApHoldMapper;
import com.atguigu.yygh.appointment.infrastructure.mapper.ApOrderMapper;
import com.atguigu.yygh.appointment.infrastructure.mapper.ApSlotMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class OrderQueryAppServiceImpl implements OrderQueryAppService {

    private final ApOrderMapper apOrderMapper;
    private final ApHoldMapper apHoldMapper;
    private final ApSlotMapper apSlotMapper;

    @Override
    public OrderDetailResponse getOrderDetail(String orderId) {
        ApOrder order = apOrderMapper.selectById(orderId);
        if (order == null) {
            throw new AppointmentBizException("订单不存在");
        }
        ApHold hold = apHoldMapper.selectById(order.getHoldId());
        if (hold == null) {
            throw new AppointmentBizException("订单关联预占单不存在");
        }
        ApSlot slot = apSlotMapper.selectById(hold.getSlotId());
        return new OrderDetailResponse(
                order.getOrderId(),
                order.getHoldId(),
                order.getScheduleId(),
                order.getPatientId(),
                order.getStatus(),
                hold.getStatus(),
                slot == null ? null : slot.getSequenceNo(),
                order.getAmount(),
                order.getPayDeadline(),
                order.getPayTime()
        );
    }
}
