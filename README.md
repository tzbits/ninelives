# Nine Lives

Nine lives is a Javascript dsl for creating choice-based interactive fiction.

# Dependencies

Here are the specific versions known to work. They are based on Ubuntu 24.04 LTS defaults.

bazel 9.1.0 - https://bazel.build/install

  * Use [bazelisk](https://github.com/bazelbuild/bazelisk/releases) to install on Ubuntu.

Java [JDK 21](https://openjdk.org/projects/jdk/21/)

  * `sudo apt install openjdk-21-jdk` on Ubuntu.

Python 3.13

# Usage

To try "The Cat" cd into the repository and run,

    $ bazel run //9l/cat:cat_local_server
    Serving HTTP on port 8080

Now visit `http://localhost:8080` and try the story.

To release the project into a tar file:

    $ bazel build 9l/cat:release
    Target //9l/cat:cat_release up-to-date:
      bazel-bin/9l/cat/cat_release.tar

# TODO

* we need reconcile `story` state with `game.state`

* ! defaults to adding to adding the css class to the following text
  * we should make that an explicit command:

        !css funky Now this senetence is funky looking.

  * actually, we should make it so you can plug in custom functions for ! in JavaScript
* make a better way to list vs wrap choices
* consider adding a less cryptic conditional choice format:
  * >some-node-id !if isThisTrue(foo) !then "go happily"
* support line continuation with \\\n
* redo inventory: make it something you import separately
  * e.g. pair down game.js to the bare minimum
  * provide other modules that can be imported into story.js
* Sticky vs once-only choices
  * sticky is the default, add once:
  * >examineshoe !once Look closely at the shoe
* alt-sequences: array of strings that get cycled through on each visit
  * !seq sticky: settles on the final one when reached.
  * !seq cycle: loops around (seq length modulo visit count)
  * !seq once: display nothing after going past end.
  * allow empty, allow nesting
* !shuffle: random output from a sequence of strings
