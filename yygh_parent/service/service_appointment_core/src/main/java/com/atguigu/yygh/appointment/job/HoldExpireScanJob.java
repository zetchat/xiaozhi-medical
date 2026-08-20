package com.atguigu.yygh.appointment.job;

import com.atguigu.yygh.appointment.application.OrderTimeoutService;
import com.atguigu.yygh.appointment.domain.order.model.ApOrder;
import com.atguigu.yygh.appointment.infrastructure.mapper.ApOrderMapper;
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
        List<ApOrder> expiredOrders = apOrderMapper.findExpiredUnpaid(50);
        for (ApOrder order : expiredOrders) {
            try {
                orderTimeoutService.closeIfExpired(order.getOrderId(), order.getHoldId(), null);
            } catch (Exception ex) {
                log.error("扫描超时订单处理失败, orderId={}, holdId={}",
                        order.getOrderId(), order.getHoldId(), ex);
            }
        }
    }
}
