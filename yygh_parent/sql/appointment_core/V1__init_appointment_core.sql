CREATE DATABASE IF NOT EXISTS yygh_appointment_core
  DEFAULT CHARACTER SET utf8mb4
  DEFAULT COLLATE utf8mb4_0900_ai_ci;

USE yygh_appointment_core;

SET NAMES utf8mb4;
SET FOREIGN_KEY_CHECKS = 0;

DROP TABLE IF EXISTS ap_schedule;
CREATE TABLE ap_schedule (
    schedule_id          VARCHAR(32)  NOT NULL COMMENT '排班ID',
    doctor_id            VARCHAR(32)  NOT NULL COMMENT '医生ID',
    dept_id              VARCHAR(32)  NOT NULL COMMENT '科室ID',
    hospital_id          VARCHAR(32)  NOT NULL COMMENT '医院ID',
    visit_date           DATE         NOT NULL COMMENT '出诊日期',
    time_period          TINYINT      NOT NULL COMMENT '时间段:0上午 1下午 2晚上',
    total_count          INT          NOT NULL COMMENT '总号源数',
    available_count      INT          NOT NULL DEFAULT 0 COMMENT '可用号源数',
    held_count           INT          NOT NULL DEFAULT 0 COMMENT '预占号源数',
    confirmed_count      INT          NOT NULL DEFAULT 0 COMMENT '已确认号源数',
    status               VARCHAR(20)  NOT NULL COMMENT 'DRAFT/OPEN/SUSPENDED/CANCELLED/CLOSED',
    allow_cancel         TINYINT      NOT NULL DEFAULT 1 COMMENT '是否允许退号:0否 1是',
    open_time            DATETIME(3)  NULL COMMENT '放号时间',
    close_time           DATETIME(3)  NULL COMMENT '停止预约时间',
    version              INT          NOT NULL DEFAULT 0 COMMENT '乐观锁版本',
    created_at           DATETIME(3)  NOT NULL DEFAULT CURRENT_TIMESTAMP(3),
    updated_at           DATETIME(3)  NOT NULL DEFAULT CURRENT_TIMESTAMP(3) ON UPDATE CURRENT_TIMESTAMP(3),
    PRIMARY KEY (schedule_id),
    KEY idx_schedule_doctor_date (doctor_id, visit_date),
    KEY idx_schedule_dept_date (dept_id, visit_date),
    KEY idx_schedule_hospital_date (hospital_id, visit_date),
    KEY idx_schedule_status_date (status, visit_date)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='排班聚合表';

DROP TABLE IF EXISTS ap_slot;
CREATE TABLE ap_slot (
    slot_id              VARCHAR(32)  NOT NULL COMMENT '号位ID',
    schedule_id          VARCHAR(32)  NOT NULL COMMENT '排班ID',
    sequence_no          INT          NOT NULL COMMENT '就诊序号',
    status               VARCHAR(20)  NOT NULL COMMENT 'AVAILABLE/HELD/CONFIRMED/INVALID',
    hold_id              VARCHAR(32)  NULL COMMENT '当前占用的Hold ID',
    patient_id           VARCHAR(32)  NULL COMMENT '当前占用患者ID',
    locked_at            DATETIME(3)  NULL COMMENT '预占时间',
    confirmed_at         DATETIME(3)  NULL COMMENT '确认时间',
    released_at          DATETIME(3)  NULL COMMENT '释放时间',
    invalid_reason       VARCHAR(100) NULL COMMENT '作废原因',
    created_at           DATETIME(3)  NOT NULL DEFAULT CURRENT_TIMESTAMP(3),
    updated_at           DATETIME(3)  NOT NULL DEFAULT CURRENT_TIMESTAMP(3) ON UPDATE CURRENT_TIMESTAMP(3),
    PRIMARY KEY (slot_id),
    UNIQUE KEY uk_slot_schedule_seq (schedule_id, sequence_no),
    KEY idx_slot_schedule_status_seq (schedule_id, status, sequence_no),
    KEY idx_slot_hold_id (hold_id),
    KEY idx_slot_patient_id (patient_id),
    CONSTRAINT fk_slot_schedule FOREIGN KEY (schedule_id) REFERENCES ap_schedule(schedule_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='具体号位表';

DROP TABLE IF EXISTS ap_hold;
CREATE TABLE ap_hold (
    hold_id              VARCHAR(32)  NOT NULL COMMENT '预占单ID',
    request_no           VARCHAR(64)  NOT NULL COMMENT '业务幂等号',
    schedule_id          VARCHAR(32)  NOT NULL COMMENT '排班ID',
    slot_id              VARCHAR(32)  NOT NULL COMMENT '号位ID',
    patient_id           VARCHAR(32)  NOT NULL COMMENT '患者ID',
    status               VARCHAR(20)  NOT NULL COMMENT 'INIT/HELD/CONFIRMED/EXPIRED/RELEASED/FAILED',
    active_flag          TINYINT      NOT NULL DEFAULT 1 COMMENT '是否有效:1有效 0失效',
    expire_time          DATETIME(3)  NOT NULL COMMENT '预占过期时间',
    release_reason       VARCHAR(30)  NULL COMMENT 'TIMEOUT/USER_CANCEL/SCHEDULE_CANCEL/RECONCILE',
    source               VARCHAR(30)  NULL COMMENT '请求来源:APP/H5/JMETER/ADMIN',
    trace_id             VARCHAR(64)  NULL COMMENT '链路追踪ID',
    remark               VARCHAR(255) NULL COMMENT '备注',
    created_at           DATETIME(3)  NOT NULL DEFAULT CURRENT_TIMESTAMP(3),
    updated_at           DATETIME(3)  NOT NULL DEFAULT CURRENT_TIMESTAMP(3) ON UPDATE CURRENT_TIMESTAMP(3),
    PRIMARY KEY (hold_id),
    UNIQUE KEY uk_hold_request_no (request_no),
    UNIQUE KEY uk_hold_schedule_patient_active (schedule_id, patient_id, active_flag),
    KEY idx_hold_schedule_status_expire (schedule_id, status, expire_time),
    KEY idx_hold_slot_id (slot_id),
    KEY idx_hold_patient_id (patient_id),
    CONSTRAINT fk_hold_schedule FOREIGN KEY (schedule_id) REFERENCES ap_schedule(schedule_id),
    CONSTRAINT fk_hold_slot FOREIGN KEY (slot_id) REFERENCES ap_slot(slot_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='号源预占单表';

DROP TABLE IF EXISTS ap_order;
CREATE TABLE ap_order (
    order_id             VARCHAR(32)   NOT NULL COMMENT '订单ID',
    request_no           VARCHAR(64)   NOT NULL COMMENT '业务幂等号',
    hold_id              VARCHAR(32)   NOT NULL COMMENT '预占单ID',
    patient_id           VARCHAR(32)   NOT NULL COMMENT '患者ID',
    schedule_id          VARCHAR(32)   NOT NULL COMMENT '排班ID',
    amount               DECIMAL(10,2) NOT NULL DEFAULT 0.00 COMMENT '订单金额',
    status               VARCHAR(20)   NOT NULL COMMENT 'CREATED/UNPAID/PAID/CANCELLED/REFUNDING/REFUNDED',
    pay_deadline         DATETIME(3)   NULL COMMENT '支付截止时间',
    pay_time             DATETIME(3)   NULL COMMENT '支付时间',
    cancel_reason        VARCHAR(30)   NULL COMMENT 'TIMEOUT/USER_CANCEL/SCHEDULE_CANCEL',
    source               VARCHAR(30)   NULL COMMENT '下单来源',
    created_at           DATETIME(3)   NOT NULL DEFAULT CURRENT_TIMESTAMP(3),
    updated_at           DATETIME(3)   NOT NULL DEFAULT CURRENT_TIMESTAMP(3) ON UPDATE CURRENT_TIMESTAMP(3),
    PRIMARY KEY (order_id),
    UNIQUE KEY uk_order_request_no (request_no),
    UNIQUE KEY uk_order_hold_id (hold_id),
    KEY idx_order_patient_status (patient_id, status),
    KEY idx_order_schedule_status (schedule_id, status),
    KEY idx_order_pay_deadline (status, pay_deadline),
    CONSTRAINT fk_order_hold FOREIGN KEY (hold_id) REFERENCES ap_hold(hold_id),
    CONSTRAINT fk_order_schedule FOREIGN KEY (schedule_id) REFERENCES ap_schedule(schedule_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='预约订单表';

DROP TABLE IF EXISTS ap_outbox_message;
CREATE TABLE ap_outbox_message (
    msg_id               VARCHAR(32)  NOT NULL COMMENT '消息ID',
    biz_type             VARCHAR(30)  NOT NULL COMMENT 'ORDER_TIMEOUT/SCHEDULE_CANCELLED/PAY_CONFIRMED/HOLD_RELEASED',
    biz_key              VARCHAR(64)  NOT NULL COMMENT '业务主键',
    payload              JSON         NOT NULL COMMENT '消息内容',
    status               VARCHAR(20)  NOT NULL COMMENT 'NEW/PUBLISHED/CONSUMED/FAIL',
    retry_count          INT          NOT NULL DEFAULT 0 COMMENT '重试次数',
    next_retry_time      DATETIME(3)  NULL COMMENT '下次重试时间',
    last_error           VARCHAR(255) NULL COMMENT '最后一次错误信息',
    created_at           DATETIME(3)  NOT NULL DEFAULT CURRENT_TIMESTAMP(3),
    updated_at           DATETIME(3)  NOT NULL DEFAULT CURRENT_TIMESTAMP(3) ON UPDATE CURRENT_TIMESTAMP(3),
    PRIMARY KEY (msg_id),
    KEY idx_outbox_status_retry (status, next_retry_time),
    KEY idx_outbox_biz (biz_type, biz_key)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='本地消息Outbox表';

DROP TABLE IF EXISTS ap_payment_record;
CREATE TABLE ap_payment_record (
    pay_record_id        VARCHAR(32)  NOT NULL COMMENT '支付流水ID',
    order_id             VARCHAR(32)  NOT NULL COMMENT '订单ID',
    pay_channel          VARCHAR(20)  NOT NULL COMMENT '支付渠道',
    pay_status           VARCHAR(20)  NOT NULL COMMENT 'INIT/SUCCESS/FAIL',
    channel_trade_no     VARCHAR(64)  NULL COMMENT '渠道交易流水号',
    callback_payload     TEXT         NULL COMMENT '支付回调内容',
    paid_at              DATETIME(3)  NULL COMMENT '支付成功时间',
    created_at           DATETIME(3)  NOT NULL DEFAULT CURRENT_TIMESTAMP(3),
    updated_at           DATETIME(3)  NOT NULL DEFAULT CURRENT_TIMESTAMP(3) ON UPDATE CURRENT_TIMESTAMP(3),
    PRIMARY KEY (pay_record_id),
    UNIQUE KEY uk_payment_trade (pay_channel, channel_trade_no),
    KEY idx_payment_order_id (order_id),
    CONSTRAINT fk_payment_order FOREIGN KEY (order_id) REFERENCES ap_order(order_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='支付流水表';

DROP TABLE IF EXISTS ap_reconcile_task;
CREATE TABLE ap_reconcile_task (
    task_id              VARCHAR(32)  NOT NULL COMMENT '对账任务ID',
    task_type            VARCHAR(40)  NOT NULL COMMENT '任务类型',
    biz_key              VARCHAR(64)  NOT NULL COMMENT '业务键',
    detail_json          JSON         NULL COMMENT '异常详情',
    status               VARCHAR(20)  NOT NULL COMMENT 'NEW/PROCESSING/SUCCESS/FAIL',
    retry_count          INT          NOT NULL DEFAULT 0,
    last_error           VARCHAR(255) NULL,
    created_at           DATETIME(3)  NOT NULL DEFAULT CURRENT_TIMESTAMP(3),
    updated_at           DATETIME(3)  NOT NULL DEFAULT CURRENT_TIMESTAMP(3) ON UPDATE CURRENT_TIMESTAMP(3),
    PRIMARY KEY (task_id),
    KEY idx_reconcile_type_status (task_type, status),
    KEY idx_reconcile_biz_key (biz_key)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='对账任务表';

DROP TABLE IF EXISTS ap_schedule_event;
CREATE TABLE ap_schedule_event (
    event_id             VARCHAR(32)  NOT NULL COMMENT '事件ID',
    schedule_id          VARCHAR(32)  NOT NULL COMMENT '排班ID',
    event_type           VARCHAR(30)  NOT NULL COMMENT 'CREATE/PUBLISH/SUSPEND/RESUME/CANCEL/GENERATE_SLOTS',
    before_status        VARCHAR(20)  NULL COMMENT '变更前状态',
    after_status         VARCHAR(20)  NULL COMMENT '变更后状态',
    reason               VARCHAR(255) NULL COMMENT '原因',
    operator_id          VARCHAR(32)  NULL COMMENT '操作人ID',
    status               VARCHAR(20)  NOT NULL COMMENT 'NEW/PUBLISHED/PROCESSED/FAIL',
    retry_count          INT          NOT NULL DEFAULT 0,
    created_at           DATETIME(3)  NOT NULL DEFAULT CURRENT_TIMESTAMP(3),
    updated_at           DATETIME(3)  NOT NULL DEFAULT CURRENT_TIMESTAMP(3) ON UPDATE CURRENT_TIMESTAMP(3),
    PRIMARY KEY (event_id),
    KEY idx_schedule_event_schedule (schedule_id),
    KEY idx_schedule_event_type_status (event_type, status),
    CONSTRAINT fk_schedule_event_schedule FOREIGN KEY (schedule_id) REFERENCES ap_schedule(schedule_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='排班事件表';

SET FOREIGN_KEY_CHECKS = 1;

-- 测试初始化建议：
-- 1. 先调用 /api/v2/admin/schedules/test-init 创建并发布排班
-- 2. 再使用返回的 scheduleId 进行预约压测
