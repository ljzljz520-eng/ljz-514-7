#!/usr/bin/env bash
# 一键启动后端与前端（开发模式）
set -e
DIR="$(cd "$(dirname "$0")" && pwd)"

(cd "$DIR/backend" && mvn spring-boot:run) &
BACK_PID=$!
(cd "$DIR/frontend" && [ -d node_modules ] || npm install && npm run dev) &
FRONT_PID=$!

trap "kill $BACK_PID $FRONT_PID 2>/dev/null" EXIT
wait
