#!/usr/bin/env python3
"""Configure, build, and launch the Hero Line Wars duel."""

from __future__ import annotations

import argparse
import os
import shutil
import subprocess
import sys
from pathlib import Path


def project_root() -> Path:
    return Path(__file__).resolve().parent.parent


def parse_args() -> argparse.Namespace:
    parser = argparse.ArgumentParser(
        description="Configure (if necessary), build, and launch Hero Line Wars.",
        formatter_class=argparse.ArgumentDefaultsHelpFormatter,
    )
    parser.add_argument(
        "--build-dir",
        dest="build_dir",
        default=os.environ.get("BUILD_DIR"),
        help="CMake build directory to use or create.",
    )
    parser.add_argument(
        "--generator",
        dest="generator",
        default=os.environ.get("CMAKE_GENERATOR"),
        help="Explicit CMake generator to use during configuration.",
    )
    parser.add_argument(
        "--config",
        dest="config",
        default=os.environ.get("CMAKE_BUILD_TYPE"),
        help=(
            "Configuration to build/run (e.g. Debug, Release). Only used for "
            "multi-config generators such as Visual Studio, Xcode, or Ninja Multi-Config."
        ),
    )
    parser.add_argument(
        "--skip-build",
        dest="skip_build",
        action="store_true",
        help="Skip the build step and only run the executable.",
    )
    parser.add_argument(
        "args",
        nargs=argparse.REMAINDER,
        help="Arguments forwarded to the hero_line_wars executable (prefix with --).",
    )
    return parser.parse_args()


def ensure_cmake_available() -> None:
    if shutil.which("cmake") is None:
        sys.exit("cmake is required but was not found in PATH.")


def run_command(command: list[str], cwd: Path | None = None) -> None:
    print(f"[launcher] Running: {' '.join(command)}")
    subprocess.run(command, cwd=cwd, check=True)


def configure_project(root: Path, build_dir: Path, generator: str | None) -> None:
    cmake_command = ["cmake", "-S", str(root), "-B", str(build_dir)]
    if generator:
        cmake_command.extend(["-G", generator])
    run_command(cmake_command)


def build_project(build_dir: Path, config: str | None) -> None:
    command = ["cmake", "--build", str(build_dir)]
    if config:
        command.extend(["--config", config])
    run_command(command)


def candidate_configurations(config: str | None) -> list[str]:
    if config:
        return [config]
    # Common default configurations to probe when none is specified.
    return ["", "Debug", "Release", "RelWithDebInfo", "MinSizeRel"]


def executable_candidates(build_dir: Path, config: str | None) -> list[Path]:
    names = ["hero_line_wars", "hero_line_wars.exe"]
    candidates: list[Path] = []
    for cfg in candidate_configurations(config):
        cfg_dir = build_dir if not cfg else build_dir / cfg
        for name in names:
            candidate = cfg_dir / name
            candidates.append(candidate)
    return candidates


def locate_executable(build_dir: Path, config: str | None) -> Path:
    for candidate in executable_candidates(build_dir, config):
        if candidate.exists() and candidate.is_file() and os.access(candidate, os.X_OK):
            return candidate
    raise FileNotFoundError(
        "Unable to locate the hero_line_wars executable. "
        "Run the build first or specify --config if you used a multi-config generator."
    )


def strip_arg_separator(args: list[str]) -> list[str]:
    if args and args[0] == "--":
        return args[1:]
    return args


def launch_game(executable: Path, args: list[str]) -> int:
    command = [str(executable), *args]
    print(f"[launcher] Starting {' '.join(command)}")
    process = subprocess.run(command, cwd=str(executable.parent))
    return process.returncode


def main() -> int:
    args = parse_args()
    ensure_cmake_available()

    root = project_root()
    build_dir = Path(args.build_dir) if args.build_dir else root / "build"
    build_dir.mkdir(parents=True, exist_ok=True)

    if not args.skip_build:
        configure_project(root, build_dir, args.generator)
        build_project(build_dir, args.config)

    try:
        executable = locate_executable(build_dir, args.config)
    except FileNotFoundError as exc:
        sys.exit(str(exc))

    forwarded_args = strip_arg_separator(args.args)
    return launch_game(executable, forwarded_args)


if __name__ == "__main__":
    sys.exit(main())
