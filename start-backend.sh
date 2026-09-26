#!/usr/bin/env bash
# 启动后端（自动定位 JDK）
set -e
cd "$(dirname "$0")/backend"
if [ -z "$JAVA_HOME" ] && [ -d /home/node/tools/jdk17 ]; then
  export JAVA_HOME=/home/node/tools/jdk17
  export PATH="$JAVA_HOME/bin:$PATH"
fi
if [ ! -f target/visitor-route-system-1.0.0.jar ]; then
  MVN=$(command -v mvn || echo /home/node/tools/maven/bin/mvn)
  "$MVN" -q package -DskipTests
fi
exec java -jar target/visitor-route-system-1.0.0.jar
