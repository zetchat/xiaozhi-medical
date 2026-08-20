package com.atguigu.yygh.appointment.api.response;

import lombok.AllArgsConstructor;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@AllArgsConstructor
public class PaymentConfirmResponse {

    private String orderId;
    private String holdId;
    private String orderStatus;
    private String holdStatus;
    private LocalDateTime payTime;
}
