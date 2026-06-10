# Install the Nine Lives compiler

## Overview

Duration: 2:00

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

Duration: 5:00

To build and run Nine Lives, we need a few specific tools. These instructions are based on Ubuntu 26.04 LTS defaults.

First, let's install the Java Development Kit (JDK) version 21:

    sudo apt install openjdk-21-jdk

Next, ensure you have a recent version of python installed:

    python3 --version

Python 3.12 or later is recommended.

For Bazel, we recommend using **Bazelisk**, which automatically manages Bazel versions for you. Download the latest release from the [Bazelisk GitHub page](https://github.com/bazelbuild/bazelisk/releases) and make it executable in your path:

    # Example for downloading and installing bazelisk
    sudo curl -L https://github.com/bazelbuild/bazelisk/releases/latest/download/bazelisk-linux-amd64 -o /usr/local/bin/bazelisk
    sudo chmod +x /usr/local/bin/bazelisk
    sudo ln -s /usr/local/bin/bazelisk /usr/local/bin/bazel

## Clone the repository

Duration: 2:00

Now, we need to get the source code. Clone the Nine Lives repository from GitHub:

    git clone https://github.com/tzbits/ninelives.git

Change directory to the root of the repository:

    cd ninelives

> ⓘ **Stay in the root!**
> Most Bazel commands should be run from the root of the repository.

## Verify build rules

Duration: 2:00

Once inside the repository, we can use Bazel to inspect the available build rules. This confirms that Bazel is correctly configured and can see the project structure.

Run the following command to list all targets:

    bazel query //...

You should see a list of targets starting with `//9l/...`, `//java/...`, and others. This indicates that the `ninelives_story` build rules are working correctly.

## Run a story locally

Duration: 3:00

The ultimate test of our installation is running a story server. We'll use "Cloak of Darkness" (`//9l/cloak`) as our test case.

Run the local server with the following command:

    bazel run //9l/cloak:cloak_local_server

You should see output similar to:

    Serving HTTP on port 8080

Now, open your web browser and visit `http://localhost:8080`. You should be able to play the "Cloak of Darkness" story!

To stop the server, press `Ctrl+C` in your terminal.

## That's all folks!

Duration: 1:00

Congratulations! You have successfully set up the Nine Lives compiler environment and run your first story locally.

In this guide, we have:
- Installed JDK 21, Python 3.13, and Bazelisk.
- Verified the project structure using `bazel query`.
- Launched a local story server using `bazel run`.

### Next steps

* Check out the [Tutorial: Create your first story](tutorial-create-your-first-story.md) to start writing your own interactive fiction.
* Explore the `9l/` directory to see how stories are structured.