param([switch]$DeviceTests)
$ErrorActionPreference = 'Stop'
$taskProjectRoot = Split-Path -Parent $PSScriptRoot
# Select a JDK compatible with the A2 Gradle wrapper. Android Studio's JDK 25 is too new.
$taskJavaCandidates = @($env:JAVA_HOME, (Join-Path $env:USERPROFILE '.jdks\openjdk-23.0.2'))
$taskCompatibleJavaFound = $false
foreach ($taskJavaCandidate in $taskJavaCandidates) {
    if ($taskJavaCandidate -and (Test-Path -LiteralPath (Join-Path $taskJavaCandidate 'bin\java.exe'))) {
        $taskJavaVersion = & (Join-Path $taskJavaCandidate 'bin\java.exe') -version 2>&1 | Out-String
        if ($taskJavaVersion -match 'version "(17|18|19|20|21|22|23)[.]') { $env:JAVA_HOME = $taskJavaCandidate; $taskCompatibleJavaFound = $true; break }
    }
}
if (-not $taskCompatibleJavaFound) { throw 'Set JAVA_HOME to JDK 17 or 21, or select one in Android Studio Gradle settings.' }
$env:GRADLE_USER_HOME = Join-Path $taskProjectRoot '.gradle-user-home'
$taskBuildTasks = @(':app:assembleDebug', ':app:testDebugUnitTest', ':app:lintDebug', ':app:assembleDebugAndroidTest')
if ($DeviceTests) { $taskBuildTasks += ':app:connectedDebugAndroidTest' }
Push-Location $taskProjectRoot
try {
    & .\gradlew.bat @taskBuildTasks --console=plain
    if ($LASTEXITCODE -ne 0) { throw "Build/checks failed with exit code $LASTEXITCODE" }
} finally { Pop-Location }
