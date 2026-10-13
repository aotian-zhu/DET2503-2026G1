param([string]$TeamName = 'G1')
$ErrorActionPreference = 'Stop'
if ($TeamName -notmatch '^[A-Za-z0-9_-]+$') { throw 'TeamName may contain letters, digits, underscores and hyphens.' }
$taskProjectRoot = [IO.Path]::GetFullPath((Split-Path -Parent $PSScriptRoot))
$taskOutputRoot = Join-Path $taskProjectRoot 'deliverables'
$taskStagingRoot = Join-Path $taskProjectRoot '.tools\packaging'
$taskStage = Join-Path $taskStagingRoot ([Guid]::NewGuid().ToString())
$taskCopyRoot = Join-Path $taskStage 'P1'
New-Item -ItemType Directory -Path $taskCopyRoot,$taskOutputRoot -Force | Out-Null
$taskIgnoredParts = @('.git','.gradle','.gradle-user-home','.idea','.kotlin','.tools','build','captures')
Get-ChildItem -LiteralPath $taskProjectRoot -File -Recurse -Force | ForEach-Object {
    $taskRelative = $_.FullName.Substring($taskProjectRoot.Length + 1)
    $taskParts = $taskRelative -split '[\\/]'
    $taskSkip = @($taskParts | Where-Object { $_ -in $taskIgnoredParts }).Count -gt 0
    if (-not $taskSkip -and $_.Name -ne 'local.properties' -and $_.Extension -ne '.zip' -and $_.Name -notlike 'emulator-*.log') {
        $taskCopyTarget = Join-Path $taskCopyRoot $taskRelative
        New-Item -ItemType Directory -Path (Split-Path -Parent $taskCopyTarget) -Force | Out-Null
        Copy-Item -LiteralPath $_.FullName -Destination $taskCopyTarget
    }
}
$taskZip = Join-Path $taskOutputRoot "DTE_2503_26_${TeamName}_P1.zip"
Compress-Archive -LiteralPath $taskCopyRoot -DestinationPath $taskZip -Force
# The only recursive cleanup is the freshly created staging folder, checked below.
$taskResolvedStage = [IO.Path]::GetFullPath($taskStage)
$taskAllowedPrefix = [IO.Path]::GetFullPath($taskStagingRoot) + [IO.Path]::DirectorySeparatorChar
if (-not $taskResolvedStage.StartsWith($taskAllowedPrefix, [StringComparison]::OrdinalIgnoreCase)) { throw 'Unsafe staging cleanup path.' }
Remove-Item -LiteralPath $taskResolvedStage -Recurse -Force
Write-Output $taskZip
