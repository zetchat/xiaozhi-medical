package com.atguigu.yygh.appointment.controller;

import com.atguigu.yygh.appointment.api.response.OrderDetailResponse;
import com.atguigu.yygh.appointment.application.OrderCancelAppService;
import com.atguigu.yygh.appointment.application.OrderQueryAppService;
import com.atguigu.yygh.appointment.common.api.ApiResponse;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v2/orders")
public class OrderController {

    private final OrderQueryAppService orderQueryAppService;
    private final OrderCancelAppService orderCancelAppService;

    public OrderController(OrderQueryAppService orderQueryAppService,
                           OrderCancelAppService orderCancelAppService) {
        this.orderQueryAppService = orderQueryAppService;
        this.orderCancelAppService = orderCancelAppService;
    }

    @GetMapping("/{orderId}")
    public ApiResponse<OrderDetailResponse> detail(@PathVariable String orderId) {
        return ApiResponse.ok(orderQueryAppService.getOrderDetail(orderId));
    }

    @PostMapping("/{orderId}/cancel")
    public ApiResponse<OrderDetailResponse> cancel(@PathVariable String orderId) {
        return ApiResponse.ok(orderCancelAppService.cancelOrder(orderId));
    }
}
