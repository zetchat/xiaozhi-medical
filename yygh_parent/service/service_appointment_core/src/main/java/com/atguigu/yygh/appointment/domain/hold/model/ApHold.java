package com.atguigu.yygh.appointment.domain.hold.model;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@TableName("ap_hold")
public class ApHold {

    @TableId(type = IdType.INPUT)
    private String holdId;
    private String requestNo;
    private String scheduleId;
    private String slotId;
    private String patientId;
    private String status;
    private Integer activeFlag;
    private LocalDateTime expireTime;
    private String releaseReason;
    private String source;
    private String traceId;
    private String remark;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}
