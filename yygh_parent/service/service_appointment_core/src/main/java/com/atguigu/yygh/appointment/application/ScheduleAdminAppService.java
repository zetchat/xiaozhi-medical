package com.atguigu.yygh.appointment.application;

import com.atguigu.yygh.appointment.api.command.CreateScheduleCommand;
import com.atguigu.yygh.appointment.api.response.ScheduleResponse;

public interface ScheduleAdminAppService {

    ScheduleResponse createSchedule(CreateScheduleCommand command);

    ScheduleResponse generateSlots(String scheduleId);

    ScheduleResponse publishSchedule(String scheduleId);

    ScheduleResponse suspendSchedule(String scheduleId);

    ScheduleResponse resumeSchedule(String scheduleId);

    ScheduleResponse cancelSchedule(String scheduleId);

    ScheduleResponse initTestSchedule(CreateScheduleCommand command);
}
