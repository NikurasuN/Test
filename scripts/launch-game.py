#!/usr/bin/env python3
"""Build and launch Hero Line Wars without relying on CMake.

This helper script is intended for players who simply want to compile and
run the duel without setting up an IDE or invoking CMake manually. It tries
to locate a suitable C++17 compiler, performs a direct one-shot build of the
engine, and finally executes the freshly built binary.

Examples
--------
    # Build into the default ./build/local directory and start the game
    python scripts/launch-game.py

    # Pass arguments through to the game (after a -- separator)
    python scripts/launch-game.py -- --help

    # Rebuild from scratch into a custom directory
    python scripts/launch-game.py --build-dir out/game --clean
"""

from __future__ import annotations

import argparse
import os
import shutil
import subprocess
import sys
from pathlib import Path
from typing import Iterable, List, Sequence

REPO_ROOT = Path(__file__).resolve().parents[1]
SOURCE_DIR = REPO_ROOT / "src"
DEFAULT_BUILD_DIR = REPO_ROOT / "build" / "local"
GAME_NAME = "hero_line_wars.exe" if os.name == "nt" else "hero_line_wars"

# List of source files that make up the game target. Keep this in sync with
# CMakeLists.txt so that both build paths produce the same executable.
GAME_SOURCES = [
    "main.cpp",
    "HeroLineWarsGame.cpp",
    "Hero.cpp",
    "IconLibrary.cpp",
    "Item.cpp",
    "Team.cpp",
    "UnitType.cpp",
]


class LaunchError(RuntimeError):
    """Raised when the launcher cannot fulfil a request."""


def parse_arguments(argv: Sequence[str]) -> argparse.Namespace:
    parser = argparse.ArgumentParser(description=__doc__)
    parser.add_argument(
        "--build-dir",
        type=Path,
        default=DEFAULT_BUILD_DIR,
        help="Directory where the executable will be produced (default: %(default)s)",
    )
    parser.add_argument(
        "--compiler",
        help="Explicit compiler executable to use (overrides auto-detection)",
    )
    parser.add_argument(
        "--skip-build",
        action="store_true",
        help="Assume the binary already exists and only launch the game",
    )
    parser.add_argument(
        "--build-only",
        action="store_true",
        help="Compile the executable but do not launch it",
    )
    parser.add_argument(
        "--clean",
        action="store_true",
        help="Remove the build directory before compiling",
    )
    parser.add_argument(
        "game_args",
        nargs=argparse.REMAINDER,
        help="Arguments passed straight to the game after the -- separator",
    )
    return parser.parse_args(argv)


def normalise_game_args(args: Sequence[str]) -> List[str]:
    """Strip a leading separator that argparse leaves behind."""

    if args and args[0] == "--":
        return list(args[1:])
    return list(args)


def ensure_directory(path: Path) -> None:
    path.mkdir(parents=True, exist_ok=True)


def clean_directory(path: Path) -> None:
    if path.exists():
        shutil.rmtree(path)


def detect_compiler(explicit: str | None) -> str:
    """Return the compiler executable that should be used."""

    if explicit:
        return explicit

    candidates: Iterable[str]
    if os.name == "nt":
        # On Windows we first try the Visual C++ compiler (cl), then fall back to
        # MinGW or LLVM installations that ship a g++-compatible driver.
        candidates = ("cl", "clang++", "g++")
    else:
        candidates = ("g++", "clang++")

    for candidate in candidates:
        if shutil.which(candidate):
            return candidate

    raise LaunchError(
        "No suitable C++ compiler was found. Install g++, clang++, or build from an "
        "IDE before running this script."
    )


def build_command(compiler: str, output: Path) -> List[str]:
    """Construct the command used to compile the game."""

    source_paths = [str(SOURCE_DIR / file) for file in GAME_SOURCES]

    if compiler.lower() == "cl":
        command = [
            "cl",
            "/std:c++17",
            "/EHsc",
            "/nologo",
            f"/Fe{output.name}",
        ]
        command.extend(["/I", str(SOURCE_DIR)])
        command.extend(source_paths)
        return command

    # The fallback branch targets compilers with a g++-style interface.
    command = [
        compiler,
        "-std=c++17",
        "-O2",
        "-Wall",
        "-Wextra",
        "-pedantic",
        "-o",
        str(output),
    ]
    command.extend(source_paths)
    command.extend(["-I", str(SOURCE_DIR)])
    return command


def run_build(compiler: str, build_dir: Path) -> Path:
    ensure_directory(build_dir)
    executable = build_dir / GAME_NAME

    if compiler.lower() == "cl":
        # MSVC writes output files into the working directory.
        command = build_command(compiler, executable)
        result = subprocess.run(command, cwd=build_dir)
    else:
        command = build_command(compiler, executable)
        result = subprocess.run(command)

    if result.returncode != 0:
        raise LaunchError(
            f"Compilation failed with exit code {result.returncode}. Check the compiler "
            "output above for details."
        )

    if not executable.exists():
        raise LaunchError(
            f"Expected executable {executable} was not produced. Ensure your compiler "
            "supports C++17 and retry."
        )

    return executable


def launch_game(executable: Path, args: Sequence[str]) -> int:
    command = [str(executable)] + list(args)
    process = subprocess.run(command)
    return process.returncode


def main(argv: Sequence[str] | None = None) -> int:
    options = parse_arguments(argv or sys.argv[1:])
    game_args = normalise_game_args(options.game_args)
    build_dir = options.build_dir
    if not build_dir.is_absolute():
        build_dir = REPO_ROOT / build_dir

    if options.clean and build_dir.exists():
        print(f"[launcher] Removing {build_dir}")
        clean_directory(build_dir)

    executable = build_dir / GAME_NAME

    if not options.skip_build:
        compiler = detect_compiler(options.compiler)
        print(f"[launcher] Using compiler: {compiler}")
        print(f"[launcher] Building into: {build_dir}")
        executable = run_build(compiler, build_dir)
    else:
        if not executable.exists():
            raise LaunchError(
                f"--skip-build was requested but {executable} does not exist. Run the script "
                "without --skip-build first."
            )

    if options.build_only:
        print("[launcher] Build completed. Skipping execution as requested.")
        return 0

    print(f"[launcher] Launching {executable}")
    return launch_game(executable, game_args)


if __name__ == "__main__":
    try:
        sys.exit(main())
    except LaunchError as error:
        print(f"Launcher error: {error}", file=sys.stderr)
        sys.exit(1)
    except KeyboardInterrupt:
        print("\nLaunch aborted by user.")
        sys.exit(130)
