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

The project uses CMake and targets C++17.

```bash
# Configure and build (Debug by default)
cmake -S . -B build
cmake --build build

# Run the duel
./build/hero_line_wars
```

To create a release build:

```bash
cmake -S . -B build/release -DCMAKE_BUILD_TYPE=Release
cmake --build build/release --config Release
./build/release/hero_line_wars
```

## Windows executable helper

A PowerShell helper script is available under `scripts/build-windows-exe.ps1`. It configures a Release build with CMake and copy
s the resulting executable into `dist\windows`.
