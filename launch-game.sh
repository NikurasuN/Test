#!/usr/bin/env bash
set -euo pipefail

SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
BUILD_DIR="$SCRIPT_DIR/.launcher-build"
GAME_BIN="$BUILD_DIR/hero_line_wars"
SRC_DIR="$SCRIPT_DIR/src"

SOURCES=(
  "main.cpp"
  "HeroLineWarsGame.cpp"
  "Hero.cpp"
  "IconLibrary.cpp"
  "Item.cpp"
  "Team.cpp"
  "UnitType.cpp"
)

choose_compiler() {
  if command -v g++ >/dev/null 2>&1; then
    echo "g++"
    return 0
  fi
  if command -v clang++ >/dev/null 2>&1; then
    echo "clang++"
    return 0
  fi
  return 1
}

CXX="$(choose_compiler || true)"
if [[ -z "$CXX" ]]; then
  echo "Error: Unable to find a C++17 compiler (g++ or clang++) on PATH." >&2
  exit 1
fi

needs_rebuild=0
if [[ ! -x "$GAME_BIN" ]]; then
  needs_rebuild=1
else
  for source in "${SOURCES[@]}"; do
    if [[ "$SRC_DIR/$source" -nt "$GAME_BIN" ]]; then
      needs_rebuild=1
      break
    fi
  done
fi

if [[ "$needs_rebuild" -eq 1 ]]; then
  mkdir -p "$BUILD_DIR"
  echo "[launcher] Building hero_line_wars with $CXX..."
  "$CXX" -std=c++17 -O2 -pipe -I"$SRC_DIR" \
    "${SOURCES[@]/#/$SRC_DIR/}" \
    -o "$GAME_BIN"
  echo "[launcher] Build finished."
fi

echo "[launcher] Starting Hero Line Wars..."
cd "$BUILD_DIR"
exec "$GAME_BIN" "$@"
