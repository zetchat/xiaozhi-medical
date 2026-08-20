package com.atguigu.yygh.appointment.infrastructure.mapper;

import com.atguigu.yygh.appointment.domain.order.model.ApOrder;
import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;
import org.apache.ibatis.annotations.Update;

import java.util.List;

@Mapper
public interface ApOrderMapper extends BaseMapper<ApOrder> {

    @Update("""
        UPDATE ap_order
        SET status = #{targetStatus},
            cancel_reason = #{cancelReason},
            updated_at = NOW(3)
        WHERE order_id = #{orderId}
          AND status = #{currentStatus}
        """)
    int updateStatusIfCurrent(@Param("orderId") String orderId,
                              @Param("currentStatus") String currentStatus,
                              @Param("targetStatus") String targetStatus,
                              @Param("cancelReason") String cancelReason);

    @Update("""
        UPDATE ap_order
        SET status = 'PAID',
            pay_time = #{payTime},
            updated_at = NOW(3)
        WHERE order_id = #{orderId}
          AND status = 'UNPAID'
        """)
    int markPaid(@Param("orderId") String orderId,
                 @Param("payTime") java.time.LocalDateTime payTime);

    @Select("""
        SELECT *
        FROM ap_order
        WHERE status = 'UNPAID'
          AND pay_deadline < NOW(3)
        ORDER BY pay_deadline ASC
        LIMIT #{limit}
        """)
    List<ApOrder> findExpiredUnpaid(@Param("limit") int limit);
}
