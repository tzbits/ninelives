# Nine Lives

Nine lives is a Javascript dsl for creating choice-based interactive fiction.

# Dependencies

Here are the specific versions known to work. They are based on Ubuntu 24.04 LTS defaults.

bazel 8.3.1 - https://bazel.build/install

  * Use [bazelisk](https://github.com/bazelbuild/bazelisk/releases) to install on Ubuntu.

Java [JDK 21](https://openjdk.org/projects/jdk/21/)

  * `sudo apt install openjdk-21-jdk` on Ubuntu.

Python 3.12.3

# Usage

To try "The Cat" cd into the repository and run,

    $ bazel run //9l/cat:cat_local_server
    Serving HTTP on port 8080

Now visit `htpp://localhost:8080` and try the story.

To release the project into a tar file:

    $ bazel build 9l/cat:release
    Target //9l/cat:cat_release up-to-date:
      bazel-bin/9l/cat/cat_release.tar

# TODO

Ways to help

* Document the dsl
* consider adding a less cryptic conditional choice format:
  * >some-node-id !if isThisTrue(foo) !then "go happily"
* support line continuation with \\\n
* Hook up JsExpr
* redo inventory: make it something you import separately
  * e.g. pair down game.js to the bare minimum
  * provide other modules that can be imported into story.js
