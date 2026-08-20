package com.atguigu.yygh.appointment.controller;

import com.atguigu.yygh.appointment.api.command.AppointmentCreateCommand;
import com.atguigu.yygh.appointment.api.response.AppointmentCreateResponse;
import com.atguigu.yygh.appointment.application.AppointmentAppService;
import com.atguigu.yygh.appointment.common.api.ApiResponse;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v2/appointments")
public class AppointmentController {

    private final AppointmentAppService appointmentAppService;

    public AppointmentController(AppointmentAppService appointmentAppService) {
        this.appointmentAppService = appointmentAppService;
    }

    @PostMapping
    public ApiResponse<AppointmentCreateResponse> create(@Valid @RequestBody AppointmentCreateCommand command) {
        return ApiResponse.ok(appointmentAppService.createAppointment(command));
    }
}
