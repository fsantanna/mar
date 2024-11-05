#!/usr/bin/env sh
exec java -Xmx500M -Xms500M -ea -cp "$(dirname "$0")"/mar.jar mar.MainKt "$@"
