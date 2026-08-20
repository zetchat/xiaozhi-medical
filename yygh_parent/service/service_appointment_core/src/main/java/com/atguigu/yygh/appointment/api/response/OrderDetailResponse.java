package com.atguigu.yygh.appointment.api.response;

import lombok.AllArgsConstructor;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Data
@AllArgsConstructor
public class OrderDetailResponse {

    private String orderId;
    private String holdId;
    private String scheduleId;
    private String patientId;
    private String orderStatus;
    private String holdStatus;
    private Integer sequenceNo;
    private BigDecimal amount;
    private LocalDateTime payDeadline;
    private LocalDateTime payTime;
}
