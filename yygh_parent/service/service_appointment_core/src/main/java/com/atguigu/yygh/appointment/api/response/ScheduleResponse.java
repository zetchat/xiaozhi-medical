package com.atguigu.yygh.appointment.api.response;

import lombok.AllArgsConstructor;
import lombok.Data;

import java.time.LocalDate;
import java.time.LocalDateTime;

@Data
@AllArgsConstructor
public class ScheduleResponse {

    private String scheduleId;
    private String status;
    private Integer totalCount;
    private Integer availableCount;
    private Integer heldCount;
    private Integer confirmedCount;
    private LocalDate visitDate;
    private Integer timePeriod;
    private LocalDateTime openTime;
    private LocalDateTime closeTime;
}
