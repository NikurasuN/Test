[CmdletBinding()]
param(
    [string]$BuildDir
)

Set-StrictMode -Version Latest
$ErrorActionPreference = 'Stop'

function Find-Executable {
    param([string]$BuildDirectory)

    $candidates = @()
    $candidates += Join-Path $BuildDirectory 'hero_line_wars.exe'

    if ($env:CMAKE_BUILD_TYPE) {
        $candidates += Join-Path (Join-Path $BuildDirectory $env:CMAKE_BUILD_TYPE) 'hero_line_wars.exe'
    }

    $candidates += Join-Path (Join-Path $BuildDirectory 'Debug') 'hero_line_wars.exe'
    $candidates += Join-Path (Join-Path $BuildDirectory 'Release') 'hero_line_wars.exe'

    foreach ($candidate in $candidates) {
        if ($candidate -and (Test-Path $candidate)) {
            return (Resolve-Path $candidate).Path
        }
    }

    $found = Get-ChildItem -Path $BuildDirectory -Filter 'hero_line_wars.exe' -Recurse -File -ErrorAction SilentlyContinue |
        Select-Object -First 1
    if ($found) {
        return $found.FullName
    }

    return $null
}

$projectRoot = (Resolve-Path (Join-Path $PSScriptRoot '..')).Path

if (-not $BuildDir) {
    if ($env:BUILD_DIR) {
        $BuildDir = $env:BUILD_DIR
    } else {
        $BuildDir = Join-Path $projectRoot 'build'
    }
}

$buildDirPath = Resolve-Path -Path $BuildDir -ErrorAction SilentlyContinue
if ($buildDirPath) {
    $BuildDir = $buildDirPath.Path
} else {
    $BuildDir = [System.IO.Path]::GetFullPath($BuildDir)
}

$executablePath = Find-Executable -BuildDirectory $BuildDir

if (-not $executablePath) {
    $cmakeArgs = @('-S', $projectRoot, '-B', $BuildDir)
    if ($env:CMAKE_GENERATOR) {
        $cmakeArgs += @('-G', $env:CMAKE_GENERATOR)
    }

    cmake @cmakeArgs
    cmake --build $BuildDir

    $executablePath = Find-Executable -BuildDirectory $BuildDir
}

if (-not $executablePath) {
    throw "Unable to locate hero_line_wars.exe in '$BuildDir'."
}

& $executablePath @args
