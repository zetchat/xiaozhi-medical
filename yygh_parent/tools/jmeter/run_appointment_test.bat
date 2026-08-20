@echo off
setlocal

set "SCRIPT_DIR=%~dp0"
set "PS_SCRIPT=%SCRIPT_DIR%run_appointment_test.ps1"

if "%~4"=="" (
    echo 用法:
    echo   run_appointment_test.bat ^<JMeterBin^> ^<TestPlan^> ^<BaseUrl^> ^<ScheduleId^> [Threads] [RampUp] [Loops]
    echo.
    echo 示例:
    echo   run_appointment_test.bat "C:\apache-jmeter-5.6.3\bin\jmeter.bat" "C:\jmeter\appointment-create.jmx" "http://192.168.1.100:8200" "1912345678901234567" 200 20 1
    exit /b 1
)

set "JMETER_BIN=%~1"
set "TEST_PLAN=%~2"
set "BASE_URL=%~3"
set "SCHEDULE_ID=%~4"
set "THREADS=%~5"
set "RAMP_UP=%~6"
set "LOOPS=%~7"

if "%THREADS%"=="" set "THREADS=100"
if "%RAMP_UP%"=="" set "RAMP_UP=10"
if "%LOOPS%"=="" set "LOOPS=1"

powershell -ExecutionPolicy Bypass -File "%PS_SCRIPT%" ^
  -JMeterBin "%JMETER_BIN%" ^
  -TestPlan "%TEST_PLAN%" ^
  -BaseUrl "%BASE_URL%" ^
  -ScheduleId "%SCHEDULE_ID%" ^
  -Threads %THREADS% ^
  -RampUp %RAMP_UP% ^
  -Loops %LOOPS% ^
  -GenerateReport

exit /b %ERRORLEVEL%
