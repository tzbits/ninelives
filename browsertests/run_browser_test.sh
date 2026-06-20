#!/bin/bash
test_path=${1}
#echo "http://127.0.0.0:8080/browsertests/run_test.htm?path=${test_path}"
/usr/bin/google-chrome \
  --headless \
  --dump-dom \
  --virtual-time-budget=5000 \
  "http://127.0.0.0:8080/browsertests/run_test.htm?path=${test_path}" 2>/dev/null | grep -F "[CONSOLE]" | grep -v -F "ignore for run_browser_test.sh"