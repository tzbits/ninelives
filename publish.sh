#!/bin/bash
set -euo pipefail

DEPLOY_TARGET="${DEPLOY_TARGET:-${HOME}/public_html}"

# Parse command-line arguments passed by Bazel.
RSYNC_TOOL="$1"
shift
SRC_FILES="$*"

if [[ -z "$RSYNC_TOOL" || -z "$DEPLOY_TARGET" || -z "$SRC_FILES" ]]; then
  echo "Usage: publsh.sh {rsync_tool} {dest_dir} {src_file*}"
  exit 1
fi

echo "Running publish.sh from:";
echo "  $(pwd)";
echo "RSYNC_TOOL: $RSYNC_TOOL";
echo "DEPLOY_TARGET: $DEPLOY_TARGET";
echo "SRC_FILES: ";
echo "  $SRC_FILES";

# --copy-links (-L) flag to dereference symlinks
# -rltgoDv is -a without -p
"$RSYNC_TOOL" -azv --relative --mkpath --chmod=D755,F644 -L $SRC_FILES "${DEPLOY_TARGET}/."
echo "Rsync completed successfully."
