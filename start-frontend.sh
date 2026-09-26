#!/usr/bin/env bash
# 启动前端开发服务器
set -e
cd "$(dirname "$0")/frontend"
[ -d node_modules ] || npm install
exec npm run dev
