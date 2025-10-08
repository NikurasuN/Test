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

On Windows you can run the helper batch script from Command Prompt:

```cmd
scripts\run-game.cmd
```

The script configures (if needed) and builds the project before starting `hero_line_wars.exe`. To use a different build directory, pass `--build-dir <path>` or set the `BUILD_DIR` environment variable.

After building with CMake, a Windows-native launcher (`run_game.exe`) is generated in the build output. Double-click it (or run it from the terminal) to locate `hero_line_wars.exe` in the build tree and start the duel without scripting.

```bash
cmake -S . -B build/release -DCMAKE_BUILD_TYPE=Release
cmake --build build/release --config Release
./build/release/hero_line_wars
```

## Windows executable helper

If you need to create a redistributable folder, configure a Release build with CMake and copy the resulting executables from the build output (including `run_game.exe` and `hero_line_wars.exe`) into your desired directory.
