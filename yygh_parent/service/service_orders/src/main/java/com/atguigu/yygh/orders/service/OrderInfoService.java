package com.atguigu.yygh.orders.service;

import com.atguigu.yygh.model.order.OrderInfo;
import com.atguigu.yygh.vo.order.OrderCountQueryVo;
import com.atguigu.yygh.vo.order.OrderCountVo;
import com.baomidou.mybatisplus.extension.service.IService;

import java.util.List;
import java.util.Map;

/**
 * 订单表 服务类
 */
public interface OrderInfoService extends IService<OrderInfo> {

    //平台下单
    Long createOrder(String scheduleId, Long patientId);

    //获取订单详情
    OrderInfo getOrderInfo(Long orderId);

    //取消预约挂号
    Boolean cancelOrder(Long orderId);

    //给这一天所有就诊人发短信提醒
    void patientTips(String dateString);

    //统计每天平台预约数据
    Map<String,Object> selectOrderCount(OrderCountQueryVo orderCountQueryVo);
}
