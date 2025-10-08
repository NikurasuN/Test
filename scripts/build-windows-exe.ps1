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

$executable = Join-Path $BuildDir 'hero_line_wars.exe'
if (-not (Test-Path $executable)) {
    $altPath = Join-Path $BuildDir 'Release/hero_line_wars.exe'
    if (Test-Path $altPath) {
        $executable = $altPath
    } else {
        throw "Could not locate hero_line_wars.exe in the build directory."
    }
}

Copy-Item $executable -Destination $OutputDir -Force
Write-Host "Executable copied to $OutputDir"
