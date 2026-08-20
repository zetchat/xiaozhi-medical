package com.atguigu.yygh.appointment.domain.slot.model;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@TableName("ap_slot")
public class ApSlot {

    @TableId(type = IdType.INPUT)
    private String slotId;
    private String scheduleId;
    private Integer sequenceNo;
    private String status;
    private String holdId;
    private String patientId;
    private LocalDateTime lockedAt;
    private LocalDateTime confirmedAt;
    private LocalDateTime releasedAt;
    private String invalidReason;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}
