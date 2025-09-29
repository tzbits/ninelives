# Nine Lives DSL Reference

## Syntax

The model of 9l is that the 9l file corresponds to a Javascript module file. The file contains nodes which correspond to javascript functions. The body of a node is text that is displayed when the node function is called by the user making a choice. The node body is implicitly a javascript interpolated string, so any mention of variables using the javascript `${}` interpolation syntax are automatically interpolated in the body.

Start of line syntax:

`#` is a comment
`=` begins a node identifier, which begins a node.
`|` is followed by a space is treated as literal javascript code.
`!` follwed by a token is a command.
`>` folowed by a node id is a choice.


## Nodes

A node is a section of a story with an identifier.

```
=0=

Something behind the book shelf smells like cheese.
```

The node's identifier is in column 1, bracked by `=` characters. Stylistically, two blank lines should separate nodes.

## Choices

Choices are listed at the end of a node and contain a target node along with text to be displayed as the choice.

Here are two choices between going to node `=1=` and node `=2`:

```
=0=

Something behind the book shelf smells like cheese.

>1 Sniff behind the book shelf.
>2 Meow loudly.
```

Instead of numbering nodes, they may also be named. Stylistically, names should be all lowercase, like identifiers in python.

Choices are also interpolated,

```
=0=

Something behind the book shelf smells like cheese.

| const adv = game.isVisited("=catnip=") ? "loudly" : "softly";

>1 Sniff behind the book shelf.
>2 Meow ${adv}.
```

### Displaying the choice text

The variable `${choice}` contains the text of the choice that led to the current node. For example, the `=water=` and `=blankets=` nodes print the choice:

```
=hot=

It so hot, I couldn't sleep.

>water I got up and drank some water.
>blankets I threw the sheets on the floor.


=water=

${choice}

It really hit the spot, but somehow I still couldn't sleep.


=blankets=

${choice}

Somehow after that I dozed off.
```

## Variables and State

In addition to the `choice` variable, the `game` variable in the scope of a ndoe and gives access to the underlying game mechanics via methods like `game.say(\`hello ${name}\`)` which prints the node's text to the screen.

The `story` variable is the Javascript object that holds the story state.

For example,

```
=interview=

| story.john = {};
| story.john.mood = 0;

"Hi, I'm John, I'll be your interviewer today. Can I get you something to drink?"

>beer "Yessir, I'll take a cold beer!"
>nothanks "No, thank you for offering though.".


=beer=

| story.john.mood--;

${choice}

John replies, "I'm sorry, drinking on company property is against company policy."
```

## Conditional Content

### Conditional text

Javascript can be used to conditionally display content in a node. For example,

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
>nodeid ? testexpression; choicetext
```

If the test is false, the choice is omitted from the choice list. For example,

```
>dropcloak ? story.cloak.canDrop(); "drop Cloak"
>takecloak ? story.cloak.canTake(); "take Cloak"
```
