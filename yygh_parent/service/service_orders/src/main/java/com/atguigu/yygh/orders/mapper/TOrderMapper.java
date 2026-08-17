package com.atguigu.yygh.orders.mapper;

import com.atguigu.yygh.model.order.TOrder;
import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Update;

public interface TOrderMapper extends BaseMapper<TOrder> {
    
    @Update("UPDATE t_order SET status = #{status} WHERE order_id = #{orderId}")
    void updateStatus(@Param("orderId") String orderId, @Param("status") String status);

    @Update("UPDATE t_order SET status = #{targetStatus} WHERE order_id = #{orderId} AND status = #{sourceStatus}")
    int updateStatusIfCurrent(@Param("orderId") String orderId,
                              @Param("sourceStatus") String sourceStatus,
                              @Param("targetStatus") String targetStatus);
}
