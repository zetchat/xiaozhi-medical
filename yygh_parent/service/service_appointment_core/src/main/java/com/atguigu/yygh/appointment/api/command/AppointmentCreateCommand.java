package com.atguigu.yygh.appointment.api.command;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;

@Data
public class AppointmentCreateCommand {

    @NotBlank
    private String requestNo;

    @NotBlank
    private String patientId;

    @NotBlank
    private String scheduleId;

    private String source;
}
