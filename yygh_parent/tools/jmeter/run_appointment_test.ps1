param(
    [Parameter(Mandatory = $true)]
    [string]$JMeterBin,

    [Parameter(Mandatory = $true)]
    [string]$TestPlan,

    [Parameter(Mandatory = $true)]
    [string]$Protocol,

    [Parameter(Mandatory = $true)]
    [string]$ServerHost,

    [Parameter(Mandatory = $true)]
    [int]$Port,

    [Parameter(Mandatory = $true)]
    [string]$ScheduleId,

    [int]$Threads = 100,
    [int]$RampUp = 10,
    [int]$Loops = 1,

    [string]$ResultRoot = "$PSScriptRoot\\results",

    [switch]$GenerateReport
)

Set-StrictMode -Version Latest
$ErrorActionPreference = "Stop"

if (-not (Test-Path -Path $JMeterBin)) {
    throw "JMeter 启动脚本不存在: $JMeterBin"
}

if (-not (Test-Path -Path $TestPlan)) {
    throw "JMeter 测试计划不存在: $TestPlan"
}

$timestamp = Get-Date -Format "yyyyMMdd_HHmmss"
$runDir = Join-Path $ResultRoot "run_$timestamp"
$resultFile = Join-Path $runDir "result.jtl"
$logFile = Join-Path $runDir "jmeter.log"
$reportDir = Join-Path $runDir "html-report"

New-Item -ItemType Directory -Path $runDir -Force | Out-Null

$args = @(
    "-n",
    "-t", $TestPlan,
    "-l", $resultFile,
    "-j", $logFile,

    "-Jprotocol=$Protocol",
    "-Jhost=$ServerHost",
    "-Jport=$Port",

    "-JscheduleId=$ScheduleId",
    "-Jthreads=$Threads",
    "-JrampUp=$RampUp",
    "-Jloops=$Loops"
)

if ($GenerateReport) {
    $args += @("-e", "-o", $reportDir)
}

Write-Host "开始执行 JMeter 压测..."
Write-Host "JMeterBin : $JMeterBin"
Write-Host "TestPlan  : $TestPlan"

Write-Host "Protocol  : $Protocol"
Write-Host "Host      : $ServerHost"
Write-Host "Port      : $Port"

Write-Host "ScheduleId: $ScheduleId"
Write-Host "Threads   : $Threads"
Write-Host "RampUp    : $RampUp"
Write-Host "Loops     : $Loops"
Write-Host "RunDir    : $runDir"
Write-Host ""
Write-Host "实际命令:"
Write-Host "& `"$JMeterBin`" $($args -join ' ')"
Write-Host ""

& $JMeterBin @args
$exitCode = $LASTEXITCODE

Write-Host ""
Write-Host "执行完成，退出码: $exitCode"
Write-Host "结果文件: $resultFile"
Write-Host "日志文件: $logFile"

if ($GenerateReport) {
    Write-Host "HTML 报告: $reportDir"
}

exit $exitCode
