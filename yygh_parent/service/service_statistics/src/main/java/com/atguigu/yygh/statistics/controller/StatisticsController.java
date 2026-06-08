package com.atguigu.yygh.statistics.controller;

import com.atguigu.yygh.common.result.R;
import com.atguigu.yygh.order.client.OrderInfoFeignClient;
import com.atguigu.yygh.vo.order.OrderCountQueryVo;
import com.atguigu.yygh.vo.order.OrderQueryVo;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;
import java.util.Map;

@RestController
@RequestMapping("/admin/statistics")
public class StatisticsController {

    @Autowired
    OrderInfoFeignClient orderInfoFeignClient;

    //预约统计
    @GetMapping("getCountMap")
    public R getCountMap(OrderCountQueryVo orderCountQueryVo) { //前端以路径形式传递参数。不用加@RequestBody
        Map<String, Object> orderCountMap = orderInfoFeignClient.getOrderCount(orderCountQueryVo);
        return R.ok().data(orderCountMap);
    }
}
