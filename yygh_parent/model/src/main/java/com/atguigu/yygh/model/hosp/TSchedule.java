package com.atguigu.yygh.model.hosp;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import com.fasterxml.jackson.annotation.JsonFormat;
import lombok.Data;

import java.io.Serializable;
import java.util.Date;

/**
 * 冗余排班表 (t_schedule)
 */
@Data
@TableName("t_schedule")
public class TSchedule implements Serializable {

    private static final long serialVersionUID = 1L;

    /**
     * 主键，本地排班ID
     */
    @TableId(value = "schedule_id", type = IdType.ASSIGN_ID)
    private String scheduleId;

    /**
     * 科室ID
     */
    @TableField("dept_id")
    private String deptId;

    /**
     * 医生ID
     */
    @TableField("doctor_id")
    private String doctorId;

    /**
     * 就诊日期 (如 2023-10-25)
     */
    @JsonFormat(pattern = "yyyy-MM-dd")
    @TableField("work_date")
    private Date workDate;

    /**
     * 就诊时段 (如 MORNING, AFTERNOON)
     */
    @TableField("work_time")
    private String workTime;

    /**
     * 总放号量
     */
    @TableField("total_num")
    private Integer totalNum;

    /**
     * 业务熔断状态：NORMAL(正常), STOPPED(停诊)
     */
    @TableField("status")
    private String status;

    /**
     * 最后同步时间
     */
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    @TableField("update_time")
    private Date updateTime;
}
