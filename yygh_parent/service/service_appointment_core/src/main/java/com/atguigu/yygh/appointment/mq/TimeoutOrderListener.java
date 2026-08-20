package com.atguigu.yygh.appointment.mq;

import com.atguigu.yygh.appointment.config.RabbitMQConfig;
import com.atguigu.yygh.appointment.application.OrderTimeoutService;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.core.Message;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.stereotype.Component;

@Component
@Slf4j
@RequiredArgsConstructor
public class TimeoutOrderListener {

    private final ObjectMapper objectMapper;
    private final OrderTimeoutService orderTimeoutService;

    @RabbitListener(queues = RabbitMQConfig.DLQ_QUEUE)
    public void handleTimeout(Message message) throws Exception {
        TimeoutOrderMessage timeoutMessage = objectMapper.readValue(message.getBody(), TimeoutOrderMessage.class);
        orderTimeoutService.closeIfExpired(
                timeoutMessage.getOrderId(),
                timeoutMessage.getHoldId(),
                timeoutMessage.getMsgId()
        );
    }
}
