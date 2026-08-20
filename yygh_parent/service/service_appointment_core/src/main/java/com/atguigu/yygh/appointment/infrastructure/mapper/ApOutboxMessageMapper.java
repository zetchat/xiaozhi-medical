package com.atguigu.yygh.appointment.infrastructure.mapper;

import com.atguigu.yygh.appointment.domain.outbox.model.ApOutboxMessage;
import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;
import org.apache.ibatis.annotations.Update;

import java.util.List;

@Mapper
public interface ApOutboxMessageMapper extends BaseMapper<ApOutboxMessage> {

    @Update("""
        UPDATE ap_outbox_message
        SET status = #{targetStatus},
            updated_at = NOW(3)
        WHERE msg_id = #{msgId}
          AND status = #{currentStatus}
        """)
    int updateStatusIfCurrent(@Param("msgId") String msgId,
                              @Param("currentStatus") String currentStatus,
                              @Param("targetStatus") String targetStatus);

    @Update("""
        UPDATE ap_outbox_message
        SET retry_count = retry_count + 1,
            next_retry_time = #{nextRetryTime},
            last_error = #{lastError},
            updated_at = NOW(3)
        WHERE msg_id = #{msgId}
        """)
    int updateRetry(@Param("msgId") String msgId,
                    @Param("nextRetryTime") java.time.LocalDateTime nextRetryTime,
                    @Param("lastError") String lastError);

    @Select("""
        SELECT *
        FROM ap_outbox_message
        WHERE status = 'NEW'
          AND (next_retry_time IS NULL OR next_retry_time <= NOW(3))
        ORDER BY created_at ASC
        LIMIT #{limit}
        """)
    List<ApOutboxMessage> findRetryableMessages(@Param("limit") int limit);
}
