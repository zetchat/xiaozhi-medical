package com.atguigu.yygh.appointment.job;

import com.atguigu.yygh.appointment.application.OrderTimeoutService;
import com.atguigu.yygh.appointment.domain.order.model.ApOrder;
import com.atguigu.yygh.appointment.infrastructure.mapper.ApOrderMapper;
import com.atguigu.yygh.common.trace.TraceContext;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.util.List;

@Component
@Slf4j
@RequiredArgsConstructor
public class HoldExpireScanJob {

    private final ApOrderMapper apOrderMapper;
    private final OrderTimeoutService orderTimeoutService;

    @Scheduled(cron = "30 */1 * * * ?")
    public void scanExpiredOrders() {
        String traceId = TraceContext.generateTraceId();
        try {
            TraceContext.setTraceId(traceId);
            List<ApOrder> expiredOrders = apOrderMapper.findExpiredUnpaid(50);
            log.info("开始扫描超时订单, traceId: {}, size: {}", traceId, expiredOrders.size());
            for (ApOrder order : expiredOrders) {
                try {
                    orderTimeoutService.closeIfExpired(order.getOrderId(), order.getHoldId(), null);
                } catch (Exception ex) {
                    log.error("扫描超时订单处理失败, traceId: {}, orderId: {}, holdId: {}",
                            traceId, order.getOrderId(), order.getHoldId(), ex);
                }
            }
        } finally {
            TraceContext.clear();
        }
    }
}
