package com.atguigu.yygh.appointment.application;

import com.atguigu.yygh.appointment.api.response.OrderDetailResponse;

public interface OrderCancelAppService {

    OrderDetailResponse cancelOrder(String orderId);
}
