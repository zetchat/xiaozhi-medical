package com.atguigu.yygh.appointment.application;

import com.atguigu.yygh.appointment.api.command.PaymentConfirmCommand;
import com.atguigu.yygh.appointment.api.response.PaymentConfirmResponse;

public interface PaymentAppService {

    PaymentConfirmResponse confirmPayment(PaymentConfirmCommand command);
}
