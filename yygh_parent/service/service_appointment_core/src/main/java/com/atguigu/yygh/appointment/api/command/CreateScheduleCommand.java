package com.atguigu.yygh.appointment.api.command;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

import java.time.LocalDate;
import java.time.LocalDateTime;

@Data
public class CreateScheduleCommand {

    @NotBlank
    private String doctorId;

    @NotBlank
    private String deptId;

    @NotBlank
    private String hospitalId;

    @NotNull
    private LocalDate visitDate;

    @NotNull
    private Integer timePeriod;

    @NotNull
    @Min(1)
    private Integer totalCount;

    @NotNull
    private Integer allowCancel;

    private LocalDateTime openTime;

    private LocalDateTime closeTime;
}
