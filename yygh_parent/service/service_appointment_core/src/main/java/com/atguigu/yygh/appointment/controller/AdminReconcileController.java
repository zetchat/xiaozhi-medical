package com.atguigu.yygh.appointment.controller;

import com.atguigu.yygh.appointment.api.response.ReconcileTaskResponse;
import com.atguigu.yygh.appointment.application.ReconcileQueryAppService;
import com.atguigu.yygh.appointment.common.api.ApiResponse;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/v2/admin/reconcile")
public class AdminReconcileController {

    private final ReconcileQueryAppService reconcileQueryAppService;

    public AdminReconcileController(ReconcileQueryAppService reconcileQueryAppService) {
        this.reconcileQueryAppService = reconcileQueryAppService;
    }

    @GetMapping("/tasks")
    public ApiResponse<List<ReconcileTaskResponse>> listTasks(@RequestParam(defaultValue = "20") int limit) {
        return ApiResponse.ok(reconcileQueryAppService.listRecentTasks(limit));
    }
}
