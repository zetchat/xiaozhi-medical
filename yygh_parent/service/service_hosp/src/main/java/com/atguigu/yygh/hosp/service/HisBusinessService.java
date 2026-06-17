package com.atguigu.yygh.hosp.service;

import com.atguigu.yygh.client_dto.HisLockResponse;
import com.atguigu.yygh.common.result.Result;

public interface HisBusinessService {

    /**
     * 核心接口：锁定号源
     * @param patientId 就诊人ID
     * @param scheduleId 排班ID
     * @return 锁定结果，包含hisSeqNo
     */
    HisLockResponse lockTicket(String patientId, String scheduleId);

    /**
     * 核心接口：解锁号源 (供取消订单或死信队列超时调用)
     * @param hisSeqNo 锁号流水号
     * @return 解锁结果
     */
    Result unlockTicket(String hisSeqNo);
}
