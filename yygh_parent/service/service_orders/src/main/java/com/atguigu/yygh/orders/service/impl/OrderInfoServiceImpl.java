package com.atguigu.yygh.orders.service.impl;

import com.alibaba.fastjson.JSONObject;
import com.atguigu.yygh.common.exception.YyghException;
import com.atguigu.yygh.enums.OrderStatusEnum;
import com.atguigu.yygh.hosp.client.HospitalFeignClient;
import com.atguigu.yygh.model.order.OrderInfo;
import com.atguigu.yygh.model.user.Patient;
import com.atguigu.yygh.orders.mapper.OrderInfoMapper;
import com.atguigu.yygh.orders.service.OrderInfoService;
import com.atguigu.yygh.orders.utils.HttpRequestHelper;
import com.atguigu.yygh.rabbit.RabbitService;
import com.atguigu.yygh.rabbit.constant.MqConst;
import com.atguigu.yygh.user.client.PatientFeignClient;
import com.atguigu.yygh.vo.hosp.ScheduleOrderVo;
import com.atguigu.yygh.vo.msm.MsmVo;
import com.atguigu.yygh.vo.order.OrderCountQueryVo;
import com.atguigu.yygh.vo.order.OrderCountVo;
import com.atguigu.yygh.vo.order.OrderMqVo;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import org.joda.time.DateTime;
import org.springframework.beans.BeanUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.util.CollectionUtils;

import java.util.*;

/**
 * 订单表 服务实现类
 */
@Service
public class OrderInfoServiceImpl extends ServiceImpl<OrderInfoMapper, OrderInfo> implements OrderInfoService {

    @Autowired
    private PatientFeignClient patientFeignClient;

    @Autowired
    private HospitalFeignClient hospitalFeignClient;

    @Autowired
    private RabbitService rabbitService;

    @Autowired
    private OrderInfoMapper orderInfoMapper;

    @Override
    public Map<String, Object> selectOrderCount(OrderCountQueryVo orderCountQueryVo) {
        List<OrderCountVo> orderCountVoList = orderInfoMapper.selectOrderCount(orderCountQueryVo);
        if(CollectionUtils.isEmpty(orderCountVoList)){
            throw new YyghException(20001,"数据为空");
        }


        Map<String, Object> result = new HashMap<>();

        List<String> dateList = orderCountVoList.stream().map(OrderCountVo::getReserveDate).toList();
        List<Integer> countList = orderCountVoList.stream().map(OrderCountVo::getCount).toList();

        //key名称与前端页面取值名称一致。
        result.put("dateList", dateList);
        result.put("countList", countList);

        return result;
    }

    @Override
    public void patientTips(String dateString) {
        LambdaQueryWrapper<OrderInfo> queryWrapper = new LambdaQueryWrapper<>();
        queryWrapper.eq(OrderInfo::getReserveDate, dateString);
        queryWrapper.ne(OrderInfo::getOrderStatus, -1);
        List<OrderInfo> orderInfoList = orderInfoMapper.selectList(queryWrapper);
        if (!CollectionUtils.isEmpty(orderInfoList)) {
            for (OrderInfo orderInfo : orderInfoList) {
                String patientPhone = orderInfo.getPatientPhone();
                MsmVo msmVo = new MsmVo();
                msmVo.setPhone(patientPhone);
                //msmVo.setTemplateCode("");
                rabbitService.sendMessage(MqConst.EXCHANGE_DIRECT_MSM, MqConst.ROUTING_MSM_ITEM, msmVo);
            }
        }
    }

    @Override
    public Boolean cancelOrder(Long orderId) {
        //1.判断时间是否超过最晚取消订单时间
        OrderInfo orderInfo = orderInfoMapper.selectById(orderId);
        Date quitTime = orderInfo.getQuitTime();
        DateTime quitDateTime = new DateTime(quitTime);
        if (quitDateTime.isBeforeNow()) {
            throw new YyghException(20001, "超过退号最晚时间,不能退号!");
        }

        //2.远程调用医院取消订单接口  修改订单状态为-1
        Map<String, Object> paramMap = new HashMap<>(); //给远程接口准备参数
        paramMap.put("hoscode", orderInfo.getHoscode());
        paramMap.put("hosRecordId", orderInfo.getHosRecordId());
        paramMap.put("sign", "");
        JSONObject result = HttpRequestHelper.sendRequest(paramMap, "http://localhost:9998/order/updateCancelStatus");

        if (result.getInteger("code") == 200) {
            //3.医院取消成功

            //3.1 修改自己订单状态 -1
            orderInfo.setOrderStatus(OrderStatusEnum.CANCLE.getStatus()); // -1
            orderInfoMapper.updateById(orderInfo);

            //3.2 修改 排班 号源数量    MQ异步完成
            //3.3 给用户发送取消预约成功短信   MQ异步完成
            //准备消息数据，并发送消息
            OrderMqVo orderMqVo = new OrderMqVo();
            orderMqVo.setScheduleId(orderInfo.getScheduleId());
            MsmVo msmVo = new MsmVo();
            msmVo.setPhone(orderInfo.getPatientPhone());
            orderMqVo.setMsmVo(msmVo);
            //msmVo.setTemplateCode("xxx");
            //发送预约挂号消息和取消预约挂号消息，用同一个交换机和队列。消费者要通过参数进行判断到底是挂号还是取消挂号。
            //判断条件：传递可预约数量及剩余数量就执行挂号数据更新；否则就做取消挂号数据更新；
            rabbitService.sendMessage(MqConst.EXCHANGE_DIRECT_ORDER, MqConst.ROUTING_ORDER, orderMqVo);
            return true;
        } else {
            //3.医院取消失败
            throw new YyghException(20001, "取消预约失败");
        }

    }

    @Override
    public OrderInfo getOrderInfo(Long orderId) {
        OrderInfo orderInfo = orderInfoMapper.selectById(orderId);
        orderInfo.getParam().put("orderStatusString",
                OrderStatusEnum.getStatusNameByStatus(orderInfo.getOrderStatus()));
        return orderInfo;
    }

    @Override
    public Long createOrder(String scheduleId, Long patientId) {

        //1.远程调用用户微服务：根据就诊人id获取就诊人信息Patient
        Patient patient = patientFeignClient.getPatientInfo(patientId); //就诊人数据

        //2.远程调用医院微服务：根据排班id获取ScheduleOrderVo
        ScheduleOrderVo scheduleOrderVo = hospitalFeignClient.getScheduleOrderVo(scheduleId);

        //3.封装请求参数，远程调用医院挂号接口，完成下单

        //3.1封装参数
        Map<String, Object> paramMap = new HashMap<>();
        paramMap.put("hoscode", scheduleOrderVo.getHoscode()); //医院编码
        paramMap.put("depcode", scheduleOrderVo.getDepcode()); //科室编码
        paramMap.put("hosScheduleId", scheduleOrderVo.getHosScheduleId()); //医院提供排班id
        paramMap.put("reserveDate", new DateTime(scheduleOrderVo.getReserveDate()).toString("yyyy-MM-dd")); //预约挂号日期
        paramMap.put("reserveTime", scheduleOrderVo.getReserveTime()); //预约挂号时间   0-上午  1-下午
        paramMap.put("amount", scheduleOrderVo.getAmount()); //挂号费用

        //就诊人信息
        paramMap.put("name", patient.getName()); //就诊人名称
        paramMap.put("certificatesType", patient.getCertificatesType()); //证件类型   身份证 10 、户口本 20
        paramMap.put("certificatesNo", patient.getCertificatesNo());  //实名认证的证件号
        paramMap.put("sex", patient.getSex()); //性别
        paramMap.put("birthdate", patient.getBirthdate()); //生日
        paramMap.put("phone", patient.getPhone()); //手机号
        paramMap.put("isMarry", patient.getIsMarry()); //是否结婚
        paramMap.put("provinceCode", patient.getProvinceCode()); //省编码
        paramMap.put("cityCode", patient.getCityCode()); //市编码
        paramMap.put("districtCode", patient.getDistrictCode()); //区编码
        paramMap.put("address", patient.getAddress()); //联系地址

        //就诊人对应的联系人
        paramMap.put("contactsName", patient.getContactsName()); //联系人名称
        paramMap.put("contactsCertificatesType", patient.getContactsCertificatesType()); //联系人的证件类型   身份证 10 、户口本 20
        paramMap.put("contactsCertificatesNo", patient.getContactsCertificatesNo());  //实名认证的证件号
        paramMap.put("contactsPhone", patient.getContactsPhone()); //联系人手机号
        paramMap.put("timestamp", HttpRequestHelper.getTimestamp()); //下单时间
        //String sign = HttpRequestHelper.getSign(paramMap, signInfoVo.getSignKey());
        paramMap.put("sign", ""); //接口权限校验签名 ：  相当与于秘钥

        //3.2 远程接口调用
        JSONObject result = HttpRequestHelper.sendRequest(paramMap, "http://localhost:9998/order/submitOrder");
        //4.判断医院挂号是否成功
        if (result.getInteger("code") == 200) {
            //4.1 挂号成功
            JSONObject jsonObject = result.getJSONObject("data"); //resultMap
            //5.保存订单
            OrderInfo orderInfo = new OrderInfo();
            BeanUtils.copyProperties(scheduleOrderVo, orderInfo);

            //这个值可以从请求JWT令牌中获取。
            Long userId = 26L;  //登录系统用户ID   例如：张老三
            orderInfo.setUserId(userId);

            //生成订单流水号   [0,100)
            String outTradeNo = System.currentTimeMillis() + "" + new Random().nextInt(100);
            orderInfo.setOutTradeNo(outTradeNo);
            orderInfo.setScheduleId(scheduleId); //存储mongodb主键值
            //orderInfo.setScheduleId(scheduleOrderVo.getHosScheduleId()); //医院方内部排班序号值
            orderInfo.setPatientId(patient.getId());
            orderInfo.setPatientName(patient.getName());
            orderInfo.setPatientPhone(patient.getPhone());
            orderInfo.setHosRecordId(jsonObject.getInteger("hosRecordId").toString());
            orderInfo.setNumber(jsonObject.getInteger("number"));
            orderInfo.setFetchTime(jsonObject.getString("fetchTime"));
            orderInfo.setFetchAddress(jsonObject.getString("fetchAddress"));
            orderInfo.setOrderStatus(0); // // 0 下单未支付    1 已支付   2 取号   -1 取消挂号      咱们自己规定
            orderInfoMapper.insert(orderInfo); //主键回填


            //系统优化：6和7步骤，需要进行异步操作，大大提高系统响应速度。借助MQ

            //发送mq信息更新号源和短信通知
            OrderMqVo orderMqVo = new OrderMqVo();
            orderMqVo.setScheduleId(scheduleId);
            orderMqVo.setReservedNumber(jsonObject.getInteger("reservedNumber"));
            orderMqVo.setAvailableNumber(jsonObject.getInteger("availableNumber"));

            //短信提示
            MsmVo msmVo = new MsmVo();
            msmVo.setPhone(orderInfo.getPatientPhone());
            String reserveDate =
                    new DateTime(orderInfo.getReserveDate()).toString("yyyy-MM-dd")
                            + (orderInfo.getReserveTime() == 0 ? "上午" : "下午");
            Map<String, Object> param = new HashMap<String, Object>() {{
                put("title", orderInfo.getHosname() + "|" + orderInfo.getDepname() + "|" + orderInfo.getTitle());
                put("amount", orderInfo.getAmount());
                put("reserveDate", reserveDate);
                put("name", orderInfo.getPatientName());
                put("quitTime", new DateTime(orderInfo.getQuitTime()).toString("yyyy-MM-dd HH:mm"));
            }}; // 匿名内部类动态代码块。

            msmVo.setParam(param); //msmVo对象的param属性是HashMap集合
            orderMqVo.setMsmVo(msmVo);

            //6.修改排班可预约数量 mongo
            //7.发送短信信息提醒

            rabbitService.sendMessage(MqConst.EXCHANGE_DIRECT_ORDER, MqConst.ROUTING_ORDER, orderMqVo); // 6和7步骤由消费者端来异步处理。
            return orderInfo.getId();
        } else {
            //4.2 挂号失败
            //throw new YyghException(20001, result.getString("msg"));
            throw new YyghException(20001, "挂号失败");
        }
    }
}
