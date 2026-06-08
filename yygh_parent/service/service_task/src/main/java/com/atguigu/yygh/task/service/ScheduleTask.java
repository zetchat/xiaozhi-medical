package com.atguigu.yygh.task.service;

import com.atguigu.yygh.rabbit.RabbitService;
import com.atguigu.yygh.rabbit.constant.MqConst;
import org.joda.time.DateTime;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.scheduling.annotation.EnableScheduling;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

@Component
@EnableScheduling //开启定时任务功能
public class ScheduleTask {

    @Autowired
    RabbitService rabbitService;

    //定时表达式7个组成部分：秒 分  时  日  月  周 年    一般年省略不写
    //@Scheduled(cron = "0 0 20 * * ?")
    @Scheduled(cron = "0/10 * * * * ?")
    public void taskSendMsg(){
        DateTime now = new DateTime();
        DateTime dateTime = now.plusDays(1); //  计算从订单表查询，需要进行预约提醒的订单，给该订单就诊人发信息
        rabbitService.sendMessage(MqConst.EXCHANGE_DIRECT_TASK,MqConst.ROUTING_TASK_8,dateTime.toString("yyyy-MM-dd"));
    }
}
