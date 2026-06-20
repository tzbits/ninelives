Below is the mechanism for running tests on the command line that run in chrome browser.

The server is already running via `browsertests/serve_tests.sh`. You do ***not*** need to start it.

See an example at:

    /java/com/tzbits/ninelives/browsertests/TestCanLog.js

Run a test with:

    bowsertests/run_browser_test.sh /java/com/tzbits/ninelives/browsertests/TestCanLog.js

Do not run `browsertests/run_all_tests.sh`, it will be run later in subsquent steps. Please update `browsertests/run_all_tests.sh` though, if adding new tests.
