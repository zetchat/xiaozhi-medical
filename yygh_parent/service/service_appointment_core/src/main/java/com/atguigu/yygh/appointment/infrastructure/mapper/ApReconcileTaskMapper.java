package com.atguigu.yygh.appointment.infrastructure.mapper;

import com.atguigu.yygh.appointment.domain.reconcile.model.ApReconcileTask;
import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

import java.util.List;

@Mapper
public interface ApReconcileTaskMapper extends BaseMapper<ApReconcileTask> {

    @Select("""
        SELECT COUNT(1)
        FROM ap_reconcile_task
        WHERE task_type = #{taskType}
          AND biz_key = #{bizKey}
          AND status IN ('NEW', 'PROCESSING')
        """)
    int countOpenTask(@Param("taskType") String taskType,
                      @Param("bizKey") String bizKey);

    @Select("""
        SELECT *
        FROM ap_reconcile_task
        ORDER BY created_at DESC
        LIMIT #{limit}
        """)
    List<ApReconcileTask> findRecentTasks(@Param("limit") int limit);
}
