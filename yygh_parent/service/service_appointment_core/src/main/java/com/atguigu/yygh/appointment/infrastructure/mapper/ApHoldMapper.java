package com.atguigu.yygh.appointment.infrastructure.mapper;

import com.atguigu.yygh.appointment.domain.hold.model.ApHold;
import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;
import org.apache.ibatis.annotations.Update;

import java.util.List;

@Mapper
public interface ApHoldMapper extends BaseMapper<ApHold> {

    @Select("""
        SELECT *
        FROM ap_hold
        WHERE schedule_id = #{scheduleId}
          AND patient_id = #{patientId}
          AND active_flag = 1
        LIMIT 1
        """)
    ApHold findActiveByScheduleAndPatient(@Param("scheduleId") String scheduleId,
                                          @Param("patientId") String patientId);

    @Update("""
        UPDATE ap_hold
        SET status = #{targetStatus},
            active_flag = #{activeFlag},
            release_reason = #{releaseReason},
            updated_at = NOW(3)
        WHERE hold_id = #{holdId}
          AND status = #{currentStatus}
          AND active_flag = 1
        """)
    int updateStatusIfCurrent(@Param("holdId") String holdId,
                              @Param("currentStatus") String currentStatus,
                              @Param("targetStatus") String targetStatus,
                              @Param("activeFlag") Integer activeFlag,
                              @Param("releaseReason") String releaseReason);

    @Update("""
        UPDATE ap_hold
        SET status = 'CONFIRMED',
            updated_at = NOW(3)
        WHERE hold_id = #{holdId}
          AND status = 'HELD'
          AND active_flag = 1
        """)
    int markConfirmed(@Param("holdId") String holdId);

    @Select("""
        SELECT *
        FROM ap_hold
        WHERE status = 'HELD'
          AND expire_time < NOW(3)
        ORDER BY expire_time ASC
        LIMIT #{limit}
        """)
    List<ApHold> findExpiredHeld(@Param("limit") int limit);
}
