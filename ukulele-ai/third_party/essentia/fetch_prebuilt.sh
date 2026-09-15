#!/usr/bin/env bash
set -euo pipefail
ROOT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")/../.." && pwd)"
OUT_DIR="${ROOT_DIR}/third_party/essentia/prebuilt/active"
WORK_DIR="${ROOT_DIR}/third_party/essentia/.work/rn-essentia-static"
REPO_URL="https://github.com/deeeed/rn-essentia-static.git"
REPO_REF="476d5cfa763ad8950bf91f876788e7d6739fdecc"
ABIS=(arm64-v8a x86_64)
rm -rf "$WORK_DIR" "$OUT_DIR"
mkdir -p "$(dirname "$WORK_DIR")" "$OUT_DIR"
git -c init.defaultBranch=main init "$WORK_DIR" >/dev/null
git -C "$WORK_DIR" remote add origin "$REPO_URL"
git -C "$WORK_DIR" fetch --depth 1 origin "$REPO_REF"
git -C "$WORK_DIR" checkout --detach FETCH_HEAD >/dev/null
for abi in "${ABIS[@]}"; do
  mkdir -p "$OUT_DIR/$abi"
  cp "$WORK_DIR/android/jniLibs/$abi/libessentia.a" "$OUT_DIR/$abi/libessentia.a"
done
mkdir -p "$OUT_DIR/include"
cp -R "$WORK_DIR/cpp/include/essentia" "$OUT_DIR/include/"
printf 'repo=%s\nref=%s\ncommit=%s\n' "$REPO_URL" "$REPO_REF" "$(git -C "$WORK_DIR" rev-parse HEAD)" > "$OUT_DIR/SOURCE.txt"
