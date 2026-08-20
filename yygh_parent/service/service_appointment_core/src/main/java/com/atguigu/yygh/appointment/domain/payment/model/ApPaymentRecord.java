package com.atguigu.yygh.appointment.domain.payment.model;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@TableName("ap_payment_record")
public class ApPaymentRecord {

    @TableId(type = IdType.INPUT)
    private String payRecordId;
    private String orderId;
    private String payChannel;
    private String payStatus;
    private String channelTradeNo;
    private String callbackPayload;
    private LocalDateTime paidAt;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}
