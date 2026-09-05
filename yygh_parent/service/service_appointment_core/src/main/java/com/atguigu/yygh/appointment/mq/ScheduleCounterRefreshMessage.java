package com.atguigu.yygh.appointment.mq;

import lombok.Data;

import java.time.LocalDateTime;

@Data
public class ScheduleCounterRefreshMessage {

    private String msgId;
    private String traceId;
    private String scheduleId;
    private String reason;
    private LocalDateTime occurredAt;
}
