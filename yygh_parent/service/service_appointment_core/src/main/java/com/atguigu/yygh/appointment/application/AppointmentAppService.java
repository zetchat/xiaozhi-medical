package com.atguigu.yygh.appointment.application;

import com.atguigu.yygh.appointment.api.command.AppointmentCreateCommand;
import com.atguigu.yygh.appointment.api.response.AppointmentCreateResponse;

public interface AppointmentAppService {

    AppointmentCreateResponse createAppointment(AppointmentCreateCommand command);
}
