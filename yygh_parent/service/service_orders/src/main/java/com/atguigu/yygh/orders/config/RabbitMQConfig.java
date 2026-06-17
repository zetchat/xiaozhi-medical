package com.atguigu.yygh.orders.config;

import org.springframework.amqp.core.*;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.util.HashMap;
import java.util.Map;

@Configuration
public class RabbitMQConfig {

    // 延迟交换机（业务发送消息到此）
    public static final String DELAY_EXCHANGE = "delay_exchange";
    // 延迟路由键
    public static final String DELAY_ROUTING_KEY = "delay_routing_key";
    // 延迟队列（消息在此等待，不设置消费者）
    public static final String DELAY_QUEUE = "delay_queue";

    // 死信交换机（延迟队列超时后，消息转发到此）
    public static final String DLX_EXCHANGE = "dead_letter_exchange";
    // 死信路由键
    public static final String DLX_ROUTING_KEY = "dead_letter_routing_key";
    // 死信队列（真正被消费的队列，如关单监听器）
    public static final String DLQ_QUEUE = "dead_letter_queue";

    /**
     * 声明延迟队列（带死信配置）
     */
    @Bean
    public Queue delayQueue() {
        Map<String, Object> args = new HashMap<>(3);
        // x-dead-letter-exchange：配置死信交换机
        args.put("x-dead-letter-exchange", DLX_EXCHANGE);
        // x-dead-letter-routing-key：配置死信路由键
        args.put("x-dead-letter-routing-key", DLX_ROUTING_KEY);
        // 可以在发送消息时动态设置 expiration，所以这里可以不配全局 TTL
        return QueueBuilder.durable(DELAY_QUEUE).withArguments(args).build();
    }

    /**
     * 声明延迟交换机
     */
    @Bean
    public DirectExchange delayExchange() {
        return new DirectExchange(DELAY_EXCHANGE);
    }

    /**
     * 绑定延迟队列到延迟交换机
     */
    @Bean
    public Binding delayBinding(Queue delayQueue, DirectExchange delayExchange) {
        return BindingBuilder.bind(delayQueue).to(delayExchange).with(DELAY_ROUTING_KEY);
    }

    // ==========================================

    /**
     * 声明死信队列（实际消费队列）
     */
    @Bean
    public Queue deadLetterQueue() {
        return new Queue(DLQ_QUEUE, true);
    }

    /**
     * 声明死信交换机
     */
    @Bean
    public DirectExchange deadLetterExchange() {
        return new DirectExchange(DLX_EXCHANGE);
    }

    /**
     * 绑定死信队列到死信交换机
     */
    @Bean
    public Binding deadLetterBinding(Queue deadLetterQueue, DirectExchange deadLetterExchange) {
        return BindingBuilder.bind(deadLetterQueue).to(deadLetterExchange).with(DLX_ROUTING_KEY);
    }
}
