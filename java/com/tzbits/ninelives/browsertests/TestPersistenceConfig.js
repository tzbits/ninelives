import {assert, logToScreen} from "/browsertests/TestUtil.js";
import {game} from "/java/com/tzbits/ninelives/data/game.js";

logToScreen("Starting Persistence and Config Test");

// Mocking some game elements for headless test
if (!document.getElementById('story')) {
    const storyDiv = document.createElement('div');
    storyDiv.id = 'story';
    document.body.appendChild(storyDiv);
}
if (!document.getElementById('banner')) {
    const bannerImg = document.createElement('img');
    bannerImg.id = 'banner';
    document.body.appendChild(bannerImg);
}

// Initialize the game view's story reference if it's null
if (!game.gameView.story) {
    game.gameView.story = document.getElementById('story');
}

// Mocking some game nodes
game.gameNodes['=g:start='] = {
    exec: (g) => {
        g.state.score = 10;
    }
};

// 1. Test Game-Specific Keys
logToScreen("Testing game-specific localStorage keys...");
game.config.gameName = "TestGame";
game.config.gameVersion = "1.0.1";
game.step(game.choice('=g:start=', 'Start'));

const prefix = "9l:TestGame:1.0.1:";
assert(localStorage.getItem(prefix + "nodeHistory") !== null, "nodeHistory should be stored with game-specific prefix");

// 2. Test persistStateHistory switch (enabled by default)
assert(localStorage.getItem(prefix + "stateHistory") !== null, "stateHistory should be stored when persistStateHistory is true");

// 3. Test persistStateHistory switch (disabled)
logToScreen("Testing persistStateHistory = false...");
game.reset();
localStorage.clear();
game.config.persistStateHistory = false;
game.step(game.choice('=g:start=', 'Start'));
assert(localStorage.getItem(prefix + "stateHistory") === null, "stateHistory should NOT be stored when persistStateHistory is false");

// 4. Test replay with missing stateHistory (Development mode scenario)
logToScreen("Testing replay without stateHistory...");
game.reset();
const savedNodesJson = localStorage.getItem(prefix + "nodeHistory");
const savedNodes = JSON.parse(savedNodesJson);
assert(typeof savedNodes[0] === 'object', "nodeHistory should contain objects, not strings");
assert(savedNodes[0].nodeId === '=g:start=', "Choice object should have correct nodeId");
assert(savedNodes[0].txt === 'Start', "Choice txt should match.");
assert(Array.isArray(savedNodes[0].data) && savedNodes[0].data.length === 0, "Default extra data should be an empty array.");

// We simulate a fresh session where only nodeHistory exists
game.replay(savedNodes, []); 
assert(game.state.score === 10, "State should be reconstructed from node logic during replay");

// 5. Test initialState preservation
logToScreen("Testing initialState preservation...");
game.reset();
localStorage.clear();
game.state.preInitProp = "fixed_value";
game.step(game.choice('=g:start=', 'Start'));

assert(game.state.preInitProp === "fixed_value", "Property set before first step should be preserved");
assert(localStorage.getItem(prefix + "initialState") !== null, "initialState should be persisted");
const initial = JSON.parse(localStorage.getItem(prefix + "initialState"));
assert(initial.preInitProp === "fixed_value", "initialState in storage should have the pre-initialized property");

// Test recovery from storage
logToScreen("Testing recovery of initialState from storage...");
game.reset();
game.restoreFromStorage();
assert(game.state.preInitProp === "fixed_value", "Property should be recovered after restoreFromStorage");
assert(game.initialState.preInitProp === "fixed_value", "initialState should be recovered after restoreFromStorage");

logToScreen("All Persistence and Config tests passed!");
