package com.atguigu.yygh.appointment.mq;

import lombok.Data;

import java.time.LocalDateTime;

@Data
public class TimeoutOrderMessage {

    private String msgId;
    private String traceId;
    private String orderId;
    private String holdId;
    private String scheduleId;
    private String patientId;
    private String requestNo;
    private LocalDateTime expireTime;
}
