package com.atguigu.yygh.hosp.mapper;

import com.atguigu.yygh.model.hosp.HisLockRecord;
import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;
import org.apache.ibatis.annotations.Update;

public interface HisLockRecordMapper extends BaseMapper<HisLockRecord> {

    @Select("SELECT * FROM his_lock_record WHERE schedule_id = #{scheduleId} AND patient_id = #{patientId} LIMIT 1")
    HisLockRecord findByScheduleAndPatient(@Param("scheduleId") String scheduleId, @Param("patientId") String patientId);

    @Update("UPDATE his_lock_record SET status = #{status} WHERE his_seq_no = #{hisSeqNo}")
    int updateStatus(@Param("hisSeqNo") String hisSeqNo, @Param("status") String status);
}
