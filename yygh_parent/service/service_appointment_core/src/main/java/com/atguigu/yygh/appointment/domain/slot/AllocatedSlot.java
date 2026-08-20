package com.atguigu.yygh.appointment.domain.slot;

import lombok.AllArgsConstructor;
import lombok.Data;

@Data
@AllArgsConstructor
public class AllocatedSlot {

    private String slotId;
    private Integer sequenceNo;
}
