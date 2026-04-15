#!/bin/sh
set -eu

REPO_DIR=$(CDPATH= cd -- "$(dirname -- "$0")" && pwd)
JDK17_HOME="/opt/homebrew/opt/openjdk@17/libexec/openjdk.jdk/Contents/Home"

if [ ! -d "$JDK17_HOME" ]; then
  echo "JDK 17 not found at: $JDK17_HOME" >&2
  echo "Install openjdk@17 or update mvnw to point at your JDK 17 home." >&2
  exit 1
fi

export JAVA_HOME="$JDK17_HOME"
export PATH="$JAVA_HOME/bin:$PATH"

exec mvn "$@"
