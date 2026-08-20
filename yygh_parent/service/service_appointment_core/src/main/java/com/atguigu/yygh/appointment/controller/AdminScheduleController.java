package com.atguigu.yygh.appointment.controller;

import com.atguigu.yygh.appointment.api.command.CreateScheduleCommand;
import com.atguigu.yygh.appointment.api.response.ScheduleResponse;
import com.atguigu.yygh.appointment.application.ScheduleAdminAppService;
import com.atguigu.yygh.appointment.common.api.ApiResponse;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v2/admin/schedules")
public class AdminScheduleController {

    private final ScheduleAdminAppService scheduleAdminAppService;

    public AdminScheduleController(ScheduleAdminAppService scheduleAdminAppService) {
        this.scheduleAdminAppService = scheduleAdminAppService;
    }

    @PostMapping
    public ApiResponse<ScheduleResponse> create(@Valid @RequestBody CreateScheduleCommand command) {
        return ApiResponse.ok(scheduleAdminAppService.createSchedule(command));
    }

    @PostMapping("/{scheduleId}/slots/generate")
    public ApiResponse<ScheduleResponse> generateSlots(@PathVariable String scheduleId) {
        return ApiResponse.ok(scheduleAdminAppService.generateSlots(scheduleId));
    }

    @PostMapping("/{scheduleId}/publish")
    public ApiResponse<ScheduleResponse> publish(@PathVariable String scheduleId) {
        return ApiResponse.ok(scheduleAdminAppService.publishSchedule(scheduleId));
    }

    @PostMapping("/{scheduleId}/suspend")
    public ApiResponse<ScheduleResponse> suspend(@PathVariable String scheduleId) {
        return ApiResponse.ok(scheduleAdminAppService.suspendSchedule(scheduleId));
    }

    @PostMapping("/{scheduleId}/resume")
    public ApiResponse<ScheduleResponse> resume(@PathVariable String scheduleId) {
        return ApiResponse.ok(scheduleAdminAppService.resumeSchedule(scheduleId));
    }

    @PostMapping("/{scheduleId}/cancel")
    public ApiResponse<ScheduleResponse> cancel(@PathVariable String scheduleId) {
        return ApiResponse.ok(scheduleAdminAppService.cancelSchedule(scheduleId));
    }

    @PostMapping("/test-init")
    public ApiResponse<ScheduleResponse> testInit(@Valid @RequestBody CreateScheduleCommand command) {
        return ApiResponse.ok(scheduleAdminAppService.initTestSchedule(command));
    }
}
