package com.atguigu.yygh.order.client.fallback;

import com.atguigu.yygh.common.exception.YyghException;
import com.atguigu.yygh.order.client.OrderInfoFeignClient;
import com.atguigu.yygh.vo.order.OrderCountQueryVo;
import org.springframework.stereotype.Component;

import java.util.Map;

@Component
public class OrderDegradeFeignClient implements OrderInfoFeignClient {

    //当前远程调用出现问题，由该方法进行降级处理：返回兜底数据。
    @Override
    public Map<String, Object> getOrderCount(OrderCountQueryVo orderCountQueryVo) {
        throw new YyghException(20001, "远程调用预约统计失败!");
    }
}
