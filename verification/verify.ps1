param(
    [Parameter(Mandatory = $true)]
    [ValidateSet('forge-1.20.1', 'neoforge-1.21.1', 'neoforge-26.1.2')]
    [string]$Target
)

$ErrorActionPreference = 'Stop'
$projectRoot = Split-Path -Parent $PSScriptRoot
$targetRoot = Join-Path $projectRoot $Target
$runRoot = Join-Path $targetRoot 'build/verification-run'
$utf8 = [Text.UTF8Encoding]::new($false)
[IO.Directory]::CreateDirectory($runRoot) | Out-Null
[IO.File]::WriteAllText((Join-Path $runRoot 'eula.txt'), "eula=true`n", $utf8)
$properties = @'
online-mode=false
server-ip=127.0.0.1
server-port=0
enable-status=false
view-distance=2
simulation-distance=2
spawn-protection=0
level-type=minecraft:flat
generate-structures=false
max-tick-time=-1
'@
[IO.File]::WriteAllText((Join-Path $runRoot 'server.properties'), $properties, $utf8)
$log = Join-Path $targetRoot 'build/verification-console.log'
Push-Location -LiteralPath $targetRoot
try {
    & .\gradlew.bat runServer -Pverification --console=plain --max-workers=2 *> $log
    $result = $LASTEXITCODE
    Get-Content -LiteralPath $log -Tail 35
    if ($result -ne 0 -or !(Select-String -LiteralPath $log -SimpleMatch 'ADDON_VERIFICATION_PASSED' -Quiet)) {
        throw "Verification failed. Inspect $log"
    }
} finally {
    Pop-Location
}
