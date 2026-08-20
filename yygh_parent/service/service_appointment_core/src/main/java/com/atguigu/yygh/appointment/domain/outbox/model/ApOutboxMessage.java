package com.atguigu.yygh.appointment.domain.outbox.model;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@TableName("ap_outbox_message")
public class ApOutboxMessage {

    @TableId(type = IdType.INPUT)
    private String msgId;
    private String bizType;
    private String bizKey;
    private String payload;
    private String status;
    private Integer retryCount;
    private LocalDateTime nextRetryTime;
    private String lastError;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}
