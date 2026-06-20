import {assert, logToScreen} from "/browsertests/TestUtil.js";
import {game} from "/java/com/tzbits/ninelives/data/game.js";

logToScreen("Starting Refresh Persistence Test");

// Mocking some game elements for headless test
if (!document.getElementById('story')) {
    const storyDiv = document.createElement('div');
    storyDiv.id = 'story';
    document.body.appendChild(storyDiv);
}

// Initialize the game view's story reference if it's null
if (!game.gameView.story) {
    game.gameView.story = document.getElementById('story');
}

// Setup game configuration
game.config.gameName = "RefreshTest";
game.config.gameVersion = "1.0.0";
const prefix = "9l:RefreshTest:1.0.0:";
localStorage.clear();

// Mock game nodes
game.gameNodes['=g:0='] = {
    exec: (g) => {
        g.say("Start node");
    }
};
game.gameNodes['=g:node1='] = {
    exec: (g) => {
        g.say("Node 1");
        g.state.visitedNode1 = true;
    }
};

// Start the game as if window.onload was called
logToScreen("Simulating first load...");
if (!game.restoreFromStorage()) {
    game.startNew();
}

assert(game.currentNode() === '=g:0=', "Game should start at =g:0=");

// Advance to node 1
logToScreen("Stepping to node 1...");
game.step(game.choice('=g:node1=', 'Go to node 1'));

assert(game.currentNode() === '=g:node1=', "Current node should be =g:node1=");
assert(game.state.visitedNode1 === true, "State should have visitedNode1 = true");

// Verify storage
assert(localStorage.getItem(prefix + "nodeHistory") !== null, "nodeHistory should be in localStorage");

// Simulate Refresh (re-import/re-initialize)
logToScreen("Simulating page refresh...");

// We can't really re-import in this environment easily, but we can simulate what happens in story.js window.onload
// after a fresh game instance is created.
// Since 'game' is exported as a singleton, we need to reset it to simulate a new instance.

game.reset(); 
// game.reset() clears history but doesn't clear gameNodes or config.
// Crucially, it should leave localStorage alone.

logToScreen("Restoring from storage after 'refresh'...");
if (game.restoreFromStorage()) {
    logToScreen("Restore successful.");
} else {
    logToScreen("Restore FAILED.");
    game.startNew();
}

assert(game.currentNode() === '=g:node1=', "After refresh, game should be at =g:node1=, but was at " + game.currentNode());
assert(game.state.visitedNode1 === true, "After refresh, state should have visitedNode1 = true");

logToScreen("Refresh Persistence Test PASSED!");
