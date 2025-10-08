# Hero Line Wars (C++ Arena)

This repository now contains a self-contained C++ interpretation of the classic Hero Line Wars skirmish. The original Swing UI
has been replaced with an interactive terminal duel where you recruit units, equip items, and march down the lane wave after wa
ve.

## Gameplay overview

- Choose between three hero archetypes (Ranger, Berserker, or Battle Mage), each with distinct primary attributes.
- Recruit units before every wave to increase your income and tilt the battle in your favour.
- Purchase equipment to specialise your hero and dominate the front line.
- Win waves to damage the opposing base—lose and your own fortifications crumble.

## Building and running

The project uses CMake and targets C++17.

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
