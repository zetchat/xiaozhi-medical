package com.atguigu.yygh.client_dto;

import lombok.Data;

@Data
public class HisLockResponse {
    private boolean success;
    private String msg;
    private String hisSeqNo;

    public static HisLockResponse success(String hisSeqNo) {
        HisLockResponse response = new HisLockResponse();
        response.setSuccess(true);
        response.setHisSeqNo(hisSeqNo);
        return response;
    }

    public static HisLockResponse fail(String msg) {
        HisLockResponse response = new HisLockResponse();
        response.setSuccess(false);
        response.setMsg(msg);
        return response;
    }
}
