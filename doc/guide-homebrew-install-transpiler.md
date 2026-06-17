# Install the Nine Lives compiler (homebrew)

## Overview

Nine Lives is a Javascript DSL for creating choice-based interactive fiction. Before you can start crafting your own stories, you need to set up the development environment on your local machine.

In this guide, we will walk through the process of installing the necessary dependencies and verifying that you can build and run Nine Lives stories.

### What you'll learn

- How to install the Nine Lives dependencies
- How to verify the Bazel build rules
- How to run a local story server

### What you'll need

- A computer running macOS or Linux with [Homebrew](https://brew.sh/) installed
- Basic familiarity with the command line
- `git` installed to clone the repository (Homebrew provides this)

## Install dependencies

Duration: 2:00

tldr;

    brew install --cask temurin@21
    brew install bazelisk entr python

To build and run Nine Lives, we need a few specific tools. These instructions assume you have [Homebrew](https://brew.sh/) installed.

First, check if you already have Java 21 or later installed:

    java -version

If you have version 21 or later, you can skip the JDK installation. Otherwise, install the Java Development Kit (JDK) version 21. We recommend the Temurin distribution:

    brew install --cask temurin@21

Next, check if you have a recent version of Python installed:

    python3 --version

If you have version 3.12 or later, you can skip the Python installation. Otherwise, install it:

    brew install python

For Bazel, we recommend using **Bazelisk**, which automatically manages Bazel versions for you. You can install it easily via Homebrew:

    brew install bazelisk

Finally, check if you have `entr` installed. This tool is useful for automatically reloading the server when you change the source code:

    entr -version

If `entr` is not installed, you can install it via Homebrew:

    brew install entr

## Clone the repository

Duration: 1:00

Now, we need to get the source code. Clone the Nine Lives repository from GitHub:

    git clone https://github.com/tzbits/ninelives.git

Change directory to the root of the repository:

    cd ninelives

> ⓘ **Stay in the root!**
> Most Bazel commands should be run from the root of the repository.

## Verify build rules

Duration: 1:00

Once inside the repository, we can use Bazel to inspect the available build rules. This confirms that Bazel is correctly configured and can see the project structure.

Run the following command to list all targets:

    bazel query //...

You should see a list of targets starting with `//9l/...`, `//java/...`, and others. This indicates that the `ninelives_story` build rules are working correctly.

## Run a story locally

Duration: 5:00

The ultimate test of our installation is running a story server. We'll use "Cloak of Darkness" (`//9l/cloak`) as our test case.

Run the local server with the following command:

    bazel run //9l/cloak:cloak_local_server

You should see output similar to:

    Serving HTTP on port 8080

Now, open your web browser and visit `http://localhost:8080`. You should be able to play the "Cloak of Darkness" story!

To stop the server, press `Ctrl+C` in your terminal.

## That's all folks!

Congratulations! You have successfully set up the Nine Lives compiler environment and run your first story locally.

In this guide, we have:
- Installed JDK 21, Python, Bazelisk, and `entr` using Homebrew.
- Verified the project structure using `bazel query`.
- Launched a local story server using `bazel run`.

### Next steps

* Check out the [Tutorial: Create your first story](tutorial-create-your-first-story.md) to start writing your own interactive fiction.
* Explore the `9l/` directory to see how stories are structured.