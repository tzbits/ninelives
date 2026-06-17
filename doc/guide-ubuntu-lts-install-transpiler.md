# Install the Nine Lives compiler (Ubuntu LTS)

## Overview

Nine Lives is a Javascript DSL for creating choice-based interactive fiction. Before you can start crafting your own stories, you need to set up the development environment on your local machine.

In this guide, we will walk through the process of installing the necessary dependencies and verifying that you can build and run Nine Lives stories.

### What you'll learn

- How to install the Nine Lives dependencies
- How to verify the Bazel build rules
- How to run a local story server

### What you'll need

- A computer running Ubuntu 26.04 LTS (recommended) or a compatible Linux distribution
- Basic familiarity with the command line
- `git` installed to clone the repository

## Install dependencies

Duration: 1:00

tldr;

    sudo apt install openjdk-21-jdk python3 python-is-python3 entr
    # Install bazelisk manually (see below)

To build and run Nine Lives, we need a few specific tools. These instructions are based on Ubuntu 26.04 LTS defaults.

First, check if you already have Java 21 or later installed:

    java -version

If you have version 21 or later, you can skip the JDK installation. Otherwise, install the Java Development Kit (JDK) version 21:

    sudo apt install openjdk-21-jdk

Next, check if you have a recent version of Python installed:

    python --version

If you have version 3.12 or later, you can skip the Python installation. Otherwise, install it:

    sudo apt install python3 python-is-python3

For Bazel, we recommend using **Bazelisk**, which automatically manages Bazel versions for you. Download the latest release from the [Bazelisk GitHub page](https://github.com/bazelbuild/bazelisk/releases) and make it executable in your path:

    # Example for downloading and installing bazelisk
    mkdir -p ~/.local/bin
    curl -L https://github.com/bazelbuild/bazelisk/releases/latest/download/bazelisk-linux-amd64 -o ~/.local/bin/bazelisk
    chmod +x ~/.local/bin/bazelisk
    source ~/.bashrc  # in case you didn't already have ~/.local/bin

Finally, check if you have `entr` installed. This tool is useful for automatically reloading the server when you change the source code:

    entr -version

If `entr` is not installed, you can install it using `apt`:

    sudo apt install entr

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
- Installed JDK 21, Python 3.13, Bazelisk, and `entr`.
- Verified the project structure using `bazel query`.
- Launched a local story server using `bazel run`.

### Next steps

* Check out the [Tutorial: Create your first story](tutorial-create-your-first-story.md) to start writing your own interactive fiction.
* Explore the `9l/` directory to see how stories are structured.