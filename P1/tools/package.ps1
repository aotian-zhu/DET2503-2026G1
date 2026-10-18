param([string]$TeamName = 'G1')
$ErrorActionPreference = 'Stop'
if ($TeamName -notmatch '^[A-Za-z0-9_-]+$') { throw 'TeamName may contain letters, digits, underscores and hyphens.' }

$taskProjectRoot = [IO.Path]::GetFullPath((Split-Path -Parent $PSScriptRoot))
$taskOutputRoot = Join-Path $taskProjectRoot 'deliverables'
$taskVideoName = 'ArenaCompanion-P1-10min-demo.mp4'
$taskVideoPath = Join-Path $taskOutputRoot $taskVideoName

# Allow only Android project inputs. Developer docs, APKs, caches and credentials
# stay outside the teacher's archive. Required test observations are in README.
$taskProjectFiles = @(
    '.gitignore',
    'app/build.gradle.kts',
    'app/proguard-rules.pro',
    'build.gradle.kts',
    'settings.gradle.kts',
    'gradle.properties',
    'gradlew',
    'gradlew.bat',
    'local.properties.example',
    'gradle/libs.versions.toml',
    'gradle/wrapper/gradle-wrapper.jar',
    'gradle/wrapper/gradle-wrapper.properties'
)
$taskEntries = [Collections.Generic.List[object]]::new()
foreach ($taskRelative in $taskProjectFiles) {
    $taskSourcePath = Join-Path $taskProjectRoot $taskRelative
    if (-not (Test-Path -LiteralPath $taskSourcePath -PathType Leaf)) { throw ('Required project file is missing: ' + $taskRelative) }
    $taskEntries.Add([pscustomobject]@{ Source = $taskSourcePath; Entry = 'P1/' + $taskRelative })
}
$taskSourceRoot = Join-Path $taskProjectRoot 'app/src'
$taskSourceFiles = @(Get-ChildItem -LiteralPath $taskSourceRoot -File -Recurse | Sort-Object FullName)
if ($taskSourceFiles.Count -eq 0) { throw 'Application source files are missing.' }
foreach ($taskSourceFile in $taskSourceFiles) {
    if ($taskSourceFile.Name -in @('.DS_Store', 'Thumbs.db')) { continue }
    $taskRelative = $taskSourceFile.FullName.Substring($taskProjectRoot.Length + 1).Replace('\', '/')
    $taskEntries.Add([pscustomobject]@{ Source = $taskSourceFile.FullName; Entry = 'P1/' + $taskRelative })
}
$taskReadmePath = Join-Path $taskProjectRoot 'README.md'
foreach ($taskRequired in @($taskReadmePath, $taskVideoPath)) {
    if (-not (Test-Path -LiteralPath $taskRequired -PathType Leaf)) { throw ('Required submission file is missing: ' + $taskRequired) }
    if ((Get-Item -LiteralPath $taskRequired).Length -eq 0) { throw ('Required submission file is empty: ' + $taskRequired) }
}
$taskEntries.Add([pscustomobject]@{ Source = $taskReadmePath; Entry = 'README.md' })
$taskEntries.Add([pscustomobject]@{ Source = $taskVideoPath; Entry = $taskVideoName })

$taskStagingRoot = Join-Path $taskProjectRoot '.tools/packaging'
$taskStage = Join-Path $taskStagingRoot ([Guid]::NewGuid().ToString())
New-Item -ItemType Directory -Path $taskStage,$taskOutputRoot -Force | Out-Null
$taskZip = Join-Path $taskOutputRoot ('DTE_2503_26_' + $TeamName + '_P1.zip')
$taskTemporaryZip = Join-Path $taskStage 'submission.zip'
Add-Type -AssemblyName System.IO.Compression
$taskZipStream = [IO.File]::Open($taskTemporaryZip, [IO.FileMode]::CreateNew)
$taskArchive = [IO.Compression.ZipArchive]::new($taskZipStream, [IO.Compression.ZipArchiveMode]::Create, $false)
try {
    foreach ($taskFile in ($taskEntries | Sort-Object Entry)) {
        $taskEntry = $taskArchive.CreateEntry($taskFile.Entry, [IO.Compression.CompressionLevel]::Optimal)
        $taskSourceStream = [IO.File]::OpenRead($taskFile.Source)
        $taskEntryStream = $taskEntry.Open()
        try { $taskSourceStream.CopyTo($taskEntryStream) }
        finally { $taskSourceStream.Dispose(); $taskEntryStream.Dispose() }
    }
} finally { $taskArchive.Dispose(); $taskZipStream.Dispose() }
Move-Item -LiteralPath $taskTemporaryZip -Destination $taskZip -Force

# Remove only this invocation's freshly created staging directory.
$taskResolvedStage = [IO.Path]::GetFullPath($taskStage)
$taskAllowedPrefix = [IO.Path]::GetFullPath($taskStagingRoot) + [IO.Path]::DirectorySeparatorChar
if (-not $taskResolvedStage.StartsWith($taskAllowedPrefix, [StringComparison]::OrdinalIgnoreCase)) { throw 'Unsafe staging cleanup path.' }
Remove-Item -LiteralPath $taskResolvedStage -Recurse -Force

Write-Output ('Created: ' + $taskZip)
Write-Output ('Top-level items: P1/, ' + $taskVideoName + ', README.md')
Write-Output ('Files: ' + $taskEntries.Count)
Write-Output ('Bytes: ' + (Get-Item -LiteralPath $taskZip).Length)
