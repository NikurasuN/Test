@echo off
setlocal ENABLEDELAYEDEXPANSION

REM Determine project root based on script location
set "SCRIPT_DIR=%~dp0"
for %%i in ("%SCRIPT_DIR%..") do set "PROJECT_ROOT=%%~fi"

REM Default build directory can be overridden via BUILD_DIR env or --build-dir/-B flag
if defined BUILD_DIR (
    set "SELECTED_BUILD_DIR=%BUILD_DIR%"
) else (
    set "SELECTED_BUILD_DIR=build"
)

set "GAME_ARGS="

:parse_args
if "%~1"=="" goto after_parse
if "%~1"=="--" (
    shift
    goto append_rest
)
if /I "%~1"=="--build-dir" (
    shift
    if "%~1"=="" goto usage
    set "SELECTED_BUILD_DIR=%~1"
    shift
    goto parse_args
) else if /I "%~1"=="-B" (
    shift
    if "%~1"=="" goto usage
    set "SELECTED_BUILD_DIR=%~1"
    shift
    goto parse_args
)
if defined GAME_ARGS (
    set "GAME_ARGS=%GAME_ARGS% "%~1""
) else (
    set "GAME_ARGS="%~1""
)
shift
goto parse_args

:append_rest
if "%~1"=="" goto after_parse
if defined GAME_ARGS (
    set "GAME_ARGS=%GAME_ARGS% "%~1""
) else (
    set "GAME_ARGS="%~1""
)
shift
goto append_rest

:after_parse
pushd "%PROJECT_ROOT%" >nul
for %%i in ("%SELECTED_BUILD_DIR%") do set "BUILD_DIR=%%~fi"
popd >nul

set "CMAKE_GENERATOR_NAME="
if defined CMAKE_GENERATOR (
    set "CMAKE_GENERATOR_NAME=%CMAKE_GENERATOR%"
) else (
    where ninja >nul 2>nul
    if not errorlevel 1 (
        set "CMAKE_GENERATOR_NAME=Ninja"
    ) else (
        where msbuild >nul 2>nul
        if not errorlevel 1 (
            set "CMAKE_GENERATOR_NAME=Visual Studio 17 2022"
        )
    )
)

set "CMAKE_ARCH_ARGS="
if defined CMAKE_GENERATOR_NAME (
    if /I "%CMAKE_GENERATOR_NAME%"=="Visual Studio 17 2022" (
        if defined VSCMD_ARG_TGT_ARCH (
            set "CMAKE_ARCH_ARGS=-A %VSCMD_ARG_TGT_ARCH%"
        ) else (
            set "CMAKE_ARCH_ARGS=-A x64"
        )
    )
)

echo Configuring project in "%BUILD_DIR%"...
if defined CMAKE_GENERATOR_NAME (
    echo Using CMake generator: %CMAKE_GENERATOR_NAME%
    cmake -S "%PROJECT_ROOT%" -B "%BUILD_DIR%" -G "%CMAKE_GENERATOR_NAME%" %CMAKE_ARCH_ARGS%
) else (
    cmake -S "%PROJECT_ROOT%" -B "%BUILD_DIR%"
)
if errorlevel 1 goto cmake_failed

echo Building project...
cmake --build "%BUILD_DIR%"
if errorlevel 1 goto build_failed

call :locate_executable "%BUILD_DIR%"
if not defined GAME_EXE (
    echo Could not locate hero_line_wars.exe inside "%BUILD_DIR%".
    goto fail
)

echo Launching %GAME_EXE% %GAME_ARGS%
"%GAME_EXE%" %GAME_ARGS%
goto :eof

:locate_executable
set "GAME_EXE="
if exist "%~1\hero_line_wars.exe" (
    set "GAME_EXE=%~1\hero_line_wars.exe"
    goto :eof
)
for /f "delims=" %%f in ('dir /b /s "%~1\hero_line_wars.exe" 2^>nul') do (
    set "GAME_EXE=%%f"
    goto :found
)
:found
goto :eof

:usage
echo Usage: %~nx0 [--build-dir <path>] [-- <game arguments>]
goto fail

:cmake_failed
echo CMake configuration failed.
goto fail

:build_failed
echo Build failed.

goto fail

:fail
exit /b 1
