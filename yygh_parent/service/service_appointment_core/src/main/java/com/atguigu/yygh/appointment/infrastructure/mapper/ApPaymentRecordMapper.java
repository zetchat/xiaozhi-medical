package com.atguigu.yygh.appointment.infrastructure.mapper;

import com.atguigu.yygh.appointment.domain.payment.model.ApPaymentRecord;
import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

@Mapper
public interface ApPaymentRecordMapper extends BaseMapper<ApPaymentRecord> {

    @Select("""
        SELECT *
        FROM ap_payment_record
        WHERE pay_channel = #{payChannel}
          AND channel_trade_no = #{channelTradeNo}
        LIMIT 1
        """)
    ApPaymentRecord findByChannelTradeNo(@Param("payChannel") String payChannel,
                                         @Param("channelTradeNo") String channelTradeNo);

    @Select("""
        SELECT *
        FROM ap_payment_record
        WHERE order_id = #{orderId}
        ORDER BY created_at DESC
        LIMIT 1
        """)
    ApPaymentRecord findLatestByOrderId(@Param("orderId") String orderId);
}
