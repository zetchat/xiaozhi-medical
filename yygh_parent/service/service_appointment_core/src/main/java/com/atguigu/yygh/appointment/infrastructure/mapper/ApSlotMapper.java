package com.atguigu.yygh.appointment.infrastructure.mapper;

import com.atguigu.yygh.appointment.domain.slot.model.ApSlot;
import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;
import org.apache.ibatis.annotations.Update;

@Mapper
public interface ApSlotMapper extends BaseMapper<ApSlot> {

    @Select("""
        SELECT *
        FROM ap_slot
        WHERE schedule_id = #{scheduleId}
          AND status = 'AVAILABLE'
        ORDER BY sequence_no ASC
        LIMIT 1
        FOR UPDATE SKIP LOCKED
        """)
    ApSlot selectFirstAvailableForUpdate(@Param("scheduleId") String scheduleId);

    @Update("""
        UPDATE ap_slot
        SET status = 'HELD',
            hold_id = #{holdId},
            patient_id = #{patientId},
            locked_at = NOW(3),
            updated_at = NOW(3)
        WHERE slot_id = #{slotId}
          AND status = 'AVAILABLE'
        """)
    int markHeld(@Param("slotId") String slotId,
                 @Param("holdId") String holdId,
                 @Param("patientId") String patientId);

    @Update("""
        UPDATE ap_slot
        SET status = 'AVAILABLE',
            hold_id = NULL,
            patient_id = NULL,
            released_at = NOW(3),
            updated_at = NOW(3)
        WHERE slot_id = #{slotId}
          AND hold_id = #{holdId}
          AND status = 'HELD'
        """)
    int releaseHeld(@Param("slotId") String slotId,
                    @Param("holdId") String holdId);

    @Update("""
        UPDATE ap_slot
        SET status = 'CONFIRMED',
            confirmed_at = NOW(3),
            updated_at = NOW(3)
        WHERE slot_id = #{slotId}
          AND hold_id = #{holdId}
          AND status = 'HELD'
        """)
    int confirmHeld(@Param("slotId") String slotId,
                    @Param("holdId") String holdId);

    @Select("""
        SELECT COUNT(1)
        FROM ap_slot
        WHERE schedule_id = #{scheduleId}
        """)
    int countBySchedule(@Param("scheduleId") String scheduleId);

    @Select("""
        SELECT COUNT(1)
        FROM ap_slot
        WHERE schedule_id = #{scheduleId}
          AND status = #{status}
        """)
    int countByScheduleAndStatus(@Param("scheduleId") String scheduleId,
                                 @Param("status") String status);
}
