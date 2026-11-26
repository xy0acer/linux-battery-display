#!/bin/sh
# Small Linux launcher for the official Gradle wrapper JAR.
set -eu
PROJECT_DIR=$(CDPATH= cd -- "$(dirname -- "$0")" && pwd)

# The system default may be Java 8. Prefer the installed Java 25 JDK.
if [ -z "${JAVA_HOME:-}" ] && [ -x /usr/lib/jvm/java-25-openjdk/bin/java ]; then
    JAVA_HOME=/usr/lib/jvm/java-25-openjdk
    export JAVA_HOME
fi
JAVA_CMD=${JAVA_HOME:+$JAVA_HOME/bin/}java
if ! command -v "$JAVA_CMD" >/dev/null 2>&1; then
    printf '%s\n' 'Java 25 JDK is required. Set JAVA_HOME to its installation directory.' >&2
    exit 1
fi

# All build caches and temporary files stay inside this workspace.
GRADLE_USER_HOME="$PROJECT_DIR/.gradle-user-home"
export GRADLE_USER_HOME
mkdir -p "$PROJECT_DIR/.tmp"
exec "$JAVA_CMD" "-Djava.io.tmpdir=$PROJECT_DIR/.tmp" \
    -classpath "$PROJECT_DIR/gradle/wrapper/gradle-wrapper.jar" \
    org.gradle.wrapper.GradleWrapperMain -p "$PROJECT_DIR" "$@"
