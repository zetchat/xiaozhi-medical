package com.atguigu.yygh.orders.dto;

import lombok.Data;

@Data
public class BookingRequest {
    private String scheduleId;
    private String patientId;
}
