package com.atguigu.yygh.appointment.api.command;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;

@Data
public class PaymentConfirmCommand {

    @NotBlank
    private String orderId;

    @NotBlank
    private String payChannel;

    @NotBlank
    private String channelTradeNo;
}
