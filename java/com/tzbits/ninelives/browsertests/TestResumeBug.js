import {assert, logToScreen} from "/browsertests/TestUtil.js";
import {game} from "/java/com/tzbits/ninelives/data/game.js";

logToScreen("Starting Resume Bug Reproduction Test");

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

// 1. Setup game nodes
game.gameNodes['=g:0='] = {
    exec: (g) => {
        g.say("Node 0");
    }
};
game.gameNodes['=g:node1='] = {
    exec: (g) => {
        g.state.score = 10;
        g.say("Node 1");
    }
};
game.gameNodes['=g:node2='] = {
    exec: (g) => {
        g.say("Node 2");
    }
};

// 2. Start a game and progress
logToScreen("Starting new game and progressing to Node 2...");
game.config.gameName = "ResumeBugTest";
game.config.gameVersion = "1.0.0";
localStorage.clear();

game.step(game.choice('=g:0=', ''));
game.step(game.choice('=g:node1=', 'Go to 1'));
game.step(game.choice('=g:node2=', 'Go to 2'));

assert(game.currentNode() === '=g:node2=', "Should be at Node 2");
assert(game.nodeHistory.length === 3, "History should have 3 nodes");

// 3. Simulate page reload - Restore from storage
logToScreen("Simulating page reload and calling restoreFromStorage()...");
game.reset(); // Clear in-memory state
// Simulate what story.js SHOULD do:
if (game.restoreFromStorage()) {
    logToScreen("Restored from storage.");
} else {
    game.startNew();
}

assert(game.currentNode() === '=g:node2=', "After restore, should be at Node 2");
assert(game.nodeHistory.length === 3, "After restore, history should have 3 nodes");

// 4. Verify that we DON'T start over if restore was successful
logToScreen("Verifying we stay at Node 2...");
assert(game.currentNode() === '=g:node2=', "Current node should be =g:node2=");
assert(game.nodeHistory.length === 3, "History length should be 3");

logToScreen("Resume Bug Reproduction Test finished");
