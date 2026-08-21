import argparse
import json
import os
import sys
from datetime import datetime, timedelta
from urllib import request, error


def post_json(url: str, payload: dict, timeout: int = 10) -> dict:
    data = json.dumps(payload).encode("utf-8")
    req = request.Request(
        url=url,
        data=data,
        headers={"Content-Type": "application/json"},
        method="POST",
    )
    try:
        with request.urlopen(req, timeout=timeout) as resp:
            body = resp.read().decode("utf-8")
            return json.loads(body)
    except error.HTTPError as e:
        body = e.read().decode("utf-8", errors="ignore")
        raise RuntimeError(f"HTTP {e.code}: {body}") from e
    except error.URLError as e:
        raise RuntimeError(f"Request failed: {e}") from e


def build_payload(args) -> dict:
    now = datetime.now()
    visit_date = (now + timedelta(days=args.days_after)).strftime("%Y-%m-%d")
    open_time = now.strftime("%Y-%m-%dT%H:%M:%S")
    close_time = (now + timedelta(hours=args.close_after_hours)).strftime("%Y-%m-%dT%H:%M:%S")

    return {
        "doctorId": args.doctor_id,
        "deptId": args.dept_id,
        "hospitalId": args.hospital_id,
        "visitDate": visit_date,
        "timePeriod": args.time_period,
        "totalCount": args.total_count,
        "allowCancel": 1 if args.allow_cancel else 0,
        "openTime": open_time,
        "closeTime": close_time,
    }


def write_properties(output_file: str, base_url: str, payload: dict, data: dict) -> None:
    parent = os.path.dirname(output_file)
    if parent:
        os.makedirs(parent, exist_ok=True)

    lines = [
        f"baseUrl={base_url}",
        f"scheduleId={data.get('scheduleId', '')}",
        f"status={data.get('status', '')}",
        f"totalCount={data.get('totalCount', '')}",
        f"availableCount={data.get('availableCount', '')}",
        f"doctorId={payload.get('doctorId', '')}",
        f"deptId={payload.get('deptId', '')}",
        f"hospitalId={payload.get('hospitalId', '')}",
        f"visitDate={payload.get('visitDate', '')}",
        f"timePeriod={payload.get('timePeriod', '')}",
    ]

    with open(output_file, "w", encoding="utf-8") as fp:
        fp.write("\n".join(lines) + "\n")


def print_jmeter_command(args, schedule_id: str) -> None:
    if not args.jmeter_bin or not args.test_plan:
        return

    command = (
        f"\"{args.runner}\" "
        f"\"{args.jmeter_bin}\" "
        f"\"{args.test_plan}\" "
        f"\"{args.base_url.rstrip('/')}\" "
        f"\"{schedule_id}\" "
        f"{args.threads} {args.ramp_up} {args.loops}"
    )
    print("\n=== JMeter Command Example ===")
    print(command)


def main():
    parser = argparse.ArgumentParser(description="Prepare a fresh test schedule for pressure testing.")
    parser.add_argument("--base-url", required=True, help="e.g. http://169.254.30.44:8210")
    parser.add_argument("--doctor-id", default="doc_test_001")
    parser.add_argument("--dept-id", default="dept_test_001")
    parser.add_argument("--hospital-id", default="hospital_test_001")
    parser.add_argument("--time-period", type=int, default=0, choices=[0, 1, 2], help="0上午 1下午 2晚上")
    parser.add_argument("--total-count", type=int, default=30)
    parser.add_argument("--days-after", type=int, default=1, help="visitDate = today + N days")
    parser.add_argument("--close-after-hours", type=int, default=12)
    parser.add_argument("--allow-cancel", action="store_true", default=True)
    parser.add_argument("--timeout", type=int, default=10)
    parser.add_argument(
        "--output-file",
        default="",
        help="optional .properties output path for JMeter reuse",
    )
    parser.add_argument(
        "--runner",
        default="run_appointment_test.bat",
        help="runner script path shown in command example",
    )
    parser.add_argument("--jmeter-bin", default="", help="optional JMeter bin path for command example")
    parser.add_argument("--test-plan", default="", help="optional .jmx path for command example")
    parser.add_argument("--threads", type=int, default=100, help="shown in JMeter command example")
    parser.add_argument("--ramp-up", type=int, default=10, help="shown in JMeter command example")
    parser.add_argument("--loops", type=int, default=1, help="shown in JMeter command example")

    args = parser.parse_args()
    payload = build_payload(args)
    url = args.base_url.rstrip("/") + "/api/v2/admin/schedules/test-init"

    print("=== Request Payload ===")
    print(json.dumps(payload, ensure_ascii=False, indent=2))
    print(f"\nPOST {url}\n")

    try:
        result = post_json(url, payload, timeout=args.timeout)
    except Exception as ex:
        print(f"[ERROR] 初始化失败: {ex}", file=sys.stderr)
        sys.exit(1)

    print("=== Raw Response ===")
    print(json.dumps(result, ensure_ascii=False, indent=2))

    data = result.get("data") or {}
    schedule_id = data.get("scheduleId")
    if not schedule_id:
        print("\n[WARN] 返回中未找到 scheduleId，请检查接口返回结构。")
        return

    print("\n=== Ready For JMeter ===")
    print(f"scheduleId={schedule_id}")
    print(f"status={data.get('status')}")
    print(f"totalCount={data.get('totalCount')}")
    print(f"availableCount={data.get('availableCount')}")
    print(f"visitDate={data.get('visitDate')}")
    print(f"timePeriod={data.get('timePeriod')}")

    if args.output_file:
        write_properties(args.output_file, args.base_url.rstrip("/"), payload, data)
        print(f"outputFile={args.output_file}")

    print_jmeter_command(args, schedule_id)


if __name__ == "__main__":
    main()
