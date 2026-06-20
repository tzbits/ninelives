# State Design

## Background

Currently, Nine Lives leverages browser-based sessions for interactive fiction, but the mechanism for managing game state across these sessions is suboptimal. As stories grow in complexity and involve narrative chapters or branching paths, simply relying on transient state or unmanaged local storage becomes problematic. There is a need for a more robust and intuitive way to handle persistence, debugging, and the relationship between narrative progress and state variables.

## Requirements

*   **Persistence**: Users should be able to resume their progress after closing the browser or refreshing the page.
*   **Consistency**: Returning to a previous state (e.g., via "undo" or re-visiting a chapter) should not result in conflicting or "impossible" game states.
*   **Simplicity**: The system should be easy for authors to understand—nodes should ideally behave like functions.
*   **Navigability**: Facilitate exploration of different story branches without creating a "combinatorial explosion" of state management complexity.
*   **Debugging**: Provide an easy way to reproduce a specific game state for testing and debugging purposes.

## Design Overview

The proposed design shifts toward a history-based state model. Instead of serializing a monolithic state object, the game primarily tracks the sequence of actions (node visits) taken by the player. State is treated as a derivative of this history. This ensures that the state is always a direct consequence of the player's path through the story, facilitating a clean "undo" mechanism and consistent reloads.

### 1. Node History and Replay
The core state is a sequence of node IDs. When a player returns to a session, the engine replays this sequence in the background to reconstruct the current environment.
*   **Undo/Redo**: Since state is derived from the sequence, "undoing" an action simply means popping the last node from the history and re-running the sequence.
*   **Storage**: The sequence of node IDs and a parallel chain map of immutable state objects is persisted in local storage.

### 2. Functional State Lookup
To handle variables (like a player's jacket color), each node in the history chas an associated state object.
*   **Environment Lookup**: When searching for a property value, the engine starts with the most recent node's state object and iterates backward through the history until the property is found.
*   **Consistency**: If a node is undone, its associated state object is also removed.

## Design Details

State is managed through two parallel arrays persisted in local storage: a **node history** (an array of node IDs) and a **state history** (a parallel array of plain objects, functioning as a ChainMap).

Existing games continue to use `game.state.foo = "bar"` to set state. The framework reconstructs `game.state` from the state history before each node runs, so authors write familiar assignments and the history mechanism handles consistency and undo automatically. The only catch is that authors can only put immutable objects into the game state. For example, one cannot do:

    game.state.hat.color = 'red';

Instead, one must do:

    const hat = {...game.state.hat};
    hat.color = 'red';
    game.state.hat = hat;

### Clicking a Choice

When a user clicks a choice, the framework:

1. Initializes `game.state` and `game.preState` to `{}`.
2. Reconstructs `game.state` by iterating through the state history from oldest to newest, copying all properties onto both `game.state` and `game.preState`. After this step both objects are identical snapshots of accumulated state.
3. Runs the node function. The node may read and write `game.state` freely using immutable values.
4. Diffs `game.state` against `game.preState`. Any property whose value differs is collected into a new plain object.
5. Pushes the target node ID onto the node history.
6. Pushes the diff object onto the state history.

Because the diff is computed after the node runs, existing games need no changes to how they write state. This meets the **Simplicity** requirement.

Example — an author writes state exactly as before:

```9l
=ch4-0=

The space in the cart is dwindling.

Each flower offers its own charm and appeal, but only one more
kind can accompany you to H. Harbor today. Which will you take?

>ch4-s2-0 asters; 'asters'
>ch4-s2-0 chrysanthemums; 'chrysanthemums'
>ch4-s2-0 dahlias; 'dahlias'
>ch4-s2-0 marigolds; 'marigolds'
>ch4-s2-0 sweet williams; 'sweet williams'

=ch4-s2-0=

| game.state.kind_of_flowers = choice.data;

You survey the options before you and decide that the
${game.state.kind_of_flowers} are the most appealing and likely
to catch the eyes of your customers today.
```

The assignment `game.state.kind_of_flowers = choice.data` is diffed against `preState` after the node runs, and only the changed property is stored in the state history entry.

### Looking Up State

Because `game.state` is fully reconstructed before each node runs, authors simply read `game.state.foo`. No new `val()` function is required. This meets the **Consistency** requirement—state is always a direct consequence of the path taken, and there is no risk of stale values from a discarded branch surviving into the current one.

### Undoing a Choice

Undo is the inverse of a click:

1. Pop the node history.
2. Pop the state history.
3. Run the node function for the new most-recent node ID (which will reconstruct `game.state` from the now-shorter history as step 1–2 above).

Because the diff for the undone step is removed, any properties it set revert to whatever earlier steps had established. This prevents impossible states (e.g., keeping a "red jacket" after replaying the branch where you chose "blue").

### Persistence

Both arrays are serialized to `localStorage` under a game-specific key (e.g., `9l:gameName:gameVersion:nodeHistory`). On page load, the engine replays the node history in the background to restore the session. This meets the **Persistence** requirement without storing a monolithic state blob.

### Build-time Switch

Nine Lives supports a build-time switch to control whether `stateHistory` (the diffs) is persisted to `localStorage`.

- **Development**: By default, `bazel run //...:xxx_dev_server` disables `stateHistory` persistence. On reload, the engine replays the `nodeHistory` from scratch, recalculating the state from the current node logic. This is ideal for iterative development as changes to node functions are immediately reflected upon refreshing.
- **Release**: By default, `bazel build //...:xxx_release` (and `local_server`) enables `stateHistory` persistence. This ensures that the user's state remains exactly as it was when they last played, even if the underlying code has changed.

This is controlled via a generated `env.js` file injected by the Bazel rules.

## Task List

### Remove the old persistence mechanism

- [x] Remove `game.loadState()` and `game.saveState()` from `game.js`.
- [x] Remove the `beforeunload` handler that calls `game.saveState()`.
- [x] Remove `game.nodeVisits` and the visit-count logic inside `game.step()` (replaced by counting the node in node history).
- [x] Remove `game.gamePath` and `game.atNodeId` tracking (replaced by `nodeHistory`).

### Add the new history-based state system
  
- [x] Add `game.nodeHistory` (array of node ID strings) to `Game`.
- [x] Add `game.stateHistory` (parallel array of plain objects) to `Game`.
- [x] At the start of `game.step(choice)`, initialize `game.state = {}` and `game.preState = {}`.
- [x] Reconstruct `game.state` (and copy to `game.preState`) by iterating through `stateHistory` oldest-first, spreading each entry's properties onto both objects.
- [x] Run the node function (existing node code continues to use `game.state.foo = value` unchanged).
- [x] After the node function returns, diff `game.state` against `game.preState` to produce a plain object containing only changed properties.
- [x] Push the target node ID onto `nodeHistory` and the diff object onto `stateHistory`.
- [x] Implement `game.undo()`: pop both `nodeHistory` and `stateHistory`, then call `game.step` for the new last node in `nodeHistory`.
- [x] Persist `nodeHistory` and `stateHistory` to `localStorage` on each step (replacing the old `saveState` call).
- [x] On page load, replay `nodeHistory` from `localStorage` in the background to restore the session.

### Update the DSL and transpiler

- [ ] Update the reference documentation to describe the new persistence model and the `undo` affordance.

### Update existing stories

- [x] Audit uses of `game.loadState()` and `game.saveState()` in existing stories and remove them (state is now persisted automatically).
- [x] No changes needed to how stories read/write `game.state`.

## Alternatives Considered

1.  **Stateless Games**: Keeping games extremely short so that "state" is merely the URL of the next short segment.
    *   *Pros*: Simple, easy to debug, easy to share/bookmark.
    *   *Cons*: Limits complexity; risks combinatorial explosion if trying to represent a larger world through many tiny separate games.

2.  **Branch Point ID Only**: Storing only the ID of the most recent significant branch point.
    *   *Pros*: Simple persistence.
    *   *Cons*: Harder to bookmark specific sub-points; requires more complex UI for save/restore if the user wants to return to specific nodes within a branch.

3.  **Monolithic State Serialization**: Serializing the entire `.props` attribute of game objects to local storage.
    *   *Pros*: Captures everything.
    *   *Cons*: Gets out of hand quickly; highly prone to "state chaos" when mixing narrative sequences with variable-heavy logic. It makes the game a pure function of its state, which is often at odds with the linear/branching nature of storytelling.

4.  **Implicit History Replay**: Storing the sequence of nodes but requiring a manual "clear storage" to start over.
    *   *Pros*: Recreates state perfectly.
    *   *Cons*: Requires UI affordances for "Restart" or "Undo" to prevent players from getting stuck in a stale session. Attempting to manage this with "environment lookup" (Design Detail #2) was chosen as the preferred refinement of this approach.

## Future Work

### Narrative Chapters and Defaults
To support dividing a story into chapters, and allowing one to jump into specific chapters, authors should declare defaults at the beginning of the story for important state that crosses chapters.
**Reset on Entry**: playing a later chapter first and then playing an earlier one should pop all history related to the later chapter.
