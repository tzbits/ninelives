# Nine Lives DSL Reference

## Syntax

The model of 9l is that the 9l file corresponds to a JavaScript module file. The file contains nodes which correspond to JavaScript functions. The body of a node is text displayed when the node function is called by the user making a choice. The node body is implicitly a JavaScript interpolated string, so any mention of variables using the JavaScript `${}` interpolation syntax are automatically interpolated in the body.

Start of line syntax:

*   `#` is a comment
*   `=` begins a node identifier, which begins a node.
*   `|` is followed by a space is treated as literal JavaScript code.
*   `!` followed by a token is a command.
*   `>` followed by a node id and optional `/n` visit limit is a choice.


## Nodes

A node is a section of a story with an identifier.

```
=0=

Something behind the book shelf smells like cheese.
```

The node's identifier is in column 1, bracked by `=` characters. Stylistically, two blank lines should separate nodes.

## Formatting

The text in nodes and choices supports a subset of Markdown-style formatting:

*   **Bold**: `**text**` becomes `<b>text</b>`
*   *Italic*: `_text_` becomes `<i>text</i>`
*   ***Bold-Italic***: `***text***` becomes `<b><i>text</i></b>`
*   ~~Strikethrough~~: `~~text~~` becomes `<del>text</del>`

## Choices

Choices are listed at the end of a node and contain a target node along with text to be displayed as the choice. Extra data can be attached to a choice by appending it after a semicolon.

Here are two choices between going to node `=1=` and node `=2`:

```
=0=

Something behind the book shelf smells like cheese.

>1 Sniff behind the book shelf.
>2 Meow loudly.; 'EXTRA_DATA'
```

In the example above, if the user chooses the second option, `${choice.data}` in node `=2=` will contain the string `'EXTRA_DATA'`.

Instead of numbering nodes, they may also be named. Stylistically, names should be all lowercase, like identifiers in python.

Choices are also interpolated, for example:

```
=0=

Something behind the book shelf smells like cheese.

| const adv = game.isVisited("=catnip=") ? "loudly" : "softly";

>1 Sniff behind the book shelf.
>2 Meow ${adv}.
```

### Displaying and Using the choice Variable

The variable `${choice}` contains the `Choice` object that led to the current node. It has the following properties:

*   `choice.txt`: The text of the choice that was clicked.
*   `choice.nodeId`: The (resolved) ID of the current node.
*   `choice.data`: An array of extra data associated with the choice (if any).

For example, the `=water=` and `=blankets=` nodes print the choice text:

```
=hot=

It was so hot, I couldn't sleep.

>water I got up and drank some water.
>blankets I threw the sheets on the floor.


=water=

${choice.txt}

It really hit the spot, but somehow I still couldn't sleep.


=blankets=

${choice.txt}

Somehow after that I dozed off.
```

## Commands

A line starting with `!` is a command.

*   `!choices [wrap|nowrap]`: Sets whether choices for the next block (or globally if used outside a node) should be displayed in a wrapping horizontal layout or a vertical list.
*   `!img <url>`: Changes the source of the banner image.
*   `!scope <name>`: Sets the scope for later node IDs in the file.
*   `!c <text>`: Displays centered text (uses the `.center` CSS class).
*   `!gameover <text>`: Displays text with the `.gameover` CSS class, typically used to indicate the end of the story.
*   `!<style> <text>`: Displays text with the specified CSS class.

## The `game` Object API

The `game` object provides access to the game engine's state and methods.

### Methods

*   `game.visitCount(nodeId?)`: Returns the number of times a node has been visited. If `nodeId` is omitted, returns the count for the current node.
*   `game.isVisited(nodeId)`: Returns `true` if the node has been visited at least once.
*   `game.priorNode()`: Returns the ID of the node visited immediately before the current one.
*   `game.currentNode()`: Returns the ID of the current node.
*   `game.say(text)`: Appends a paragraph of text to the story.
*   `game.sayWith(style, text)`: Appends a paragraph of text with a specific CSS class.
*   `game.loadState(gameDataKey)`: Loads the game state from `localStorage` using the given key. This should be called once at the start of the story.
*   `game.saveState()`: Saves the current `game.state` to `localStorage` using the key provided to `game.loadState`.

### Properties

*   `game.state`: A JavaScript object for storing story-specific state. This object is persisted in `localStorage`.
*   `game.useRandomChoiceOrder`: Boolean. If `true`, the order of choices is randomized (default: `true`).
*   `game.wrapChoices`: Boolean. If `true`, choices are displayed in a wrapping horizontal layout (default: `true`). If `false`, choices are displayed in a vertical list.

## Bazel Macro Reference

The `ninelives_story` macro in `rules.bzl` is used to define a story target.

```python
load("//java/com/tzbits/ninelives:rules.bzl", "ninelives_story")

ninelives_story(
    name = "my_story",
    srcs = ["story.9l"],
    static = ["banner.jpg", "extra.css"],
    story_js = "custom_story.js",
)
```

### Parameters

*   `name`: The name of the target. This defines the name of the local server target (`<name>_local_server`) and the release tarball (`<name>_release`).
*   `srcs`: A list of `.9l` source files.
*   `static`: A list of static files to be included in the story distribution (e.g., images, CSS files).
*   `story_js`: (Optional) A label for a custom JavaScript file that defines the `story` object. If omitted, a default empty story object is used.

## Variables and State

The `game` variable is in the scope of every node and gives access to the underlying game mechanics (see [The `game` Object API](#the-game-object-api)).

The `story` variable is the JavaScript object that holds the story state.

For example,

```
=interview=

| story.john = {};
| story.john.mood = 0;

"Hi, I'm John, I'll be your interviewer today. Can I get you something to drink?"

>beer Yessir, I'll take a cold beer!
>nothanks No, thank you for offering though.


=beer=

| story.john.mood--;

${choice.txt}

John replies, "I'm sorry, drinking on company property is against company policy."
```

## Conditional Content

### Conditional text

JavaScript can be used to conditionally display content in a node. For example,

```
=hallway=

In the hallway, you ask John to direct you to the bathroom.

He replies, "The bathroom is straight ahead and to the right."

| if (story.john.mood < -3) {

John then looks at you sideways and says, "Please don't make a mess in there."

| }

```

### Conditional Choices

A conditional choice has a boolean test expression.

```
>nodeid :if testexpression; choicetextexpression
```

If the test is false, the choice is omitted from the choice list. Otherwise, the string given by choicetextexpression is used as the choice text. For example,

```
>dropcloak :if story.cloak.canDrop(); "drop Cloak"
```

The two expressions after the `:if`, separated by ';' must be valid JavaScript. You can also append extra data after another semicolon.

```
>dropcloak :if story.cloak.canDrop(); "drop Cloak"; {weight: 10}
```

Note: The older syntax `>nodeid ? testexpression; choicetextexpression` is also supported but `:if` is preferred.

### Choice Visit Limits

A choice can be limited to a certain number of visits by appending `/n` to the node ID, where `n` is the maximum number of times the target node can be visited before the choice disappears.

```
>node/1 This choice disappears after one visit to "node".
```

This can be combined with conditional choices:

```
>secret/1 :if story.hasKey; "Enter secret room"
```
