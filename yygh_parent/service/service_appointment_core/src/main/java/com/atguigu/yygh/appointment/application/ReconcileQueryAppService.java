package com.atguigu.yygh.appointment.application;

import com.atguigu.yygh.appointment.api.response.ReconcileTaskResponse;

import java.util.List;

public interface ReconcileQueryAppService {

    List<ReconcileTaskResponse> listRecentTasks(int limit);
}
