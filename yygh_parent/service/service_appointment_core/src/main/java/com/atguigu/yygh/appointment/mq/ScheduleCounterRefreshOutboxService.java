package com.atguigu.yygh.appointment.mq;

import com.atguigu.yygh.appointment.common.exception.AppointmentBizException;
import com.atguigu.yygh.appointment.domain.outbox.model.ApOutboxMessage;
import com.atguigu.yygh.appointment.infrastructure.mapper.ApOutboxMessageMapper;
import com.atguigu.yygh.common.trace.TraceContext;
import com.baomidou.mybatisplus.core.toolkit.IdWorker;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;

@Component
@RequiredArgsConstructor
public class ScheduleCounterRefreshOutboxService {

    public static final String BIZ_TYPE = "SCHEDULE_COUNTER_REFRESH";

    private final ApOutboxMessageMapper apOutboxMessageMapper;
    private final OutboxMessagePublisher outboxMessagePublisher;
    private final ObjectMapper objectMapper;

    public void enqueueRefresh(String scheduleId, String reason) {
        ApOutboxMessage message = new ApOutboxMessage();
        message.setMsgId(IdWorker.getIdStr());
        message.setBizType(BIZ_TYPE);
        message.setBizKey(scheduleId);
        message.setPayload(toJson(buildPayload(message.getMsgId(), scheduleId, reason)));
        message.setStatus("NEW");
        message.setRetryCount(0);
        message.setNextRetryTime(LocalDateTime.now());
        apOutboxMessageMapper.insert(message);
        outboxMessagePublisher.registerProjectionRefreshPublish(message);
    }

    private ScheduleCounterRefreshMessage buildPayload(String msgId, String scheduleId, String reason) {
        ScheduleCounterRefreshMessage payload = new ScheduleCounterRefreshMessage();
        payload.setMsgId(msgId);
        payload.setTraceId(TraceContext.getOrCreateTraceId());
        payload.setScheduleId(scheduleId);
        payload.setReason(reason);
        payload.setOccurredAt(LocalDateTime.now());
        return payload;
    }

    private String toJson(Object value) {
        try {
            return objectMapper.writeValueAsString(value);
        } catch (JsonProcessingException ex) {
            throw new AppointmentBizException("构建排班投影刷新消息失败");
        }
    }
}
