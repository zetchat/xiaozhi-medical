package com.atguigu.yygh.orders.mapper;

import com.atguigu.yygh.model.order.TLocalMessageLog;
import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;
import org.apache.ibatis.annotations.Update;

import java.util.List;

public interface TLocalMessageLogMapper extends BaseMapper<TLocalMessageLog> {

    @Update("UPDATE t_local_message_log SET status = #{status} WHERE msg_id = #{msgId}")
    void updateStatus(@Param("msgId") String msgId, @Param("status") String status);

    @Select("SELECT * FROM t_local_message_log WHERE status = 'NEW' AND create_time <= DATE_SUB(NOW(), INTERVAL 1 MINUTE)")
    List<TLocalMessageLog> findStagnantMessages();

    @Update("UPDATE t_local_message_log SET retry_count = retry_count + 1 WHERE msg_id = #{msgId}")
    void incrementRetryCount(@Param("msgId") String msgId);
}
