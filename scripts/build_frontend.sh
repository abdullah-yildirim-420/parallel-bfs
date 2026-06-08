#!/usr/bin/env bash
# Build the React SPA and copy it into the Java resources so the fat jar serves it
# at http://localhost:7070 (design.md §16.3, §24).
set -euo pipefail
HERE="$(cd "$(dirname "$0")/.." && pwd)"
cd "$HERE/frontend"
npm install --no-fund --no-audit
npm run build
DEST="$HERE/src/main/resources/public"
rm -rf "$DEST"
mkdir -p "$DEST"
cp -r dist/* "$DEST/"
echo "SPA copied to $DEST — rebuild the jar with: mvn -DskipTests package"
