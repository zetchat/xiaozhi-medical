package com.atguigu.yygh.appointment.domain.token;

public interface TokenGateService {

    void initScheduleToken(String scheduleId, int stock);

    boolean tryAcquireScheduleToken(String scheduleId);

    Long releaseScheduleToken(String scheduleId);
}
