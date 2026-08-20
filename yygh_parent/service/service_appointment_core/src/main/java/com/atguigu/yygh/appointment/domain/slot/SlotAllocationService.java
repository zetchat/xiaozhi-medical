package com.atguigu.yygh.appointment.domain.slot;

public interface SlotAllocationService {

    AllocatedSlot allocate(String scheduleId, String patientId, String holdId);
}
