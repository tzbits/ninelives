# Create your first Nine Lives story

## Overview

In this guide, we will create a simple branching narrative using the basic Nine Lives (9l) syntax. We'll build an abbreviated version of the "Cat and Mouse" story to understand how choices and nodes work together to form a game.

### What you’ll learn

- How to define nodes and choices using 9l syntax
- How to structure a simple branching story
- How to use special commands like `!gameover` and `!c`
- How to write a Bazel `BUILD` rule for your story
- How to run your story locally

### What you’ll need

- A text editor (like VS Code, Sublime, or even Notepad)
- The Nine Lives compiler installed (see [Install the Nine Lives compiler](guide-install-nine-lives-compiler.md))

## Basic Syntax

The 9l format uses a few simple markers that we'll use throughout this guide:
- `=id=`: Defines a **Node**. This is a specific point in our story.
- `>`: Defines a **Choice**. This links to another node.
- `Text`: Anything else is treated as story text shown to the player.

## Step 1: Setting up your project

Duration: 02:00

Before we start writing our story, let's create a place for it to live.

First, create a directory for your story inside the `9l` directory. Let's call it `my-story`:

```bash
mkdir 9l/my-story
```

Next, create an empty file named `mycatstory.9l` inside that directory. You will be typing your story into this file as we go along.

```bash
touch 9l/my-story/mycatstory.9l
```

## Step 2: The Beginning

Duration: 01:00

Every story needs a starting point. By convention, we usually start with node `0`. Open `9l/my-story/mycatstory.9l` in your editor and add our first node:

```9l
=0=

Something behind the book shelf smells like cheese.

>1 Sniff behind the book shelf.
>2 Meow loudly.
```

In this snippet:
1. `=0=` marks the start of the node.
2. The text describes the situation to the player.
3. `>1` and `>2` create buttons that the player can click to go to nodes `1` or `2`.

## Step 3: Adding a "Game Over"

Duration: 01:00

If the player chooses to sniff behind the bookshelf, they might run into trouble. We use the `!gameover` command to signify an ending. Add this to your file:

```9l
=1=

You poke your nose behind the book shelf and sniff. 
Snap! A mouse trap crushes your nose.

!gameover Good thing you have 9 lives.

>0 Try Again
```

Here, `!gameover` is a special command that formats the game over message consistently. It is only for formatting and doesn't have any effect on the game logic. We provide a choice `>0` to let the player restart from the beginning.

## Step 4: Branching the Narrative

Duration: 01:00

Now let's handle the other choice from the start: meowing. This creates a branch where the story continues instead of ending. Add the following to your file:

```9l
=2=

You meow loudly. Your owner comes over and pets you.
"Aww, what's the matter?" she asks.

>3 Purr when she pets you.
```

## Step 5: Reaching a Conclusion

Duration: 01:00

If the player purrs, they get a reward. We'll wrap up this short adventure with a "Win" state. Add node `3` to your file:

```9l
=3=

She reaches toward you to pet you on the head. You lean in and purr.
"Aww, so sweet," she says. "Do you want some cheese?"

Just then, a mouse runs by.

>4 Chase mouse.
```

And finally, the conclusion in node `4`:

```9l
=4=

You chase the mouse around the house. You catch it!

!c You've realized your potential as a cat.
!gameover You Win!
```

The `!c` command centers the text on the screen, highlighting the remark on your victory.

## Step 6: Creating the BUILD file

Duration: 01:00

Now that we have our story, we need to tell the Nine Lives compiler how to build it. Bazel uses `BUILD` files for this purpose.

Create a `BUILD` file in the same directory (`9l/my-story/BUILD`). This file tells Bazel to use the `ninelives_story` rule to compile your story.

Add the following content to your `BUILD` file:

```python
load("//java/com/tzbits/ninelives:rules.bzl", "ninelives_story")

ninelives_story(
    name = "my_story",
    srcs = ["mycatstory.9l"],
    static = [],
)
```

In this file:
1. `load(...)` imports the Nine Lives build rules.
2. `ninelives_story(...)` defines a new story target.
3. `name = "my_story"` is the name of your target (you'll use this to run it).
4. `srcs = ["mycatstory.9l"]` tells Bazel which story files to include.
5. `static = []` is for static assets like images. Since we don't have any yet, we leave it as an empty list.

## Step 7: Running your story

Duration: 02:00

With the `BUILD` file in place, you can now run your story using Bazel!

Run the following command from the root of the repository:

    bazel run //9l/my-story:my_story_local_server

Bazel will compile your story and start a local web server. You should see:

    Serving HTTP on port 8080

Open your browser and navigate to `http://localhost:8080` to play your new story!

To stop the server, press `Ctrl+C` in your terminal.

## That's all folks!

Congrats, you made it! You've gone from a blank page to a playable branching narrative. You now know how to:

- Create a welcoming starting node
- Provide choices that lead to different outcomes
- Handle game-over nodes and formatted text
- **Set up a directory and BUILD rule for your story**
- **Run your story locally using Bazel**

To see how this story expands with more complex paths, images, and even a perspective shift where you play as the mouse, you should check out the full examples.

### Next steps

* Try adding another choice to node `2` (e.g., `>5 Scratch the sofa`).
* Experiment with the `!img` command to add banners to your nodes.

### Further reading

> ⓘ **Check your progress!**
> You can always compare your snippets with the full "Cat and Mouse" source code in `9l/cat/thecat.9l` and `9l/cat/themouse.9l`.

* Read `doc/reference.md` for a full list of available 9l commands.
