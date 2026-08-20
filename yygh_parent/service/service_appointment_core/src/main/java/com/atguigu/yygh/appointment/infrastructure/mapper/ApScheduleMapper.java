package com.atguigu.yygh.appointment.infrastructure.mapper;

import com.atguigu.yygh.appointment.domain.schedule.model.ApSchedule;
import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;
import org.apache.ibatis.annotations.Update;

import java.util.List;

@Mapper
public interface ApScheduleMapper extends BaseMapper<ApSchedule> {

    @Update("""
        UPDATE ap_schedule
        SET available_count = available_count - 1,
            held_count = held_count + 1,
            version = version + 1,
            updated_at = NOW(3)
        WHERE schedule_id = #{scheduleId}
          AND status = 'OPEN'
          AND available_count > 0
        """)
    int occupyForHold(@Param("scheduleId") String scheduleId);

    @Update("""
        UPDATE ap_schedule
        SET available_count = available_count + 1,
            held_count = held_count - 1,
            version = version + 1,
            updated_at = NOW(3)
        WHERE schedule_id = #{scheduleId}
          AND held_count > 0
        """)
    int releaseFromHold(@Param("scheduleId") String scheduleId);

    @Update("""
        UPDATE ap_schedule
        SET held_count = held_count - 1,
            confirmed_count = confirmed_count + 1,
            version = version + 1,
            updated_at = NOW(3)
        WHERE schedule_id = #{scheduleId}
          AND held_count > 0
        """)
    int confirmFromHold(@Param("scheduleId") String scheduleId);

    @Update("""
        UPDATE ap_schedule
        SET status = #{targetStatus},
            updated_at = NOW(3)
        WHERE schedule_id = #{scheduleId}
          AND status = #{currentStatus}
        """)
    int updateStatusIfCurrent(@Param("scheduleId") String scheduleId,
                              @Param("currentStatus") String currentStatus,
                              @Param("targetStatus") String targetStatus);

    @Update("""
        UPDATE ap_schedule
        SET available_count = #{availableCount},
            held_count = 0,
            confirmed_count = 0,
            updated_at = NOW(3)
        WHERE schedule_id = #{scheduleId}
        """)
    int initCounts(@Param("scheduleId") String scheduleId,
                   @Param("availableCount") Integer availableCount);

    @Select("""
        SELECT *
        FROM ap_schedule
        WHERE status IN ('DRAFT', 'OPEN', 'SUSPENDED')
        ORDER BY created_at DESC
        LIMIT #{limit}
        """)
    List<ApSchedule> findRecentSchedules(@Param("limit") int limit);
}
