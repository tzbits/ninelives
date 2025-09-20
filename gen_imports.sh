#!/bin/bash
set -euo pipefail

JS_FILES="$*"

for f in $JS_FILES; do
  echo -e "import \"$f\"\n" > story-imports.js
done