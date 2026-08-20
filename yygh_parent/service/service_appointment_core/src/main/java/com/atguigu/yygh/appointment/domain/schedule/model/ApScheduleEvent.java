package com.atguigu.yygh.appointment.domain.schedule.model;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@TableName("ap_schedule_event")
public class ApScheduleEvent {

    @TableId(type = IdType.INPUT)
    private String eventId;
    private String scheduleId;
    private String eventType;
    private String beforeStatus;
    private String afterStatus;
    private String reason;
    private String operatorId;
    private String status;
    private Integer retryCount;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}
