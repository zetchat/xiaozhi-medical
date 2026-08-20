package com.atguigu.yygh.appointment.domain.schedule.model;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDate;
import java.time.LocalDateTime;

@Data
@TableName("ap_schedule")
public class ApSchedule {

    @TableId(type = IdType.INPUT)
    private String scheduleId;
    private String doctorId;
    private String deptId;
    private String hospitalId;
    private LocalDate visitDate;
    private Integer timePeriod;
    private Integer totalCount;
    private Integer availableCount;
    private Integer heldCount;
    private Integer confirmedCount;
    private String status;
    private Integer allowCancel;
    private LocalDateTime openTime;
    private LocalDateTime closeTime;
    private Integer version;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}
