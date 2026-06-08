package com.atguigu.yygh.msm.service.impl;

import com.atguigu.yygh.common.exception.YyghException;
import com.atguigu.yygh.msm.service.MsmService;
import com.atguigu.yygh.msm.utils.HttpUtils;
import com.atguigu.yygh.msm.utils.RandomUtil;
import com.atguigu.yygh.vo.msm.MsmVo;
import okhttp3.FormBody;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.Response;
import org.apache.http.HttpEntity;
import org.apache.http.HttpResponse;
import org.apache.http.util.EntityUtils;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import java.io.IOException;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;

@Service
public class MsmServiceImpl implements MsmService {

    //TODO 仅为了测试
    //发送短信实现
    @Override
    public boolean send(MsmVo msmVo) {
        System.out.println("短信发送成功...phone=" + msmVo.getPhone() + " code=1111");
        return true;
    }

/*
    //发送短信实现
    @Override
    public boolean send(MsmVo msmVo) throws IOException {
        if (!StringUtils.isEmpty(msmVo.getPhone())) {
            String code = RandomUtil.getFourBitRandom();
            return this.sendMessage(msmVo.getPhone(), code);
        }
        return false;
    }

    //发送短信方法
    private boolean sendMessage(String phone, String verifyCode) throws IOException {
        String url = "https://smssend.shumaidata.com/sms/send";
        String appCode = "97c8ccac3fd5436a8e7ab5bbcc7c7498";

        Map<String, String> params = new HashMap<>();
        params.put("receive", phone);
        params.put("tag", verifyCode);
        params.put("templateId", "templateId");

        String result = this.postForm(appCode, url, params);
        System.out.println(result);
        return true;
    }


    *//**
     * 用到的HTTP工具包：okhttp 3.13.1
     * <dependency>
     * <groupId>com.squareup.okhttp3</groupId>
     * <artifactId>okhttp</artifactId>
     * <version>3.13.1</version>
     * </dependency>
     *//*
    public  String postForm(String appCode, String url, Map<String, String> params) throws IOException {
        url = url + buildRequestUrl(params);
        OkHttpClient client = new OkHttpClient.Builder().build();
        FormBody.Builder formbuilder = new FormBody.Builder();
        Iterator<String> it = params.keySet().iterator();
        while (it.hasNext()) {
            String key = it.next();
            formbuilder.add(key, params.get(key));
        }
        FormBody body = formbuilder.build();
        Request request = new Request.Builder().url(url).addHeader("Authorization", "APPCODE " + appCode).post(body).build();
        Response response = client.newCall(request).execute();
        System.out.println("返回状态码" + response.code() + ",message:" + response.message());
        String result = response.body().string();
        return result;
    }

    public  String buildRequestUrl(Map<String, String> params) {
        StringBuilder url = new StringBuilder("?");
        Iterator<String> it = params.keySet().iterator();
        while (it.hasNext()) {
            String key = it.next();
            url.append(key).append("=").append(params.get(key)).append("&");
        }
        return url.toString().substring(0, url.length() - 1);
    }*/
}
