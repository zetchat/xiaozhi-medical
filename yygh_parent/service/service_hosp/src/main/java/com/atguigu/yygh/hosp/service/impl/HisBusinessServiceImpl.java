package com.atguigu.yygh.hosp.service.impl;

import com.atguigu.yygh.client_dto.HisLockResponse;
import com.atguigu.yygh.common.result.Result;
import com.atguigu.yygh.common.trace.TraceContext;
import com.atguigu.yygh.hosp.mapper.HisLockRecordMapper;
import com.atguigu.yygh.hosp.mapper.HisScheduleDetailMapper;
import com.atguigu.yygh.hosp.service.HisBusinessService;
import com.atguigu.yygh.model.hosp.HisLockRecord;
import com.atguigu.yygh.model.hosp.HisScheduleDetail;
import com.baomidou.mybatisplus.core.toolkit.IdWorker;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Slf4j
public class HisBusinessServiceImpl implements HisBusinessService {

    @Autowired
    private HisScheduleDetailMapper hisScheduleDetailMapper;

    @Autowired
    private HisLockRecordMapper hisLockRecordMapper;

    @Override
    @Transactional(rollbackFor = Exception.class)
    public HisLockResponse lockTicket(String patientId, String scheduleId) {
        log.info("收到HIS锁号请求, traceId: {}, patientId: {}, scheduleId: {}",
                TraceContext.getOrCreateTraceId(), patientId, scheduleId);

        // 1. 幂等性校验：同一患者在同一排班下若存在活跃锁号记录，则直接复用
        HisLockRecord existRecord = hisLockRecordMapper.findActiveByScheduleAndPatient(scheduleId, patientId);
        if (existRecord != null) {
            log.info("触发HIS锁号幂等复用, patientId: {}, scheduleId: {}, hisSeqNo: {}",
                    patientId, scheduleId, existRecord.getHisSeqNo());
            return HisLockResponse.success(existRecord.getHisSeqNo());
        }

        // 2. 寻找一个可用的具体号源 (如 3号)
        HisScheduleDetail availableDetail = hisScheduleDetailMapper.findAvailableDetail(scheduleId);
        if (availableDetail == null) {
            log.warn("HIS系统排班无可用号源, patientId: {}, scheduleId: {}", patientId, scheduleId);
            return HisLockResponse.fail("医院系统号源不足");
        }
        log.info("HIS找到可用号源明细, patientId: {}, scheduleId: {}, detailId: {}",
                patientId, scheduleId, availableDetail.getDetailId());

        // 3. 乐观锁扣减该号源 (状态 AVAILABLE -> LOCKED)，由状态表保证当前资源唯一占用
        int updated = hisScheduleDetailMapper.lockScheduleDetail(availableDetail.getDetailId());
        if (updated == 0) {
            // 乐观锁冲突，说明这极短的时间内这个号被别人抢了
            log.warn("HIS系统并发锁号冲突, patientId: {}, scheduleId: {}, detailId: {}",
                    patientId, scheduleId, availableDetail.getDetailId());
            return HisLockResponse.fail("当前就诊序号已被抢占，请重试");
        }

        // 4. 插入锁号流水表，生成锁号凭证
        String hisSeqNo = "HIS_" + IdWorker.getIdStr(); // 模拟生成复杂的HIS流水号
        HisLockRecord lockRecord = new HisLockRecord();
        lockRecord.setHisSeqNo(hisSeqNo);
        lockRecord.setScheduleId(scheduleId);
        lockRecord.setDetailId(availableDetail.getDetailId());
        lockRecord.setPatientId(patientId);
        lockRecord.setStatus("LOCKED");
        
        try {
            hisLockRecordMapper.insert(lockRecord);
        } catch (Exception e) {
            // 状态表已抢占成功，但流水落库失败时需要整体回滚事务
            log.error("插入HIS锁号流水异常，事务即将回滚, patientId: {}, scheduleId: {}, detailId: {}, hisSeqNo: {}",
                    patientId, scheduleId, availableDetail.getDetailId(), hisSeqNo, e);
            throw new RuntimeException("生成锁号凭证失败");
        }

        log.info("HIS系统锁号成功, patientId: {}, scheduleId: {}, detailId: {}, hisSeqNo: {}",
                patientId, scheduleId, availableDetail.getDetailId(), hisSeqNo);
        return HisLockResponse.success(hisSeqNo);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public Result unlockTicket(String hisSeqNo) {
        log.info("收到HIS解锁请求, traceId: {}, hisSeqNo: {}",
                TraceContext.getOrCreateTraceId(), hisSeqNo);

        // 1. 查询锁号记录
        HisLockRecord lockRecord = hisLockRecordMapper.selectById(hisSeqNo);
        if (lockRecord == null) {
            log.warn("HIS解锁失败，未找到该锁号流水: {}", hisSeqNo);
            return Result.fail().message("无效的HIS流水号");
        }

        // 2. 幂等与状态机校验
        if ("RELEASED".equals(lockRecord.getStatus())) {
            log.info("HIS解锁接口触发幂等，该号源已释放: {}", hisSeqNo);
            return Result.ok();
        }
        if ("CONFIRMED".equals(lockRecord.getStatus())) {
            log.error("HIS解锁失败，该号源已支付确认，不允许回滚: {}", hisSeqNo);
            return Result.fail().message("该号源已确认支付，无法取消");
        }

        // 3. 更新号源明细状态 (LOCKED -> AVAILABLE)
        int unlocked = hisScheduleDetailMapper.unlockScheduleDetail(lockRecord.getDetailId());
        if (unlocked == 0) {
            log.error("HIS解锁号源明细失败，状态异常. detailId: {}", lockRecord.getDetailId());
            throw new RuntimeException("号源明细状态异常");
        }

        // 4. 更新流水表状态
        hisLockRecordMapper.updateStatus(hisSeqNo, "RELEASED");
        
        log.info("HIS系统解锁号源成功, hisSeqNo: {}, scheduleId: {}, detailId: {}, patientId: {}",
                hisSeqNo, lockRecord.getScheduleId(), lockRecord.getDetailId(), lockRecord.getPatientId());
        return Result.ok();
    }
}
