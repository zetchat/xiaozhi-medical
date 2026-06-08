package com.atguigu.yygh.hosp.receiver;

import com.atguigu.yygh.hosp.service.ScheduleService;
import com.atguigu.yygh.model.hosp.Schedule;
import com.atguigu.yygh.rabbit.RabbitService;
import com.atguigu.yygh.rabbit.constant.MqConst;
import com.atguigu.yygh.vo.msm.MsmVo;
import com.atguigu.yygh.vo.order.OrderMqVo;
import com.rabbitmq.client.Channel;
import org.springframework.amqp.core.Message;
import org.springframework.amqp.rabbit.annotation.Exchange;
import org.springframework.amqp.rabbit.annotation.Queue;
import org.springframework.amqp.rabbit.annotation.QueueBinding;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.io.IOException;

@Component
public class HospitalReceiver {
    @Autowired
    ScheduleService scheduleService;

    @Autowired
    RabbitService rabbitService;


    //监听下单后，订单服务发送的消息： 修改排班号源数量信息
    @RabbitListener(bindings = @QueueBinding(
            value = @Queue(value = MqConst.QUEUE_ORDER, durable = "true"),
            exchange = @Exchange(value = MqConst.EXCHANGE_DIRECT_ORDER),
            key = {MqConst.ROUTING_ORDER}
    ))
    public void receiver(OrderMqVo orderMqVo, Message message, Channel channel) throws IOException {

        if(orderMqVo.getAvailableNumber() !=null){ //预约挂号处理里
            String scheduleId = orderMqVo.getScheduleId();
            Schedule schedule = scheduleService.getScheduleId(scheduleId); //含医院名称和科室名称
            schedule.setAvailableNumber(orderMqVo.getAvailableNumber()); //剩余号源数量
            schedule.setReservedNumber(orderMqVo.getReservedNumber()); //总可预约号源数量
            scheduleService.update(schedule); //更新mongo数据库的排班集合的数据

            MsmVo msmVo = orderMqVo.getMsmVo();
            if(msmVo!=null){
                //发mq消息，通知消费者端给用户发手机短信
                rabbitService.sendMessage(MqConst.EXCHANGE_DIRECT_MSM,MqConst.ROUTING_MSM_ITEM,msmVo);
            }
        }else{ //取消预约挂号处理
            String scheduleId = orderMqVo.getScheduleId();
            Schedule schedule = scheduleService.getScheduleId(scheduleId); //含医院名称和科室名称
            schedule.setAvailableNumber(schedule.getAvailableNumber()+1); //剩余号源数量
            scheduleService.update(schedule); //更新mongo数据库的排班集合的数据

            MsmVo msmVo = orderMqVo.getMsmVo();
            if(msmVo!=null){
                //发mq消息，通知消费者端给用户发手机短信
                rabbitService.sendMessage(MqConst.EXCHANGE_DIRECT_MSM,MqConst.ROUTING_MSM_ITEM,msmVo);
            }
        }


    }

}
