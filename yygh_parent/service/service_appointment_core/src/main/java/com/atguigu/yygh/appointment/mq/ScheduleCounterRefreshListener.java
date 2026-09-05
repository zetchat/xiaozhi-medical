package com.atguigu.yygh.appointment.mq;

import com.atguigu.yygh.appointment.config.RabbitMQConfig;
import com.atguigu.yygh.appointment.common.exception.AppointmentBizException;
import com.atguigu.yygh.appointment.domain.shared.enums.SlotStatus;
import com.atguigu.yygh.appointment.infrastructure.mapper.ApOutboxMessageMapper;
import com.atguigu.yygh.appointment.infrastructure.mapper.ApScheduleMapper;
import com.atguigu.yygh.appointment.infrastructure.mapper.ApSlotMapper;
import com.atguigu.yygh.common.trace.TraceContext;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.core.Message;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

@Component
@Slf4j
@RequiredArgsConstructor
public class ScheduleCounterRefreshListener {

    private final ObjectMapper objectMapper;
    private final ApScheduleMapper apScheduleMapper;
    private final ApSlotMapper apSlotMapper;
    private final ApOutboxMessageMapper apOutboxMessageMapper;

    @RabbitListener(queues = RabbitMQConfig.PROJECTION_QUEUE)
    public void handleRefresh(Message message) throws Exception {
        ScheduleCounterRefreshMessage refreshMessage =
                objectMapper.readValue(message.getBody(), ScheduleCounterRefreshMessage.class);
        String headerTraceId = message.getMessageProperties().getHeader(TraceContext.TRACE_HEADER);
        String traceId = StringUtils.hasText(refreshMessage.getTraceId())
                ? refreshMessage.getTraceId()
                : StringUtils.hasText(headerTraceId)
                ? headerTraceId
                : TraceContext.generateTraceId();
        try {
            TraceContext.setTraceId(traceId);
            String scheduleId = refreshMessage.getScheduleId();
            int total = apSlotMapper.countBySchedule(scheduleId);
            int available = apSlotMapper.countByScheduleAndStatus(scheduleId, SlotStatus.AVAILABLE.name());
            int held = apSlotMapper.countByScheduleAndStatus(scheduleId, SlotStatus.HELD.name());
            int confirmed = apSlotMapper.countByScheduleAndStatus(scheduleId, SlotStatus.CONFIRMED.name());
            int updated = apScheduleMapper.syncProjectionCounters(scheduleId, total, available, held, confirmed);
            if (updated == 0) {
                throw new AppointmentBizException("排班投影刷新失败，排班不存在: " + scheduleId);
            }
            markConsumed(refreshMessage.getMsgId());
            log.info("排班投影刷新完成, traceId: {}, msgId: {}, scheduleId: {}, available: {}, held: {}, confirmed: {}",
                    traceId, refreshMessage.getMsgId(), scheduleId, available, held, confirmed);
        } finally {
            TraceContext.clear();
        }
    }

    private void markConsumed(String msgId) {
        if (!StringUtils.hasText(msgId)) {
            return;
        }
        int publishedUpdated = apOutboxMessageMapper.updateStatusIfCurrent(msgId, "PUBLISHED", "CONSUMED");
        if (publishedUpdated == 0) {
            apOutboxMessageMapper.updateStatusIfCurrent(msgId, "NEW", "CONSUMED");
        }
    }
}
