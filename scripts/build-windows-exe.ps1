param(
    [string]$Generator = "Ninja",
    [string]$BuildDir = (Join-Path $PSScriptRoot "..\build\windows"),
    [string]$OutputDir = (Join-Path $PSScriptRoot "..\dist\windows"),
    [switch]$Clean
)

if (-not (Get-Command cmake -ErrorAction SilentlyContinue)) {
    throw "cmake must be available on PATH to build the project."
}

if ($Clean -and (Test-Path $BuildDir)) {
    Remove-Item -Recurse -Force $BuildDir
}

$configureArgs = @(
    '-S', (Join-Path $PSScriptRoot '..'),
    '-B', $BuildDir,
    '-G', $Generator,
    '-DCMAKE_BUILD_TYPE=Release'
)

Write-Host "Configuring project with cmake..."
cmake @configureArgs
if ($LASTEXITCODE -ne 0) {
    throw "cmake configuration failed with exit code $LASTEXITCODE"
}

Write-Host "Building release executable..."
cmake --build $BuildDir --config Release
if ($LASTEXITCODE -ne 0) {
    throw "Build failed with exit code $LASTEXITCODE"
}

if (-not (Test-Path $OutputDir)) {
    New-Item -ItemType Directory -Force -Path $OutputDir | Out-Null
}

$executables = @('hero_line_wars.exe', 'run_game.exe')

foreach ($name in $executables) {
    $exePath = Join-Path $BuildDir $name
    if (-not (Test-Path $exePath)) {
        $altPath = Join-Path $BuildDir "Release/$name"
        if (Test-Path $altPath) {
            $exePath = $altPath
        } elseif ($name -eq 'hero_line_wars.exe') {
            throw "Could not locate $name in the build directory."
        } else {
            Write-Warning "Could not locate $name; skipping copy."
            continue
        }
    }

    Copy-Item $exePath -Destination $OutputDir -Force
    Write-Host "$name copied to $OutputDir"
}
