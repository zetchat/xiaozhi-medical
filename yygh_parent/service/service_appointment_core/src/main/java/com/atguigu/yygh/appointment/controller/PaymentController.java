package com.atguigu.yygh.appointment.controller;

import com.atguigu.yygh.appointment.api.command.PaymentConfirmCommand;
import com.atguigu.yygh.appointment.api.response.PaymentConfirmResponse;
import com.atguigu.yygh.appointment.application.PaymentAppService;
import com.atguigu.yygh.appointment.common.api.ApiResponse;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v2/payments")
public class PaymentController {

    private final PaymentAppService paymentAppService;

    public PaymentController(PaymentAppService paymentAppService) {
        this.paymentAppService = paymentAppService;
    }

    @PostMapping("/confirm")
    public ApiResponse<PaymentConfirmResponse> confirm(@Valid @RequestBody PaymentConfirmCommand command) {
        return ApiResponse.ok(paymentAppService.confirmPayment(command));
    }
}
