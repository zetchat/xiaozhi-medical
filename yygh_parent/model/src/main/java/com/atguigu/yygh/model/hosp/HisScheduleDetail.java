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
 * HIS排班号源明细表 (his_schedule_detail)
 */
@Data
@TableName("his_schedule_detail")
public class HisScheduleDetail implements Serializable {

    private static final long serialVersionUID = 1L;

    /**
     * 主键，唯一号源明细ID
     */
    @TableId(value = "detail_id", type = IdType.ASSIGN_ID)
    private String detailId;

    /**
     * 关联的排班ID
     */
    @TableField("schedule_id")
    private String scheduleId;

    /**
     * 就诊序号 (1号、2号...)
     */
    @TableField("sequence_no")
    private Integer sequenceNo;

    /**
     * 状态：AVAILABLE(可用), LOCKED(暂扣), USED(已就诊)
     */
    @TableField("status")
    private String status;

    /**
     * 状态更新时间
     */
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    @TableField("update_time")
    private Date updateTime;
}
