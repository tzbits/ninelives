#!/bin/bash
# local_dev_server_bin.sh <entr_path> <local_server_target> <files_to_watch...>

ENTR_BIN="$1"
LOCAL_SERVER_TARGET="$2"
shift 2
FILES_TO_WATCH="$@"

if [ -z "$ENTR_BIN" ] || [ -z "$LOCAL_SERVER_TARGET" ]; then
  echo "Usage: $0 <entr_path> <local_server_target> <files_to_watch...>"
  exit 1
fi

# If ENTR_BIN is a relative path, we should try to find it relative to the original
# directory if we are going to cd to BUILD_WORKSPACE_DIRECTORY.
# However, entr itself will be executed BEFORE or AFTER cd?
# In the current script, it is executed AFTER cd.
# So we need the absolute path of ENTR_BIN.

# Get the absolute path of the entr binary before we cd
if [[ ! "$ENTR_BIN" =~ ^/ ]]; then
  ENTR_BIN="$(pwd)/$ENTR_BIN"
fi

# We use -n for non-interactive mode and -r to restart the process.
echo "Watching files: $FILES_TO_WATCH"

# Bazel run sets BUILD_WORKSPACE_DIRECTORY to the root of the workspace.
# We must cd there because bazel cannot be run from within the bazel-out directory.
if [ -n "$BUILD_WORKSPACE_DIRECTORY" ]; then
  cd "$BUILD_WORKSPACE_DIRECTORY"
fi

echo "$FILES_TO_WATCH" | tr ' ' '\n' | "$ENTR_BIN" -n -r bazel run -- "$LOCAL_SERVER_TARGET"
