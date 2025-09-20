# Nine Lives

Nine lives is a Javascript dsl for creating choice-based interactive fiction.

# Usage

Install bazel https://bazel.build/install

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
