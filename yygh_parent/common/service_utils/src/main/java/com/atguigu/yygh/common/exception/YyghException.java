package com.atguigu.yygh.common.exception;

import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
public class YyghException extends RuntimeException {

    private Integer code;  //异常状态码

    private String msg;  //异常描述

    public YyghException(Integer code, String msg) {
        super(msg);
        this.code = code;
        this.msg = msg;
    }

    public YyghException(Integer code, String msg, Throwable cause) {
        super(msg, cause);
        this.code = code;
        this.msg = msg;
    }
}
