package com.atguigu.yygh.orders.mq;

import com.alibaba.fastjson.JSON;
import com.atguigu.yygh.common.result.Result;
import com.atguigu.yygh.model.order.TLocalMessageLog;
import com.atguigu.yygh.model.order.TOrder;
import com.atguigu.yygh.order.client.HisRpcClient;
import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.core.Message;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import com.atguigu.yygh.orders.mapper.TLocalMessageLogMapper;
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
    @Autowired
    private TLocalMessageLogMapper messageLogMapper;

    @RabbitListener(queues = "dead_letter_queue")
    @Transactional(rollbackFor = Exception.class)
    public void handleTimeoutOrder(Message message) {
        String payload = new String(message.getBody());
        TLocalMessageLog msgLog = JSON.parseObject(payload, TLocalMessageLog.class);
        String orderId = msgLog.getOrderId();
        String msgId = msgLog.getMsgId();

        // 1. 查本地订单当前状态
        TOrder order = orderMapper.selectById(orderId);
        if (order == null) {
            markMessageConsumed(msgId);
            log.warn("超时关单消息对应订单不存在，忽略消费. msgId: {}, orderId: {}", msgId, orderId);
            return;
        }
        
        if (!"UNPAID".equals(order.getStatus())) {
            markMessageConsumed(msgId);
            log.info("订单状态无需超时关单，忽略消费. msgId: {}, orderId: {}, currentStatus: {}",
                    msgId, orderId, order.getStatus());
            return;
        }

        log.info("订单超时未支付，执行关单解锁流程. msgId: {}, orderId: {}, hisSeqNo: {}",
                msgId, orderId, order.getHisSeqNo());
            
        // 2. 先调用 HIS 解锁，失败时抛异常，由容器按 AUTO 模式回滚并触发重试
        Result hisResult = hisRpcClient.unlockTicket(order.getHisSeqNo());
        if (!hisResult.isOk()) {
            throw new RuntimeException("HIS解锁号源失败，触发 MQ 重新投递消费, orderId: " + orderId);
        }

        // 3. 原子更新本地订单状态，只有第一次成功抢到关单资格的消息才继续回滚 Redis
        int updatedRows = orderMapper.updateStatusIfCurrent(orderId, "UNPAID", "CANCELLED");
        if (updatedRows == 0) {
            markMessageConsumed(msgId);
            log.warn("订单已被其他并发消费者处理，跳过重复回滚. msgId: {}, orderId: {}", msgId, orderId);
            return;
        }

        // 4. 号源退回 Redis 票池
        redisService.incrementStock("TICKET_POOL:" + order.getScheduleId());
        markMessageConsumed(msgId);
            
        log.info("关单完成，资源已全部释放. msgId: {}, orderId: {}", msgId, orderId);
    }

    private void markMessageConsumed(String msgId) {
        int publishedUpdated = messageLogMapper.updateStatusIfCurrent(msgId, "PUBLISHED", "CONSUMED");
        if (publishedUpdated == 0) {
            messageLogMapper.updateStatusIfCurrent(msgId, "NEW", "CONSUMED");
        }
    }
}
