package com.atguigu.yygh.appointment.common.exception;

import com.atguigu.yygh.appointment.common.api.ApiResponse;
import com.atguigu.yygh.common.trace.TraceContext;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

@RestControllerAdvice
@Slf4j
public class GlobalExceptionHandler {

    @ExceptionHandler(AppointmentBizException.class)
    public ApiResponse<Void> handleBizException(AppointmentBizException ex) {
        log.warn("预约核心域业务异常, traceId: {}, message: {}", TraceContext.getOrCreateTraceId(), ex.getMessage());
        return ApiResponse.fail(ex.getMessage());
    }

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ApiResponse<Void> handleValidationException(MethodArgumentNotValidException ex) {
        String message = ex.getBindingResult().getFieldErrors().stream()
                .findFirst()
                .map(fieldError -> fieldError.getField() + ":" + fieldError.getDefaultMessage())
                .orElse("请求参数不合法");
        log.warn("预约核心域参数校验异常, traceId: {}, message: {}", TraceContext.getOrCreateTraceId(), message);
        return ApiResponse.fail(message);
    }

    @ExceptionHandler(Exception.class)
    public ApiResponse<Void> handleUnexpectedException(Exception ex) {
        log.error("预约核心域发生未处理异常, traceId: {}", TraceContext.getOrCreateTraceId(), ex);
        return ApiResponse.fail("系统繁忙，请稍后再试");
    }
}
