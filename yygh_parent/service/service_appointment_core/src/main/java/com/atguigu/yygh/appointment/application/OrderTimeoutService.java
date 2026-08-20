package com.atguigu.yygh.appointment.application;

public interface OrderTimeoutService {

    void closeIfExpired(String orderId, String holdId, String msgId);
}
