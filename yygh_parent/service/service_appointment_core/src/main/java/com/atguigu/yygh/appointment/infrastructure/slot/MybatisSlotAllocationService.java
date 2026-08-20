package com.atguigu.yygh.appointment.infrastructure.slot;

import com.atguigu.yygh.appointment.common.exception.AppointmentBizException;
import com.atguigu.yygh.appointment.domain.slot.AllocatedSlot;
import com.atguigu.yygh.appointment.domain.slot.SlotAllocationService;
import com.atguigu.yygh.appointment.domain.slot.model.ApSlot;
import com.atguigu.yygh.appointment.infrastructure.mapper.ApScheduleMapper;
import com.atguigu.yygh.appointment.infrastructure.mapper.ApSlotMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class MybatisSlotAllocationService implements SlotAllocationService {

    private final ApSlotMapper apSlotMapper;
    private final ApScheduleMapper apScheduleMapper;

    @Override
    public AllocatedSlot allocate(String scheduleId, String patientId, String holdId) {
        ApSlot slot = apSlotMapper.selectFirstAvailableForUpdate(scheduleId);
        if (slot == null) {
            throw new AppointmentBizException("当前排班已无可用号源");
        }

        int updatedSlot = apSlotMapper.markHeld(slot.getSlotId(), holdId, patientId);
        if (updatedSlot == 0) {
            throw new AppointmentBizException("号源占位失败，请稍后重试");
        }

        int updatedSchedule = apScheduleMapper.occupyForHold(scheduleId);
        if (updatedSchedule == 0) {
            throw new AppointmentBizException("排班状态异常或库存不足");
        }

        return new AllocatedSlot(slot.getSlotId(), slot.getSequenceNo());
    }
}
