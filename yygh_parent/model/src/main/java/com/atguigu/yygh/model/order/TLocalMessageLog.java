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
 * 本地消息表 (t_local_message_log)
 */
@Data
@TableName("t_local_message_log")
public class TLocalMessageLog implements Serializable {

    private static final long serialVersionUID = 1L;

    /**
     * 主键，全局唯一消息ID
     */
    @TableId(value = "msg_id", type = IdType.ASSIGN_ID)
    private String msgId;

    /**
     * 关联的订单号
     */
    @TableField("order_id")
    private String orderId;

    /**
     * 投递状态：NEW(新建), PUBLISHED(已投递), FAIL(彻底失败)
     */
    @TableField("status")
    private String status;

    /**
     * 重试次数
     */
    @TableField("retry_count")
    private Integer retryCount;

    /**
     * 消息创建时间
     */
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    @TableField("create_time")
    private Date createTime;

    /**
     * 状态更新时间
     */
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    @TableField("update_time")
    private Date updateTime;
}
