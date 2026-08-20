-- 预约核心域压测准备说明
-- 1. 先执行 V1__init_appointment_core.sql 建库建表
-- 2. 启动 service_appointment_core
-- 3. 使用 /api/v2/admin/schedules/test-init 创建压测排班
-- 4. JMeter 中使用下述患者号模板与 scheduleId 组合压测

-- 患者编号建议：
-- P000001
-- P000002
-- P000003
-- ...
-- P000100

-- requestNo 建议：
-- REQ_000001
-- REQ_000002
-- REQ_000003
-- ...
-- REQ_000100

-- 示例预约报文模板：
-- {
--   "requestNo": "REQ_000001",
--   "patientId": "P000001",
--   "scheduleId": "SCH_xxx",
--   "source": "JMETER"
-- }
