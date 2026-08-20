package com.atguigu.yygh.appointment.application;

import com.atguigu.yygh.appointment.api.response.OrderDetailResponse;

public interface OrderQueryAppService {

    OrderDetailResponse getOrderDetail(String orderId);
}
