package com.atguigu.yygh.appointment.mq;

import com.atguigu.yygh.appointment.config.RabbitMQConfig;
import com.atguigu.yygh.appointment.domain.outbox.model.ApOutboxMessage;
import com.atguigu.yygh.common.trace.TraceContext;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.stereotype.Component;
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;

import java.time.Duration;
import java.time.LocalDateTime;

@Component
@Slf4j
@RequiredArgsConstructor
public class OutboxMessagePublisher {

    private final RabbitTemplate rabbitTemplate;

    public void registerTimeoutPublish(ApOutboxMessage message, LocalDateTime expireTime) {
        TransactionSynchronizationManager.registerSynchronization(new TransactionSynchronization() {
            @Override
            public void afterCommit() {
                publishTimeoutMessage(message, expireTime);
            }
        });
    }

    public void registerProjectionRefreshPublish(ApOutboxMessage message) {
        TransactionSynchronizationManager.registerSynchronization(new TransactionSynchronization() {
            @Override
            public void afterCommit() {
                publishProjectionRefreshMessage(message);
            }
        });
    }

    public void publishTimeoutMessage(ApOutboxMessage message, LocalDateTime expireTime) {
        String traceId = TraceContext.getOrCreateTraceId();
        TraceCorrelationData correlationData = new TraceCorrelationData(message.getMsgId(), traceId);
        rabbitTemplate.convertAndSend(
                RabbitMQConfig.DELAY_EXCHANGE,
                RabbitMQConfig.DELAY_ROUTING_KEY,
                message.getPayload(),
                mqMessage -> {
                    mqMessage.getMessageProperties().setExpiration(String.valueOf(computeDelayMillis(expireTime)));
                    mqMessage.getMessageProperties().setHeader(TraceContext.TRACE_HEADER, traceId);
                    return mqMessage;
                },
                correlationData
        );
        log.info("预约超时消息已提交MQ, traceId: {}, msgId: {}, bizKey: {}",
                traceId, message.getMsgId(), message.getBizKey());
    }

    public void publishProjectionRefreshMessage(ApOutboxMessage message) {
        String traceId = TraceContext.getOrCreateTraceId();
        TraceCorrelationData correlationData = new TraceCorrelationData(message.getMsgId(), traceId);
        rabbitTemplate.convertAndSend(
                RabbitMQConfig.PROJECTION_EXCHANGE,
                RabbitMQConfig.PROJECTION_ROUTING_KEY,
                message.getPayload(),
                mqMessage -> {
                    mqMessage.getMessageProperties().setHeader(TraceContext.TRACE_HEADER, traceId);
                    return mqMessage;
                },
                correlationData
        );
        log.info("排班投影刷新消息已提交MQ, traceId: {}, msgId: {}, bizKey: {}",
                traceId, message.getMsgId(), message.getBizKey());
    }

    private long computeDelayMillis(LocalDateTime expireTime) {
        long delay = Duration.between(LocalDateTime.now(), expireTime).toMillis();
        return Math.max(delay, 1000L);
    }
}
