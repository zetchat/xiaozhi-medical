# Appointment Core API Examples

## 1. 一键初始化压测排班

`POST /api/v2/admin/schedules/test-init`

```json
{
  "doctorId": "DOC_PRESS_001",
  "deptId": "DEP_PRESS_001",
  "hospitalId": "HOSP_PRESS_001",
  "visitDate": "2026-08-25",
  "timePeriod": 0,
  "totalCount": 100,
  "allowCancel": 1,
  "openTime": "2026-08-19T09:00:00",
  "closeTime": "2026-08-25T11:30:00"
}
```

成功后记下返回的 `scheduleId`，后续预约接口直接用这个值压测。

## 2. 创建预约

`POST /api/v2/appointments`

```json
{
  "requestNo": "REQ_000001",
  "patientId": "P000001",
  "scheduleId": "替换成上一步返回的scheduleId",
  "source": "JMETER"
}
```

## 3. 查询订单

`GET /api/v2/orders/{orderId}`

返回中会包含：
- `orderStatus`
- `holdStatus`
- `sequenceNo`
- `payDeadline`
- `payTime`

## 4. 主动取消未支付订单

`POST /api/v2/orders/{orderId}/cancel`

当前版本仅支持取消 `UNPAID` 订单。

## 5. 支付确认

`POST /api/v2/payments/confirm`

```json
{
  "orderId": "订单ID",
  "payChannel": "MOCK",
  "channelTradeNo": "TRADE_000001"
}
```

## 6. 排班运营动作

- `POST /api/v2/admin/schedules/{scheduleId}/suspend`
- `POST /api/v2/admin/schedules/{scheduleId}/resume`
- `POST /api/v2/admin/schedules/{scheduleId}/cancel`

说明：
- `suspend` 会把 Redis 令牌重置为 `0`
- `resume` 会按当前 `availableCount` 重建 Redis 令牌
- `cancel` 当前只允许在没有 `held/confirmed` 号源时执行

## 7. 查询对账任务

`GET /api/v2/admin/reconcile/tasks?limit=20`

当前会生成的任务类型：
- `SCHEDULE_COUNTER_MISMATCH`
