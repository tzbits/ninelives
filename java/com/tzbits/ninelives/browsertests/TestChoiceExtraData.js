import {assert, logToScreen} from "/browsertests/TestUtil.js";
import {game} from "/java/com/tzbits/ninelives/data/game.js";

logToScreen("Starting Choice Extra Data Test");

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

// 1. Test passing extra data to node.exec
logToScreen("Testing passing extra data to node.exec...");

let receivedData = null;
game.gameNodes['=g:dataTest='] = {
    exec: (g, choice) => {
        receivedData = choice.data;
    }
};

const extraData = ["value1", {key: "value2"}];
game.step(game.choice('=g:dataTest=', 'Test', extraData));

assert(receivedData !== null, "Node should have received choice object");
assert(Array.isArray(receivedData), "Received data should be an array");
assert(receivedData[0] === "value1", "First element of extra data should match");
assert(receivedData[1].key === "value2", "Second element (object) of extra data should match");

// 2. Test persistence of extra data in nodeHistory
logToScreen("Testing persistence of extra data...");

const prefix = "9l:TestGame:1.0.0:";
game.config.gameName = "TestGame";
game.config.gameVersion = "1.0.0";
localStorage.clear();

game.step(game.choice('=g:dataTest=', 'Test Persistence', ["persistedValue"]));

const savedNodesJson = localStorage.getItem(prefix + "nodeHistory");
const savedNodes = JSON.parse(savedNodesJson);
const lastChoice = savedNodes[savedNodes.length - 1];

assert(lastChoice.nodeId === '=g:dataTest=', "Persisted choice should have correct nodeId");
assert(Array.isArray(lastChoice.data), "Persisted data should be an array");
assert(lastChoice.data[0] === "persistedValue", "Persisted extra data value should match");

// 3. Test recovery of extra data during replay
logToScreen("Testing recovery of extra data during replay...");

game.reset();
receivedData = null;

// Replay with the history we just persisted
game.replay(savedNodes, []);

assert(receivedData !== null, "Node should have received choice object during replay");
assert(Array.isArray(receivedData), "Received data should be an array during replay");
assert(receivedData[0] === "persistedValue", "Extra data should be correctly passed during replay");

logToScreen("All Choice Extra Data tests passed!");
