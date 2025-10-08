@echo off
setlocal enabledelayedexpansion

set "SCRIPT_DIR=%~dp0"
if "%SCRIPT_DIR:~-1%"=="\" set "SCRIPT_DIR=%SCRIPT_DIR:~0,-1%"
set "BUILD_DIR=%SCRIPT_DIR%\.launcher-build"
set "GAME_EXE=%BUILD_DIR%\hero_line_wars.exe"
set "SRC_DIR=%SCRIPT_DIR%\src"
set "SOURCE_FILES=main.cpp HeroLineWarsGame.cpp Hero.cpp IconLibrary.cpp Item.cpp Team.cpp UnitType.cpp"

call :find_compiler
if errorlevel 1 exit /b 1

call :needs_rebuild
if errorlevel 1 (
    call :compile
    if errorlevel 1 exit /b 1
)

pushd "%BUILD_DIR%" >nul 2>&1
"%GAME_EXE%" %*
set "EXITCODE=%ERRORLEVEL%"
popd >nul 2>&1
exit /b %EXITCODE%

:find_compiler
for %%C in (cl.exe g++.exe clang++.exe) do (
    for /f "usebackq delims=" %%P in (`where %%C 2^>nul`) do (
        set "COMPILER_CMD=%%P"
        set "COMPILER_BASENAME=%%~nP"
        goto :compiler_found
    )
)
echo [launcher] Could not find cl.exe, g++.exe, or clang++.exe on PATH.
exit /b 1
:compiler_found
set "COMPILER_TYPE=!COMPILER_BASENAME!"
exit /b 0

:needs_rebuild
if not exist "%GAME_EXE%" exit /b 1
for %%S in (%SOURCE_FILES%) do (
    set "FULL_PATH=%SRC_DIR%\%%S"
    if exist "!FULL_PATH!" (
        for %%F in ("!FULL_PATH!") do (
            for %%B in ("%GAME_EXE%") do (
                if %%~tF GTR %%~tB exit /b 1
            )
        )
    )
)
exit /b 0

:compile
if not exist "%BUILD_DIR%" mkdir "%BUILD_DIR%"
set "SOURCE_ARGS="
for %%S in (%SOURCE_FILES%) do (
    set "SOURCE_ARGS=!SOURCE_ARGS! \"%SRC_DIR%\%%S\""
)
if /i "!COMPILER_TYPE!"=="cl" (
    pushd "%BUILD_DIR%" >nul 2>&1
    echo [launcher] Building hero_line_wars with cl.exe...
    call "!COMPILER_CMD!" /nologo /std:c++17 /EHsc /O2 /I"%SRC_DIR%" !SOURCE_ARGS! /Fehero_line_wars.exe
    set "STATUS=%ERRORLEVEL%"
    popd >nul 2>&1
) else (
    echo [launcher] Building hero_line_wars with !COMPILER_BASENAME!... 
    "!COMPILER_CMD!" -std=c++17 -O2 -I"%SRC_DIR%" !SOURCE_ARGS! -o "%GAME_EXE%"
    set "STATUS=%ERRORLEVEL%"
)
if not "%STATUS%"=="0" (
    echo [launcher] Failed to build hero_line_wars.
    exit /b %STATUS%
)
exit /b 0
