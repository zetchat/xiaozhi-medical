package com.atguigu.yygh.orders.mq;

import com.atguigu.yygh.orders.mapper.TLocalMessageLogMapper;
import jakarta.annotation.PostConstruct;
import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.rabbit.connection.CorrelationData;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;


/**
 * 消息投递确认（Publisher Confirm）
 */
@Component
@Slf4j
public class RabbitConfirmCallback implements RabbitTemplate.ConfirmCallback {

    @Autowired
    private TLocalMessageLogMapper messageLogMapper;

    @Autowired
    private RabbitTemplate rabbitTemplate;

    @PostConstruct
    public void init() {
        rabbitTemplate.setConfirmCallback(this);
    }

    @Override
    public void confirm(CorrelationData correlationData, boolean ack, String cause) {
        if (correlationData == null) {
            log.warn("消息投递确认缺少 correlationData，无法更新消息状态");
            return;
        }
        
        String msgId = correlationData.getId();
        if (ack) {
            int updatedRows = messageLogMapper.updateStatusIfCurrent(msgId, "NEW", "PUBLISHED");
            if (updatedRows > 0) {
                log.info("消息投递MQ成功, msgId: {}", msgId);
            } else {
                log.info("消息投递确认到达，但消息状态已前移，跳过回写. msgId: {}", msgId);
            }
        } else {
            // 投递失败交给定时任务重试
            log.error("消息投递MQ失败, msgId: {}, cause: {}", msgId, cause);
        }
    }
}
