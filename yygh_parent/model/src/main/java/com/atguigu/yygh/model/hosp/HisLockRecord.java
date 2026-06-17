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
 * HIS号源锁定流水表 (his_lock_record)
 */
@Data
@TableName("his_lock_record")
public class HisLockRecord implements Serializable {

    private static final long serialVersionUID = 1L;

    /**
     * 主键，锁号流水凭证(返回给前台存入t_order)
     */
    @TableId(value = "his_seq_no", type = IdType.ASSIGN_ID)
    private String hisSeqNo;

    /**
     * 锁定的排班ID
     */
    @TableField("schedule_id")
    private String scheduleId;

    /**
     * 锁定了具体哪一个号源
     */
    @TableField("detail_id")
    private String detailId;

    /**
     * 挂号患者ID
     */
    @TableField("patient_id")
    private String patientId;

    /**
     * 核心状态机：LOCKED(锁定中), RELEASED(已释放), CONFIRMED(已确认/已支付)
     */
    @TableField("status")
    private String status;

    /**
     * 锁定时间
     */
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    @TableField("lock_time")
    private Date lockTime;
}
