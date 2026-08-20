package com.atguigu.yygh.appointment.domain.reconcile.model;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@TableName("ap_reconcile_task")
public class ApReconcileTask {

    @TableId(type = IdType.INPUT)
    private String taskId;
    private String taskType;
    private String bizKey;
    private String detailJson;
    private String status;
    private Integer retryCount;
    private String lastError;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}
