package com.atguigu.yygh.appointment.application.impl;

import com.atguigu.yygh.appointment.api.response.ReconcileTaskResponse;
import com.atguigu.yygh.appointment.application.ReconcileQueryAppService;
import com.atguigu.yygh.appointment.domain.reconcile.model.ApReconcileTask;
import com.atguigu.yygh.appointment.infrastructure.mapper.ApReconcileTaskMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
@RequiredArgsConstructor
public class ReconcileQueryAppServiceImpl implements ReconcileQueryAppService {

    private final ApReconcileTaskMapper apReconcileTaskMapper;

    @Override
    public List<ReconcileTaskResponse> listRecentTasks(int limit) {
        int safeLimit = Math.max(1, Math.min(limit, 100));
        return apReconcileTaskMapper.findRecentTasks(safeLimit).stream()
                .map(this::toResponse)
                .toList();
    }

    private ReconcileTaskResponse toResponse(ApReconcileTask task) {
        return new ReconcileTaskResponse(
                task.getTaskId(),
                task.getTaskType(),
                task.getBizKey(),
                task.getStatus(),
                task.getDetailJson(),
                task.getRetryCount(),
                task.getLastError(),
                task.getCreatedAt(),
                task.getUpdatedAt()
        );
    }
}
