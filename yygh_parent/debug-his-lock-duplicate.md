# Debug Session: his-lock-duplicate
- **Status**: [OPEN]
- **Issue**: `grabTicket` 二次测试时，HIS 侧插入 `his_lock_record` 失败，报 `Duplicate entry 'DET_001' for key 'his_lock_record.uk_detail_id'`，订单侧收到异常后回滚 Redis 并返回失败。
- **Debug Server**: N/A
- **Log File**: N/A

## Reproduction Steps
1. 准备 Redis 票池：`TICKET_POOL:SCH_001`
2. 准备 HIS 可用号源：`his_schedule_detail.detail_id = DET_001`
3. 调用 `POST /api/order/orderInfo/auth/grabTicket`
4. 首次请求后如果发生回滚或解锁，再次调用同一接口

## Hypotheses & Verification
| ID | Hypothesis | Likelihood | Effort | Evidence |
|----|------------|------------|--------|----------|
| A | `his_lock_record` 上对 `detail_id` 做了唯一约束，但解锁时只改状态不清理旧记录，导致同一 `detail_id` 不能再次插入 | High | Low | Pending |
| B | `findByScheduleAndPatient` 的幂等查询条件过弱，只能复用同患者记录，无法复用“已释放的同号源记录”，最终走到重复插入 | High | Low | Pending |
| C | `unlockTicket` 只把 `his_schedule_detail` 改回 `AVAILABLE`，没有同步处理 `his_lock_record` 的唯一键冲突前提 | High | Low | Pending |
| D | 表结构中的唯一索引设计与当前状态机不一致，本意应该更新旧记录而不是插入新记录 | Medium | Medium | Pending |

## Log Evidence
- 用户提供日志显示：`INSERT INTO his_lock_record ... detail_id = DET_001` 命中唯一索引 `uk_detail_id`
- 用户提供日志显示：此前已执行 `UPDATE his_schedule_detail SET status = 'AVAILABLE' WHERE detail_id = DET_001 AND status = 'LOCKED'`

## Verification Conclusion
- A：Confirmed。日志已明确给出 `Duplicate entry 'DET_001' for key 'his_lock_record.uk_detail_id'`，同时代码在解锁时仅执行 `updateStatus(..., "RELEASED")`，没有移除旧记录的 `detail_id` 占用。
- B：Confirmed。`findByScheduleAndPatient(scheduleId, patientId)` 只能命中“同患者 + 同排班”的旧记录，而且只有 `LOCKED` 才直接返回；旧记录一旦是 `RELEASED`，仍会继续走插入分支。
- C：Confirmed。`unlockTicket` 只恢复了 `his_schedule_detail` 为 `AVAILABLE`，并把流水状态设为 `RELEASED`，但没有释放 `his_lock_record.detail_id` 的唯一约束占用。
- D：Partially confirmed。当前表约束更像“一个 detail_id 任何时刻只允许有一条流水记录”，但业务代码是“每次锁号都插入一条新流水”，两者模型不一致。
