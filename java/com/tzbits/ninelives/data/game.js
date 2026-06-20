// game.js
// noinspection JSUnusedGlobalSymbols

/**
 * View class for text games.
 */
class GameView {
  /**
   * @type {HTMLDivElement}
   * @description The outer div element where each new StoryElt gets appended.
   */
  story;
  /**
   * @type {HTMLDivElement|null}
   * @description An "offscreen" buffer, created by startStoryElt.
   */
  storyElt;

  constructor() {
    this.story = document.getElementById('story');
    this.storyElt = null;
  }

  /**
   * Creates a new {@link storyElt} and appends it to {@link story}.
   */
  startStoryElt(timeStep) {
    const eltId = `t${timeStep}`
    const elt = document.createElement('div')
    elt.setAttribute('id', eltId)
    elt.classList.add('StoryElt', 'hidden')
    this.storyElt = elt;
  }

  /**
   * Clears {@link story} and appends {@link storyElt}.
   */
  showStoryElt() {
    this.story.innerHTML = "";
    window.scrollTo(0, 0);
    this.story.append(this.storyElt);
    // Reading a layout property here to force the browser to calculate and
    // render the initial 'hidden' state (opacity: 0) after appending.
    // Without this, the browser jump-cuts the fade-in transition.
    this.storyElt.offsetHeight;
    this.storyElt.classList.remove('hidden');
  }

  /** Append a paragraph of text to {@link storyElt}. */
  say(txt) {
    this.storyElt.innerHTML += `<p>${applyMarkdown(txt)}</p>`
  }

  /**
   * Append a paragraph of text to {@link storyElt} with a style.
   * @param {string} styleCode
   * @param {string} text
   */
  sayWith(styleCode, text) {
    this.storyElt.innerHTML += `<p class='${this.codeToStyle(
        styleCode)}'>${applyMarkdown(text)}</p>`
  }

  /** @param {string} styleCode */
  codeToStyle(styleCode) {
    if (styleCode === 'c') {
      return 'center';
    }
    if (styleCode === 'l') {
      return 'noindent';
    }
    if (styleCode === 'grow') {
      return 'grow';
    }
    return styleCode;
  }

  /**
   * Appends 'choices' to {@link storyElt}.
   * @param {Game} game
   * @param {Choice[]} choices
   */
  displayChoices(game, choices, wrapChoices) {
    const gameView = this
    const choicesElt = document.createElement("div")
    const wrap = (typeof wrapChoices === 'boolean') ? wrapChoices : game.wrapChoices;
    choicesElt.classList.add(wrap ? 'wrapped-choices' : 'choices')
    if (game.useRandomChoiceOrder) {
      choices.sort(function () {
        // ignoring args a & b.
        return Math.round(Math.random()) - 0.5
      })
    }
    for (const choice of choices) {
      if (!choice || !choice.nodeId) {
        continue
      }
      const choiceElt = document.createElement("div")
      choiceElt.classList.add('choice')
      const linkElt = document.createElement("span")
      linkElt.classList.add('link')
      let text = `${choice.txt}`;
      if (game.enableDebug && game.debugVerbosity > 0) {
        text += ` (${choice.nodeId})`;
      }
      linkElt.innerHTML = applyMarkdown(text);
      linkElt.onclick = function () {
        choicesElt.remove()
        game.step(choice)
      }
      choiceElt.append(linkElt)
      choicesElt.append(choiceElt)
    }
    gameView.storyElt.append(choicesElt)
  }

  /**
   * Removes any html element with the class, "choices", from the document.
   */
  removeChoices() {
    const element = document.querySelector('.choices, .wrapped-choices');
    if (element) {
      element.parentNode.removeChild(element);
    }
  }

  /** Changes the src image of the image element with the id, 'banner'. */
  img(url) {
    let bannerElt = document.getElementById('banner');
    if (bannerElt !== null) {
      bannerElt.setAttribute('src', url);
    }
  }
}

export class AbstractGameNode {
  /** @type {string} */
  nodeId;
  /** @type {function(Game, Choice): void} */
  execFn;
  location = "none";
  label = "GameNode_undefined";

  constructor(nodeId) {
    this.nodeId = nodeId
    this.execFn =
        function (game, choice) {
          game.say(
              `Executing choice but the gameNode exec method for "${choice.nodeId}"`
              + ` is not implemented. Choice text was: "${choice.txt}".`)
        }
  }

  exec(game, choice) {
    return this.execFn(game, choice)
  }

  setExecFn(execFn) {
    this.execFn = execFn
    return this
  }

  setLabel(label) {
    this.label = label
    return this
  }

  setLocation(location) {
    this.location = location
    return this
  }

  firstVisit() {
    return game.visitCount(this.nodeId) === 1;
  }

  enteredFrom(...ids) {
    return ids.map((id) => game.resolveScope(id)).includes(game.priorNode());
  }

  visitCount() {
    return game.visitCount(this.nodeId);
  }
}

/**
 * * @extends {AbstractGameNode}
 */
export class GameNode extends AbstractGameNode {
  constructor(nodeId) {
    super(nodeId)
  }
}

/**
 * @extends {AbstractGameNode}
 */
class Player extends AbstractGameNode {
  constructor() {
    super("g:player")
    // Player always starts at node "=g:0="
    this.location = "=g:0="
    this.label = "yourself"
  }
}

/**
 * * @extends {AbstractGameNode}
 */
export class GameItem extends AbstractGameNode {
  constructor(nodeId, label, location) {
    super(nodeId)
    this.location = location
    this.label = label
  }
}

class Choice {
  /** @type {string} */
  nodeId;
  /** @type {string} */
  txt;
  data = [];

  constructor(toNodeId, txt, data = []) {
    this.nodeId = toNodeId;
    this.txt = txt;
    this.data = data;
  }
}

class Game {
  /**
   * Node handlers indexed by node id.
   * @type {Record<string, AbstractGameNode>}
   */
  gameNodes;
  /** @type {Player} */
  player = new Player();

  gameView = new GameView()

  /** current scope for functions that use unqualified node ids. */
  scope = "g";  // g for global scope

  /**
   * All game states.
   * Persisted to localStorage as one blob.
   * @type {object}
   */
  state = {};

  /**
   * The state before the current node runs. Used for diffing.
   * @type {object}
   */
  preState = {};

  /** Whether to randomize the order of choices. Defaults to true. */
  useRandomChoiceOrder = true;

  /**
   * Whether to put the choices on a wrapping horizontal vs listing
   * them vertically.
   */
  wrapChoices = true;

  /** Includes debug info when clicking through the game. */
  enableDebug = false
  debugVerbosity = 0;

  /** @private */
  replaying = false;

  /**
   * History of choices for redo.
   * @type {Choice[]}
   */
  redoHistory = []

  /**
   * History of state diffs for redo.
   * @type {object[]}
   */
  redoStateHistory = []

  config = {
    gameName: 'ninelives-game',
    gameVersion: '0.0.1',
    persistStateHistory: true
  };

  /** Set this function to run on load and take control of stepping through a path in the story. */
  debugOnLoadFn = null

  /**
   * The current time, a strictly increasing int.
   * @private {number}
   */
  timeStep = 0

  /** Node ids are added to this array as they are stepped into. */
  nodeHistory = []

  /**
   * Parallel array to nodeHistory containing diff objects of state changes.
   * @type {object[]}
   */
  stateHistory = []

  /**
   * State properties set before any node runs.
   * @type {object}
   */
  initialState = {}

  /** Place to hold game items. */
  items = {}

  constructor() {
    this.gameNodes = {'=g:player=': /** @type {AbstractGameNode} */ this.player}
  }

  reset() {
    this.nodeHistory = [];
    this.stateHistory = [];
    this.redoHistory = [];
    this.redoStateHistory = [];
    this.timeStep = 0;
    this.state = Object.assign({}, this.initialState);
    this.preState = {};
  }

  confirmReset() {
    if (confirm("Are you sure you want to reset the game? This will clear all history.")) {
      this.reset();
      this.startNew();
    }
  }

  resolveScope(id) {
    // player id needs to be made a special always-global object
    // for backward compatibility.
    if (id === "=player=") {
      return "=g:player=";
    }
    if (id.includes(":")) {
      return id;
    }
    return "=" + this.scope + ":" + id.substring(1);
  }

  /**
   * Move one step forward in time and call the game node for `toNodeId`.
   * @param {Choice} choice
   */
  step(choice, isRedo = false) {
    if (this.enableDebug) {
      this.outputDebugInfo(choice)
    }

    if (!isRedo && !this.replaying) {
      this.redoHistory = [];
      this.redoStateHistory = [];
    }

    if (this.nodeHistory.length === 0 && !this.replaying) {
      this.initialState = Object.assign({}, this.state);
    }

    this.state = Object.assign({}, this.initialState);
    this.preState = {};
    // During replay, step() is called for the i-th node.
    // this.stateHistory already contains diffs for [0...i].
    // BUT wait, the i-th node's diff was produced AFTER it ran.
    // Re-reading design: "reconstructs game.state by iterating through the state history from oldest to newest... After this step both objects are identical snapshots of accumulated state. ... Runs the node function. ... Diffs game.state against preState. ... Pushes the diff object onto the state history."
    
    // So for the i-th node, it should only see diffs from [0...i-1].
    const currentDiffs = this.replaying ? this.stateHistory.slice(0, -1) : this.stateHistory;

    for (const diff of currentDiffs) {
      Object.assign(this.state, diff);
    }
    Object.assign(this.preState, this.state);

    let toNodeId = this.resolveScope(choice.nodeId)
    choice.nodeId = toNodeId; // Ensure the stored choice has the resolved ID
    this.nodeHistory.push(choice)
    this.timeStep = this.timeStep + 1

    this.gameView.startStoryElt(this.timeStep)

    if (!(toNodeId in this.gameNodes)) {
      throw `The node ${toNodeId} was not found by game.step`
    }

    // @type {AbstractGameNode}
    const nd = this.gameNodes[toNodeId];
    nd.exec(this, choice);

    if (!this.replaying) {
      // Persist state change history
      const diff = {};
      for (const key in this.state) {
        if (this.state[key] !== this.preState[key]) {
          diff[key] = this.state[key];
        }
      }
      // Also check for deleted keys (though not explicitly required by design, it's good practice)
      for (const key in this.preState) {
        if (!(key in this.state)) {
          diff[key] = undefined;
        }
      }
      this.stateHistory.push(diff);

      this.persist();
    }

    this.gameView.showStoryElt();
  }

  getStoragePrefix() {
    return `9l:${this.config.gameName}:${this.config.gameVersion}:`;
  }

  persist() {
    const prefix = this.getStoragePrefix();
    localStorage.setItem(`${prefix}nodeHistory`, JSON.stringify(this.nodeHistory));
    localStorage.setItem(`${prefix}redoHistory`, JSON.stringify(this.redoHistory));
    localStorage.setItem(`${prefix}initialState`, JSON.stringify(this.initialState));
    if (this.config.persistStateHistory) {
      localStorage.setItem(`${prefix}stateHistory`, JSON.stringify(this.stateHistory));
      localStorage.setItem(`${prefix}redoStateHistory`, JSON.stringify(this.redoStateHistory));
    }
  }

  loadHistoryFromStorage() {
    const prefix = this.getStoragePrefix();
    const nodes = localStorage.getItem(`${prefix}nodeHistory`);
    const states = localStorage.getItem(`${prefix}stateHistory`);
    const redoNodes = localStorage.getItem(`${prefix}redoHistory`);
    const redoStates = localStorage.getItem(`${prefix}redoStateHistory`);
    const initial = localStorage.getItem(`${prefix}initialState`);
    return {
      nodeHistory: nodes ? JSON.parse(nodes) : null,
      stateHistory: states ? JSON.parse(states) : null,
      redoHistory: redoNodes ? JSON.parse(redoNodes) : null,
      redoStateHistory: redoStates ? JSON.parse(redoStates) : null,
      initialState: initial ? JSON.parse(initial) : null,
    };
  }

  hasSavedSession() {
    const { nodeHistory } = this.loadHistoryFromStorage();
    return Array.isArray(nodeHistory) && nodeHistory.length > 0;
  }

  restoreFromStorage() {
    const { nodeHistory, stateHistory, redoHistory, redoStateHistory, initialState } = this.loadHistoryFromStorage();
    if (nodeHistory && nodeHistory.length > 0) {
      this.initialState = initialState || {};
      this.replay(nodeHistory, Array.isArray(stateHistory) ? stateHistory : []);
      this.redoHistory = Array.isArray(redoHistory) ? redoHistory : [];
      this.redoStateHistory = Array.isArray(redoStateHistory) ? redoStateHistory : [];
      return true;
    }
    return false;
  }

  startNew() {
    this.step(this.choice('=0=', ''));
  }

  replay(history, states = []) {
    this.replaying = true;
    try {
      this.reset();
      for (let i = 0; i < history.length; i++) {
        const choice = history[i];
        const diff = states[i] || {};
        // Note: step() will not update state history when replaying == true.
        this.stateHistory.push(diff);
        this.step(choice);
      }
    } finally {
      this.replaying = false;
    }
  }

  undo() {
    if (this.nodeHistory.length > 1) {
      const currentChoice = this.nodeHistory.pop();
      const currentDiff = this.stateHistory.pop();
      this.redoHistory.push(currentChoice);
      this.redoStateHistory.push(currentDiff);

      const lastChoice = this.nodeHistory.pop();
      this.stateHistory.pop();
      // Re-run the last node
      this.step(lastChoice, /*isRedo=*/ true);
    }
    this.persist();
  }

  redo() {
    if (this.redoHistory.length > 0) {
      const choice = this.redoHistory.pop();
      const diff = this.redoStateHistory.pop();
      this.step(choice, /*isRedo=*/ true);
      this.persist();
    }
  }

  getNode(id) {
    return this.gameNodes[this.resolveScope(id)];
  }

  /**
   * Returns the nodeId set that `step` is running. This is meant to be
   * referred to from other downstream methods called within the node's
   * exec method.
   */
  currentNode() {
    return this.nodeHistory.length > 0 ? this.nodeHistory[this.nodeHistory.length - 1].nodeId : ''
  }

  priorNode() {
    const choice = this.nodeHistory.slice(-2)[0];
    return choice ? choice.nodeId : undefined;
  }

  visitCount(nodeId) {
    const countVisits = (id) => this.nodeHistory.filter((choice) => choice.nodeId === id).length;
    if (nodeId === undefined) {
      return countVisits(this.currentNode())
    }
    const id = this.resolveScope(nodeId);
    const nd = this.gameNodes[id];
    if (!nd) {
      throw `${nodeId} (${id}) not found.`;
    }
    return countVisits(id);
  }

  isVisited(nodeId) {
    const id = this.resolveScope(nodeId);
    const nd = this.gameNodes[id];
    if (!nd) {
      throw `${nodeId} (${id}) not found.`;
    }
    return this.visitCount(id) > 0
  }

  runNodes(choiceList) {
    for (let choice of choiceList.slice(0, -1)) {
      this.step(choice)
      this.gameView.removeChoices()
    }
    if (choiceList) {
      this.step(choiceList.slice(-1)[0])
    }
  }

  /**
   * @param {Choice} choice
   */
  outputDebugInfo(choice) {
    if (this.debugVerbosity > 0) {
      const elt = document.createElement('pre')
      elt.innerText = `choice: ${JSON.stringify(choice)}`
      document.getElementById('story').append(elt)
    } else {
      const elt = document.createElement('hr')
      document.getElementById('story').append(elt)
    }
    console.log(`game.choice('${choice.nodeId}', '${choice.txt}')`)
  }

  say(txt) {
    this.gameView.say(txt)
  }

  sayWith(styleCode, txt) {
    this.gameView.sayWith(styleCode, txt)
  }

  choice(nodeId, txt, data = []) {
    return new Choice(this.resolveScope(nodeId), txt, data)
  }

  chooseFrom(choices, wrapChoices) {
    const game = this
    this.gameView.displayChoices(game, choices, wrapChoices)
  }

  choose(...choices) {
    return this.chooseFrom(choices.slice())
  }

  chooseWrap(...choices) {
    return this.chooseFrom(choices.slice(), true)
  }

  chooseNowrap(...choices) {
    return this.chooseFrom(choices.slice(), false)
  }

  img(url) {
    this.gameView.img(url)
  }

  inventory(returnToNodeId) {
    const game = this
    game.say("You have the following:")
    let choices = []
    for (let key in game.gameNodes) {
      const item = game.gameNodes[key]
      if (item instanceof GameItem && item.location === "=g:player=") {
        choices.push(game.choice(item.nodeId, item.label))
      }
    }
    choices.push(game.choice(this.resolveScope(returnToNodeId), "back"))
    game.chooseFrom(choices)
  }
}

export const game = new Game();
window.game = game;

export function visited(nodeId) {
  return game.isVisited(nodeId)
}

export function back() {
  game.choose(game.choice(game.priorNode(), 'back'));
}

function applyMarkdown(s) {
  if (!s) return s;
  return s.replace(/\*\*\*(.*?)\*\*\*/g, '<b><i>$1</i></b>')
          .replace(/\*\*(.*?)\*\*/g, '<b>$1</b>')
          .replace(/_(.*?)_/g, '<i>$1</i>')
          .replace(/~~(.*?)~~/g, '<del>$1</del>')
          .replace(/\[(.*?)\]\(>(.*?)\)/g, (match, text, nodeId) => {
            const resolvedId = game.resolveScope('=' + nodeId + '=');
            return `<span class="inline-link" onclick="game.step(game.choice('${resolvedId}', '${text.replace(/'/g, "\\'")}'))">${text}</span>`;
          });
}

// Makes it so refreshing the window resets the scroll position.
window.addEventListener('load', () => { history.scrollRestoration = 'manual' });

