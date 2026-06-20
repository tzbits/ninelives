# Nine Lives

Nine Lives is a JavaScript DSL for creating choice-based interactive fiction. 

It allows you to leverage your existing knowledge of browser JavaScript by providing a simple DSL for branching stories that transpiles into web-based browser games.

## Quick Example

Here is a snippet of Nine Lives code (`.9l`). 

Markdown-like paragraphs are treated as implicit template literals to be printed. 

Special leading characters are interpreted as the DSL. For example, explicit JavaScript is embedded with a leading '|'.

```9l
| let time = 0;
| function minutes() { return `minute${time > 1 ? "s" : ""}`; }

=0=

You've been standing in a dark room for ${++time} ${minutes()}.

There is an [oak door](>door) to your left.

>door Enter oak door.
>0 Stay where you are.

=door=

| const win = time < 4;

You enter the door and find yourself in ${win ? "a garden" : "a lava pool"}.

!gameover ${win ? "You Win!" : "You Lose!"}
```

## Getting Started

To start building and running Nine Lives stories, you'll need to set up your development environment. See detailed guides for:

*   [Install on Ubuntu LTS](doc/guide-ubuntu-lts-install-transpiler.md)
*   [Install with Homebrew (macOS/Linux)](doc/guide-homebrew-install-transpiler.md)

## Usage

Once you have the dependencies installed, you can try "The Cat" story included in the repository:

    $ bazel run //9l/cat:cat_local_server

Now visit `http://localhost:8080` and try the story.

### Hot Reloading

If you'd like the server to reload automatically when you change the source code:

    $ bazel run //9l/cat:cat_dev_server

### Releasing

To package a story for distribution:

    $ bazel build //9l/cat:cat_release

The output will be a zip file at `bazel-bin/9l/cat/cat_release.zip`.

You can upload this zip file to <http://itch.io> as an HTML project, or unzip it and deploy it to a web server.
