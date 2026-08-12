package com.atguigu.yygh.common.trace;

import feign.RequestInterceptor;
import feign.RequestTemplate;
import org.springframework.stereotype.Component;

/**
 * 透传 traceId 到下游微服务，串联跨服务日志。
 */
@Component
public class TraceFeignInterceptor implements RequestInterceptor {

    @Override
    public void apply(RequestTemplate template) {
        template.header(TraceContext.TRACE_HEADER, TraceContext.getOrCreateTraceId());
    }
}
