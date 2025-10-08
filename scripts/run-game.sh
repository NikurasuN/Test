#!/usr/bin/env bash
set -euo pipefail

PROJECT_ROOT="$(cd "$(dirname "${BASH_SOURCE[0]}")"/.. && pwd)"
BUILD_DIR="${BUILD_DIR:-$PROJECT_ROOT/build}"
EXECUTABLE_NAME="hero_line_wars"
EXECUTABLE_PATH="$BUILD_DIR/$EXECUTABLE_NAME"

if [[ ! -x "$EXECUTABLE_PATH" ]]; then
  cmake_args=(-S "$PROJECT_ROOT" -B "$BUILD_DIR")
  if [[ -n "${CMAKE_GENERATOR:-}" ]]; then
    cmake_args+=(-G "$CMAKE_GENERATOR")
  fi
  cmake "${cmake_args[@]}"
  cmake --build "$BUILD_DIR"
fi

exec "$EXECUTABLE_PATH" "$@"
