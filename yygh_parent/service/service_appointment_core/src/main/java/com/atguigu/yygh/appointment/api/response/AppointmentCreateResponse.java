package com.atguigu.yygh.appointment.api.response;

import lombok.AllArgsConstructor;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@AllArgsConstructor
public class AppointmentCreateResponse {

    private String orderId;
    private String holdId;
    private String slotId;
    private Integer sequenceNo;
    private LocalDateTime expireTime;
    private String status;
}
