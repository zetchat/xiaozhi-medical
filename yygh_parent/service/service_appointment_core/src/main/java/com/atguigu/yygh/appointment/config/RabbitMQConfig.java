package com.atguigu.yygh.appointment.config;

import org.springframework.amqp.core.Binding;
import org.springframework.amqp.core.BindingBuilder;
import org.springframework.amqp.core.DirectExchange;
import org.springframework.amqp.core.Queue;
import org.springframework.amqp.core.QueueBuilder;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.util.HashMap;
import java.util.Map;

@Configuration
public class RabbitMQConfig {

    public static final String DELAY_EXCHANGE = "ap_delay_exchange";
    public static final String DELAY_ROUTING_KEY = "ap_delay_routing_key";
    public static final String DELAY_QUEUE = "ap_delay_queue";

    public static final String DLX_EXCHANGE = "ap_dead_letter_exchange";
    public static final String DLX_ROUTING_KEY = "ap_dead_letter_routing_key";
    public static final String DLQ_QUEUE = "ap_dead_letter_queue";

    @Bean
    public Queue appointmentDelayQueue() {
        Map<String, Object> args = new HashMap<>(2);
        args.put("x-dead-letter-exchange", DLX_EXCHANGE);
        args.put("x-dead-letter-routing-key", DLX_ROUTING_KEY);
        return QueueBuilder.durable(DELAY_QUEUE).withArguments(args).build();
    }

    @Bean
    public DirectExchange appointmentDelayExchange() {
        return new DirectExchange(DELAY_EXCHANGE);
    }

    @Bean
    public Binding appointmentDelayBinding(Queue appointmentDelayQueue, DirectExchange appointmentDelayExchange) {
        return BindingBuilder.bind(appointmentDelayQueue).to(appointmentDelayExchange).with(DELAY_ROUTING_KEY);
    }

    @Bean
    public Queue appointmentDeadLetterQueue() {
        return QueueBuilder.durable(DLQ_QUEUE).build();
    }

    @Bean
    public DirectExchange appointmentDeadLetterExchange() {
        return new DirectExchange(DLX_EXCHANGE);
    }

    @Bean
    public Binding appointmentDeadLetterBinding(Queue appointmentDeadLetterQueue,
                                                DirectExchange appointmentDeadLetterExchange) {
        return BindingBuilder.bind(appointmentDeadLetterQueue).to(appointmentDeadLetterExchange).with(DLX_ROUTING_KEY);
    }
}
