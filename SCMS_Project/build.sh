#!/bin/bash
# ============================================================
#  SCMS Build & Run Script  (Linux / macOS)
#  Requires: JDK 17+
# ============================================================
set -e

PROJECT_ROOT="$(cd "$(dirname "$0")" && pwd)"
SRC="$PROJECT_ROOT/src/main/java"
OUT="$PROJECT_ROOT/out"

echo "=============================================="
echo " Smart Campus Management System – Build Tool "
echo "=============================================="

# Verify javac
if ! command -v javac &>/dev/null; then
  echo "ERROR: javac not found. Install JDK 17+ and add it to PATH."
  exit 1
fi

JAVA_VER=$(javac -version 2>&1 | awk '{print $2}' | cut -d'.' -f1)
echo "Detected Java version: $JAVA_VER"

mkdir -p "$OUT"

echo ""
echo "==> Compiling sources..."
find "$SRC" -name "*.java" | xargs javac --release 17 -d "$OUT"
echo "    Compilation successful."

echo ""
echo "==> Running standalone test suite..."
java -cp "$OUT" scms.test.TestRunner

echo ""
echo "==> Creating runnable JAR..."
cd "$OUT"
echo "Main-Class: scms.Main" > manifest.txt
jar cfm "$PROJECT_ROOT/SCMS.jar" manifest.txt .
rm manifest.txt
echo "    Created: SCMS.jar"

echo ""
echo "==> Launching application..."
echo "    (Press Ctrl+C to exit at any time)"
echo ""
java -cp "$OUT" scms.Main
