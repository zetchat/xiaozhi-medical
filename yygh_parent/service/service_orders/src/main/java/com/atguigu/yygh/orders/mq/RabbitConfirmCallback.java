package com.atguigu.yygh.orders.mq;

import com.atguigu.yygh.orders.mapper.TLocalMessageLogMapper;
import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.rabbit.connection.CorrelationData;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import javax.annotation.PostConstruct;

/**
 * 消息投递确认（Publisher Confirm）
 */
@Component
@Slf4j
public class RabbitConfirmCallback implements RabbitTemplate.ConfirmCallback {

    // TODO: 等待生成TLocalMessageLogMapper后取消注释
    // @Autowired
    // private TLocalMessageLogMapper messageLogMapper;

    @Autowired
    private RabbitTemplate rabbitTemplate;

    @PostConstruct
    public void init() {
        rabbitTemplate.setConfirmCallback(this);
    }

    @Override
    public void confirm(CorrelationData correlationData, boolean ack, String cause) {
        if (correlationData == null) {
            return;
        }
        
        String msgId = correlationData.getId();
        if (ack) {
            // 投递成功，更新消息表状态
            log.info("消息投递MQ成功, msgId: {}", msgId);
            // messageLogMapper.updateStatus(msgId, "PUBLISHED");
        } else {
            // 投递失败交给定时任务重试
            log.error("消息投递MQ失败, msgId: {}, cause: {}", msgId, cause);
        }
    }
}
