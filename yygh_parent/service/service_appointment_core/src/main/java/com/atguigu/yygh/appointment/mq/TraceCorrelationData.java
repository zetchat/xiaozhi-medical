package com.atguigu.yygh.appointment.mq;

import org.springframework.amqp.rabbit.connection.CorrelationData;

public class TraceCorrelationData extends CorrelationData {

    private final String traceId;

    public TraceCorrelationData(String id, String traceId) {
        super(id);
        this.traceId = traceId;
    }

    public String getTraceId() {
        return traceId;
    }
}
