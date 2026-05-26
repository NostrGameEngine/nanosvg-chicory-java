#!/usr/bin/env bash
set -euo pipefail

ROOT_DIR="$(cd "$(dirname "$0")/.." && pwd)"
cd "$ROOT_DIR"

if [ ! -d third_party/nanosvg/src ]; then
  echo "third_party/nanosvg/src is missing"
  exit 1
fi

./gradlew clean build
