package com.atguigu.yygh.order.client;

import com.atguigu.yygh.order.client.fallback.OrderDegradeFeignClient;
import com.atguigu.yygh.vo.order.OrderCountQueryVo;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.cloud.openfeign.SpringQueryMap;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;

import java.util.Map;

@FeignClient(name = "service-orders" , fallback = OrderDegradeFeignClient.class)
public interface OrderInfoFeignClient {


    @PostMapping("/api/order/orderInfo/inner/getOrderCount")
    public Map<String, Object> getOrderCount(@RequestBody OrderCountQueryVo orderCountQueryVo); //get请求调用@SpringQueryMap
}
