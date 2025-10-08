# Hero Line Wars (C++ Arena)

This repository now contains a self-contained C++ interpretation of the classic Hero Line Wars skirmish. The original Swing UI
has been replaced with an interactive terminal duel where you recruit units, equip items, and march down the lane wave after wa
ve.

## Gameplay overview

- Choose between three hero archetypes (Ranger, Berserker, or Battle Mage), each with distinct primary attributes.
- Recruit units before every wave to increase your income and tilt the battle in your favour.
- Purchase equipment to specialise your hero and dominate the front line.
- Win waves to damage the opposing base—lose and your own fortifications crumble.

## Prerequisites

Before building the project you need the following tools available on your
`PATH`:

- **CMake** 3.16 or newer.
- A C++17 capable compiler toolchain (e.g. Visual Studio Build Tools on
  Windows, or GCC/Clang on Linux/macOS).

> **Windows note:** If you see an error such as `cmake : Die Benennung "cmake"
> wurde nicht als Name ... erkannt`, PowerShell cannot find the CMake
> executable. Install CMake from [cmake.org](https://cmake.org/download/), or
> launch a *Developer PowerShell for Visual Studio* where CMake is already on
> the `PATH`.

## Building and running

### Windows (.NET single-file build)

> **Prerequisite:** Install the [.NET 6 SDK](https://dotnet.microsoft.com/en-us/download) if the `dotnet` command is not already available on your `PATH`. You can verify the installation with `dotnet --version` from a new terminal session.

The repository ships with a C# console port that can be published as a standalone Windows executable. From any machine with the .NET 6 SDK installed, run:

```bash
dotnet publish dotnet/HeroLineWars/HeroLineWars.csproj \
  -c Release -r win-x64 --self-contained true /p:PublishSingleFile=true
```

> **PowerShell note:** PowerShell does not support the Unix-style `\` line continuation shown above.
> Either run the command in a single line:
>
> ```powershell
> dotnet publish dotnet/HeroLineWars/HeroLineWars.csproj -c Release -r win-x64 --self-contained true /p:PublishSingleFile=true
> ```
>
> …or replace the trailing backslash with PowerShell's `` ` `` continuation character.

The resulting `HeroLineWars.exe` (found under `dotnet/HeroLineWars/bin/Release/net6.0/win-x64/publish/`) bundles the .NET runtime, so you can copy it to a fresh Windows installation and launch the game by double-clicking the executable.

### macOS/Linux (from source)

The original C++17 source is still available if you prefer to compile the terminal edition yourself.

```bash
# Configure and build (Debug by default)
cmake -S . -B build
cmake --build build

# Run the duel (macOS/Linux)
./build/hero_line_wars
```

```bash
cmake -S . -B build/release -DCMAKE_BUILD_TYPE=Release
cmake --build build/release --config Release
./build/release/hero_line_wars
```

## Creating a Release build

The commands above demonstrate how to configure and build an optimised release version of the game. Once the build completes, run the resulting `hero_line_wars` binary from the appropriate build directory for your platform.
