package com.atguigu.yygh.orders.mq;

import com.alibaba.fastjson.JSON;
import com.atguigu.yygh.common.result.Result;
import com.atguigu.yygh.model.order.TLocalMessageLog;
import com.atguigu.yygh.model.order.TOrder;
import com.atguigu.yygh.order.client.HisRpcClient;
import com.rabbitmq.client.Channel;
import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.core.Message;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import com.atguigu.yygh.orders.service.RedisService;
import com.atguigu.yygh.orders.mapper.TOrderMapper;

/**
 * 死信队列超时闭环（包含“毒药重试”修复）
 */
@Component
@Slf4j
public class TimeoutCancelListener {

    @Autowired
    private TOrderMapper orderMapper;
    @Autowired
    private RedisService redisService;
    @Autowired
    private HisRpcClient hisRpcClient;

    @RabbitListener(queues = "dead_letter_queue")
    @Transactional(rollbackFor = Exception.class)
    public void handleTimeoutOrder(Message message, Channel channel) throws Exception {
        String payload = new String(message.getBody());
        TLocalMessageLog msgLog = JSON.parseObject(payload, TLocalMessageLog.class);
        String orderId = msgLog.getOrderId();

        // 1. 查本地订单当前状态
        TOrder order = orderMapper.selectById(orderId);
        
        if (order != null && "UNPAID".equals(order.getStatus())) {
            log.info("订单超时未支付，执行关单解锁流程: {}", orderId);

            // 【核心修复】：2. 必须先调用外部 HIS 系统解锁！
            Result hisResult = hisRpcClient.unlockTicket(order.getHisSeqNo());
            
            if (!hisResult.isOk()) {
                // HIS 网络异常，抛出异常让 MQ 直接 NACK。
                // 因为本地状态依然是 UNPAID，下次重试时仍能正确进入 if 块！
                throw new RuntimeException("HIS解锁号源失败，触发 MQ 重新投递消费");
            }

            // 3. HIS 解锁成功后，更新本地数据库状态
            orderMapper.updateStatus(orderId, "CANCELLED");

            // 4. 号源退回 Redis 票池
            redisService.incrementStock("TICKET_POOL:" + order.getScheduleId());
            
            log.info("关单完成，资源已全部释放: {}", orderId);
        }

        // 5. 订单是 PAID 或 已 CANCELLED，均视为正常消费，手动 ACK
        channel.basicAck(message.getMessageProperties().getDeliveryTag(), false);
    }
}
