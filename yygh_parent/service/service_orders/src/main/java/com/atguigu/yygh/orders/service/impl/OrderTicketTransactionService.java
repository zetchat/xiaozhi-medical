package com.atguigu.yygh.orders.service.impl;

import com.atguigu.yygh.model.order.TLocalMessageLog;
import com.atguigu.yygh.model.order.TOrder;
import com.atguigu.yygh.orders.dto.BookingRequest;
import com.atguigu.yygh.orders.mapper.TLocalMessageLogMapper;
import com.atguigu.yygh.orders.mapper.TOrderMapper;
import com.baomidou.mybatisplus.core.toolkit.IdWorker;
import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.rabbit.connection.CorrelationData;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;

@Service
@Slf4j
public class OrderTicketTransactionService {

    @Autowired
    private TOrderMapper tOrderMapper;

    @Autowired
    private TLocalMessageLogMapper messageLogMapper;

    @Autowired
    private RabbitTemplate rabbitTemplate;

    @Transactional(rollbackFor = Exception.class)
    public String createOrderAndMessage(BookingRequest request, String hisSeqNo) {
        String orderId = IdWorker.getIdStr();

        // 本地事务内先落订单，再落消息记录。
        TOrder order = new TOrder();
        order.setOrderId(orderId);
        order.setStatus("UNPAID");
        order.setHisSeqNo(hisSeqNo);
        order.setScheduleId(request.getScheduleId());
        order.setPatientId(request.getPatientId());
        tOrderMapper.insert(order);

        TLocalMessageLog msg = new TLocalMessageLog();
        msg.setMsgId(IdWorker.getIdStr());
        msg.setOrderId(orderId);
        msg.setStatus("NEW");
        messageLogMapper.insert(msg);

        TransactionSynchronizationManager.registerSynchronization(new TransactionSynchronization() {
            @Override
            public void afterCommit() {
                CorrelationData correlationData = new CorrelationData(msg.getMsgId());
                rabbitTemplate.convertAndSend("delay_exchange", "delay_routing_key", msg, message -> {
                    message.getMessageProperties().setExpiration("60000");
                    return message;
                }, correlationData);
                log.info("延迟关单消息首次投递已提交给MQ. msgId: {}, orderId: {}", msg.getMsgId(), orderId);
            }
        });

        return orderId;
    }
}
