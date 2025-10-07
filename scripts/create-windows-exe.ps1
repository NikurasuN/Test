param(
    [string]$JdkHome = $env:JAVA_HOME,
    [string]$BuildDir = (Join-Path $PSScriptRoot "..\build"),
    [string]$OutputDir = (Join-Path $PSScriptRoot "..\dist\windows"),
    [string]$AppVersion = "1.0.0"
)

if (-not $JdkHome) {
    throw "JAVA_HOME is not set. Please point it at a JDK 17+ installation before running this script."
}

$javac = Join-Path $JdkHome "bin\javac.exe"
$jar = Join-Path $JdkHome "bin\jar.exe"
$jpackage = Join-Path $JdkHome "bin\jpackage.exe"

foreach ($tool in @(@{Name="javac"; Path=$javac}, @{Name="jar"; Path=$jar}, @{Name="jpackage"; Path=$jpackage})) {
    if (-not (Test-Path $tool.Path)) {
        throw "Could not find $($tool.Name) at $($tool.Path). Make sure a full JDK is installed."
    }
}

$sourceRoot = Join-Path $PSScriptRoot "..\src\main\java"
if (-not (Test-Path $sourceRoot)) {
    throw "Source directory not found: $sourceRoot"
}

$classesDir = Join-Path $BuildDir "classes"
if (Test-Path $classesDir) {
    Remove-Item -Recurse -Force $classesDir
}
New-Item -ItemType Directory -Force -Path $classesDir | Out-Null

$javaFiles = Get-ChildItem -Path $sourceRoot -Recurse -Filter *.java | ForEach-Object { $_.FullName }
if ($javaFiles.Count -eq 0) {
    throw "No Java source files were found under $sourceRoot"
}

Write-Host "Compiling Java sources..."
& $javac -encoding UTF-8 -d $classesDir @javaFiles
if ($LASTEXITCODE -ne 0) {
    throw "javac failed with exit code $LASTEXITCODE"
}

$jarPath = Join-Path $BuildDir "HeroLineWarsGame.jar"
if (-not (Test-Path $BuildDir)) {
    New-Item -ItemType Directory -Force -Path $BuildDir | Out-Null
}
Write-Host "Packing application jar..."
& $jar --create --file $jarPath --main-class com.example.herolinewars.HeroLineWarsGame -C $classesDir .
if ($LASTEXITCODE -ne 0) {
    throw "jar failed with exit code $LASTEXITCODE"
}

if (-not (Test-Path $OutputDir)) {
    New-Item -ItemType Directory -Force -Path $OutputDir | Out-Null
}

Write-Host "Building Windows installer image with jpackage..."
& $jpackage \
    --type exe \
    --name "HeroLineWarsGame" \
    --app-version $AppVersion \
    --vendor "Hero Line Wars" \
    --input $BuildDir \
    --dest $OutputDir \
    --main-jar (Split-Path -Leaf $jarPath) \
    --main-class "com.example.herolinewars.HeroLineWarsGame" \
    --win-shortcut \
    --win-menu \
    --win-dir-chooser

if ($LASTEXITCODE -ne 0) {
    throw "jpackage failed with exit code $LASTEXITCODE"
}

Write-Host "Executable created under $OutputDir"
