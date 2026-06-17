package com.atguigu.yygh.model.order;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import com.fasterxml.jackson.annotation.JsonFormat;
import lombok.Data;

import java.io.Serializable;
import java.util.Date;

/**
 * 挂号订单表 (t_order)
 */
@Data
@TableName("t_order")
public class TOrder implements Serializable {

    private static final long serialVersionUID = 1L;

    /**
     * 主键，分布式订单号(推荐雪花算法)
     */
    @TableId(value = "order_id", type = IdType.ASSIGN_ID)
    private String orderId;

    /**
     * 关联的本地排班ID
     */
    @TableField("schedule_id")
    private String scheduleId;

    /**
     * 挂号患者ID
     */
    @TableField("patient_id")
    private String patientId;

    /**
     * HIS系统返回的锁号凭证
     */
    @TableField("his_seq_no")
    private String hisSeqNo;

    /**
     * 状态：UNPAID(待支付), PAID(已支付), CANCELLED(已取消)
     */
    @TableField("status")
    private String status;

    /**
     * 订单创建时间
     */
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    @TableField("create_time")
    private Date createTime;

    /**
     * 最后更新时间
     */
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    @TableField("update_time")
    private Date updateTime;
}
