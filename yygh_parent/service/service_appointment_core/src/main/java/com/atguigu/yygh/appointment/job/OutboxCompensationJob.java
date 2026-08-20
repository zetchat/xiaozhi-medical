package com.atguigu.yygh.appointment.job;

import com.atguigu.yygh.appointment.domain.outbox.model.ApOutboxMessage;
import com.atguigu.yygh.appointment.infrastructure.mapper.ApOutboxMessageMapper;
import com.atguigu.yygh.appointment.mq.OutboxMessagePublisher;
import com.atguigu.yygh.appointment.mq.TimeoutOrderMessage;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;
import java.util.List;

@Component
@Slf4j
@RequiredArgsConstructor
public class OutboxCompensationJob {

    private final ApOutboxMessageMapper apOutboxMessageMapper;
    private final OutboxMessagePublisher outboxMessagePublisher;
    private final ObjectMapper objectMapper;

    @Scheduled(cron = "0 */1 * * * ?")
    public void compensate() {
        List<ApOutboxMessage> messages = apOutboxMessageMapper.findRetryableMessages(50);
        for (ApOutboxMessage message : messages) {
            try {
                TimeoutOrderMessage payload = objectMapper.readValue(message.getPayload(), TimeoutOrderMessage.class);
                outboxMessagePublisher.publishTimeoutMessage(message, payload.getExpireTime());
                apOutboxMessageMapper.updateRetry(message.getMsgId(), LocalDateTime.now().plusMinutes(1), null);
                log.info("Outbox 补偿投递成功, msgId={}, bizKey={}", message.getMsgId(), message.getBizKey());
            } catch (Exception ex) {
                apOutboxMessageMapper.updateRetry(message.getMsgId(), LocalDateTime.now().plusMinutes(5), ex.getMessage());
                log.error("Outbox 补偿投递失败, msgId={}", message.getMsgId(), ex);
            }
        }
    }
}
