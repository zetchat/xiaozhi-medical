package com.atguigu.yygh.hosp.service.impl;

import com.atguigu.yygh.client_dto.HisLockResponse;
import com.atguigu.yygh.common.result.Result;
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
        // 1. 幂等性校验：检查该患者是否已经在该排班下锁过号了
        // 如果网络抖动导致前台发起重试，这里直接返回之前的流水号，不会重复扣减
        HisLockRecord existRecord = hisLockRecordMapper.findByScheduleAndPatient(scheduleId, patientId);
        if (existRecord != null && "LOCKED".equals(existRecord.getStatus())) {
            log.info("触发锁号接口幂等，直接返回已存在的凭证: {}", existRecord.getHisSeqNo());
            return HisLockResponse.success(existRecord.getHisSeqNo());
        }

        // 2. 寻找一个可用的具体号源 (如 3号)
        HisScheduleDetail availableDetail = hisScheduleDetailMapper.findAvailableDetail(scheduleId);
        if (availableDetail == null) {
            log.warn("HIS系统排班无可用号源, scheduleId: {}", scheduleId);
            return HisLockResponse.fail("医院系统号源不足");
        }

        // 2.1 清理该号源上已释放的历史流水，避免 detail_id 唯一索引阻塞再次锁号
        hisLockRecordMapper.deleteReleasedByDetailId(availableDetail.getDetailId());

        // 3. 乐观锁扣减该号源 (状态 AVAILABLE -> LOCKED)
        int updated = hisScheduleDetailMapper.lockScheduleDetail(availableDetail.getDetailId());
        if (updated == 0) {
            // 乐观锁冲突，说明这极短的时间内这个号被别人抢了
            log.warn("HIS系统并发锁号冲突, detailId: {}", availableDetail.getDetailId());
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
            // 捕获唯一索引异常 (uk_detail_id)，防极端并发下同一个号被锁两次
            log.error("插入HIS锁号流水异常，可能触发了唯一索引限制", e);
            throw new RuntimeException("生成锁号凭证失败");
        }

        log.info("HIS系统锁号成功，排班: {}, 就诊人: {}, 流水号: {}", scheduleId, patientId, hisSeqNo);
        return HisLockResponse.success(hisSeqNo);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public Result unlockTicket(String hisSeqNo) {
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
        
        log.info("HIS系统解锁号源成功，流水号: {}", hisSeqNo);
        return Result.ok();
    }
}
