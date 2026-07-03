#!/bin/sh
set -eu

BASE_DIR="$(CDPATH= cd -- "$(dirname -- "$0")" && pwd)"
WRAPPER_DIR="$BASE_DIR/.mvn/wrapper"
MAVEN_VERSION="3.9.11"
DIST_NAME="apache-maven-$MAVEN_VERSION"
DIST_DIR="$WRAPPER_DIR/$DIST_NAME"
ARCHIVE="$WRAPPER_DIR/$DIST_NAME-bin.tar.gz"
URL="https://archive.apache.org/dist/maven/maven-3/$MAVEN_VERSION/binaries/$DIST_NAME-bin.tar.gz"
LOCAL_JDK_DIR="$(find "$BASE_DIR/.tools" -maxdepth 1 -type d -name 'jdk-*' 2>/dev/null | head -n 1 || true)"

if [ -n "$LOCAL_JDK_DIR" ]; then
  JAVA_HOME="$LOCAL_JDK_DIR"
  export JAVA_HOME
  export PATH="$JAVA_HOME/bin:$PATH"
fi

if [ ! -x "$DIST_DIR/bin/mvn" ]; then
  mkdir -p "$WRAPPER_DIR"
  if [ ! -f "$ARCHIVE" ]; then
    curl -fsSL "$URL" -o "$ARCHIVE"
  fi
  tar -xzf "$ARCHIVE" -C "$WRAPPER_DIR"
fi

exec "$DIST_DIR/bin/mvn" "$@"
