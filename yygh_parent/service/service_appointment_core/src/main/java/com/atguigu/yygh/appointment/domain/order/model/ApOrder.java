package com.atguigu.yygh.appointment.domain.order.model;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Data
@TableName("ap_order")
public class ApOrder {

    @TableId(type = IdType.INPUT)
    private String orderId;
    private String requestNo;
    private String holdId;
    private String patientId;
    private String scheduleId;
    private BigDecimal amount;
    private String status;
    private LocalDateTime payDeadline;
    private LocalDateTime payTime;
    private String cancelReason;
    private String source;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}
