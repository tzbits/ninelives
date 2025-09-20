/* story.js */
/* This is the default. Create a story.js in the 9l project directory to override this one. */
import {game} from "./game.js";

export const story = {};

// Example of debug code.

// const nodesCh1 = [
//   game.choice('=0=', ''),
//   game.choice('=ch1-0=', 'Chapter 1: H. Harbor - Prologue'),
//   game.choice('=ch1-smell=', 'the smell'),
//   game.choice('=ch1-attic0=', 'left'),
//   game.choice('=ch1-attic1=', 'between his legs'),
//   game.choice('=ch1-attic2=', 'bottom shelf'),
// ];

// game.enableDebug = true;
// game.debugOnLoadFn = function () {
//   console.log("begin debug")
//   game.runNodes(
//     nodesCh1
//   );
// }

window.onload = function () {
  if (game.enableDebug && game.debugOnLoadFn) {
    game.debugOnLoadFn()
    return;
  }
  game.step(game.choice('=0=', ''))
}

console.log("-- story.js loaded");
