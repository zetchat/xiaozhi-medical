package com.atguigu.yygh.appointment.mq;

import com.atguigu.yygh.appointment.infrastructure.mapper.ApOutboxMessageMapper;
import com.atguigu.yygh.common.trace.TraceContext;
import jakarta.annotation.PostConstruct;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.rabbit.connection.CorrelationData;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.stereotype.Component;

@Component
@Slf4j
@RequiredArgsConstructor
public class RabbitConfirmCallback implements RabbitTemplate.ConfirmCallback {

    private final ApOutboxMessageMapper apOutboxMessageMapper;
    private final RabbitTemplate rabbitTemplate;

    @PostConstruct
    public void init() {
        rabbitTemplate.setConfirmCallback(this);
    }

    @Override
    public void confirm(CorrelationData correlationData, boolean ack, String cause) {
        String traceId = TraceContext.generateTraceId();
        if (correlationData instanceof TraceCorrelationData) {
            traceId = ((TraceCorrelationData) correlationData).getTraceId();
        }
        TraceContext.setTraceId(traceId);
        try {
            if (correlationData == null) {
                log.warn("预约消息确认缺少 correlationData, traceId: {}", traceId);
                return;
            }
            String msgId = correlationData.getId();
            if (ack) {
                apOutboxMessageMapper.updateStatusIfCurrent(msgId, "NEW", "PUBLISHED");
                log.info("预约消息投递成功, traceId: {}, msgId: {}", traceId, msgId);
                return;
            }
            log.error("预约消息投递失败, traceId: {}, msgId: {}, cause: {}", traceId, msgId, cause);
        } finally {
            TraceContext.clear();
        }
    }
}
