package com.atguigu.yygh.orders.job;

import com.atguigu.yygh.model.order.TLocalMessageLog;
import com.atguigu.yygh.orders.mapper.TLocalMessageLogMapper;
import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.util.List;

/**
 * 消息防丢兜底（定时补偿任务）
 */
@Component
@Slf4j
public class MessageCompensationJob {

    @Autowired
    private TLocalMessageLogMapper messageLogMapper;
    
    @Autowired
    private RabbitTemplate rabbitTemplate;
    
    // @Autowired
    // private AlertService alertService;

    @Scheduled(cron = "0 * * * * ?") // 每分钟执行
    public void compensate() {
        log.info("开始执行定时补偿任务...");
        
        // 捞出创建时间超过1分钟，且状态仍为 NEW 的记录
        List<TLocalMessageLog> stagnantMsgs = messageLogMapper.findStagnantMessages();

        for (TLocalMessageLog msg : stagnantMsgs) {
            if (msg.getRetryCount() >= 3) {
                messageLogMapper.updateStatus(msg.getMsgId(), "FAIL");
                // TODO: 接入真正的短信/钉钉告警
                // alertService.sendDingTalk("【MQ告警】延迟关单消息重试3次失败，请检查 MQ 集群状态！");
                log.error("【MQ告警】延迟关单消息重试3次失败，msgId: {}", msg.getMsgId());
                continue;
            }

            messageLogMapper.incrementRetryCount(msg.getMsgId());
            
            // 重新投递
            rabbitTemplate.convertAndSend("delay_exchange", "delay_routing_key", msg, message -> {
                message.getMessageProperties().setExpiration("900000"); // 15分钟
                return message;
            });
            log.info("定时任务补偿发送消息成功: {}", msg.getMsgId());
        }
    }
}
