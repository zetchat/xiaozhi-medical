package com.atguigu.yygh.order.client;

import com.atguigu.yygh.client_dto.HisLockResponse;
import com.atguigu.yygh.common.result.Result;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;

@FeignClient(contextId = "hisRpcClient", value = "service-hosp")
public interface HisRpcClient {

    /**
     * 同步调用 HIS 接口锁定真实号源
     */
    @PostMapping("/api/hosp/hospital/inner/lockTicket")
    HisLockResponse lockTicket(@RequestParam("patientId") String patientId, @RequestParam("scheduleId") String scheduleId);

    /**
     * 调用 HIS 接口解锁号源
     */
    @PostMapping("/api/hosp/hospital/inner/unlockTicket")
    Result unlockTicket(@RequestParam("hisSeqNo") String hisSeqNo);
}
