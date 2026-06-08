package com.atguigu.yygh.msm.service;

import com.atguigu.yygh.vo.msm.MsmVo;

import java.io.IOException;

public interface MsmService {

    boolean send(MsmVo msmVo) throws IOException;
}
