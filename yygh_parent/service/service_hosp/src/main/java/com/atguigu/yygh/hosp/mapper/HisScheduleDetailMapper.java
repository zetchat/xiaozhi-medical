package com.atguigu.yygh.hosp.mapper;

import com.atguigu.yygh.model.hosp.HisScheduleDetail;
import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;
import org.apache.ibatis.annotations.Update;

public interface HisScheduleDetailMapper extends BaseMapper<HisScheduleDetail> {

    /**
     * 乐观锁扣减号源 (锁号)
     * 只有状态为 AVAILABLE 才能被锁定
     */
    @Update("UPDATE his_schedule_detail SET status = 'LOCKED' WHERE detail_id = #{detailId} AND status = 'AVAILABLE'")
    int lockScheduleDetail(@Param("detailId") String detailId);

    /**
     * 解锁号源 (退号)
     * 只有状态为 LOCKED 才能被释放回 AVAILABLE
     */
    @Update("UPDATE his_schedule_detail SET status = 'AVAILABLE' WHERE detail_id = #{detailId} AND status = 'LOCKED'")
    int unlockScheduleDetail(@Param("detailId") String detailId);

    /**
     * 找出一个可用的号源
     */
    @Select("SELECT * FROM his_schedule_detail WHERE schedule_id = #{scheduleId} AND status = 'AVAILABLE' LIMIT 1")
    HisScheduleDetail findAvailableDetail(@Param("scheduleId") String scheduleId);
}
