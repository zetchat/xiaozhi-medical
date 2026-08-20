package com.atguigu.yygh.appointment.api.response;

import lombok.AllArgsConstructor;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@AllArgsConstructor
public class ReconcileTaskResponse {

    private String taskId;
    private String taskType;
    private String bizKey;
    private String status;
    private String detailJson;
    private Integer retryCount;
    private String lastError;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}
