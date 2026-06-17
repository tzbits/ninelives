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
  displayChoices(game, choices) {
    const gameView = this
    const choicesElt = document.createElement("div")
    choicesElt.classList.add(game.wrapChoices ? 'wrapped-choices' : 'choices')
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
    const element = document.querySelector('.choices');
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
    return game.nodeVisits[this.nodeId] === 1;
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

  /** Key that game state is stored under. */
  gameDataKey = "";

  /**
   * All game states.
   * Persisted to localStorage as one blob.
   * @type {object}
   */
  state = {};

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

  /** Set this function to run on load and take control of stepping through a path in the story. */
  debugOnLoadFn = null

  /**
   * The current time, a strictly increasing int.
   * @private {number}
   */
  timeStep = 0

  /**
   * The node id that was last run.
   * '' is the starting state before any nodes have run. First node is '=0='.
   * @private {string}
   */
  atNodeId = ''

  /** Node ids are added to this array as they are stepped into. */
  gamePath = []

  /**
   * The count of visits to particular nodes, keyed by node id.
   * @type {Record<string, number>}
   */
  nodeVisits = {}

  /** Place to hold game items. */
  items = {}

  constructor() {
    this.gameNodes = {'=g:player=': /** @type {AbstractGameNode} */ this.player}
  }

  reset() {
    this.nodeVisits = {};
    this.gamePath = [];
    this.timeStep = 0;
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

  loadState(gameDataKey) {
    this.gameDataKey = gameDataKey;
    try {
      const raw = localStorage.getItem(gameDataKey);
      const parsed = JSON.parse(raw);
      if (parsed && typeof parsed === "object") {
        this.state = parsed;
      }
    } catch (e) {
      console.log(`Could not load state for ${gameDataKey}: ${e}`);
    }
  }

  saveState() {
    if (this.gameDataKey && this.gameDataKey != "") {
      localStorage.setItem(this.gameDataKey, JSON.stringify(this.state));
    } else {
      throw Error(
          `No gameDataKey set for game.saveState(). Try starting your game with game.loadState("MyGameDataKey");`)
    }
  }

  /**
   * Move one step forward in time and call the game node for `toNodeId`.
   * @param {Choice} choice
   */
  step(choice) {
    if (this.enableDebug) {
      this.outputDebugInfo(choice)
    }
    let toNodeId = this.resolveScope(choice.nodeId)
    this.atNodeId = toNodeId
    this.gamePath.push(toNodeId)
    this.timeStep = this.timeStep + 1
    if (toNodeId in this.nodeVisits) {
      this.nodeVisits[toNodeId]++
    } else {
      this.nodeVisits[toNodeId] = 1
    }

    this.gameView.startStoryElt(this.timeStep)

    if (!(this.atNodeId in this.gameNodes)) {
      throw `The node ${this.atNodeId} was not found by game.step`
    }

    // @type {AbstractGameNode}
    const nd = this.gameNodes[this.atNodeId];
    nd.exec(this, choice);
    this.gameView.showStoryElt();
  }

  getNode(id) {
    return this.gameNodes[this.resolveScope(id)];
  }

  /**
   * Returns the nodeId set that `step` is running. This is meant to be
   * referred to * from other downstream methods called within the node's
   * exec method.
   */
  currentNode() {
    return this.atNodeId
  }

  priorNode() {
    return this.gamePath.slice(-2)[0]
  }

  visitCount(nodeId) {
    if (nodeId === undefined) {
      return this.nodeVisits[this.currentNode()]
    }
    const id = this.resolveScope(nodeId);
    const nd = this.gameNodes[id];
    if (!nd) {
      throw `${nodeId} (${id}) not found.`;
    }
    const ret = this.nodeVisits[id];
    return ret || 0;
  }

  isVisited(nodeId) {
    const id = this.resolveScope(nodeId);
    const nd = this.gameNodes[id];
    if (!nd) {
      throw `${nodeId} (${id}) not found.`;
    }
    return id in this.nodeVisits && this.nodeVisits[id] > 0
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

  chooseFrom(choices) {
    const game = this
    this.gameView.displayChoices(game, choices)
  }

  choose(...choices) {
    return this.chooseFrom(choices.slice())
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
  game.choose(game.choice(game.priorNode(), '< back'));
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

window.addEventListener('beforeunload', function (ignoreEvent) {
  if (this.gameDataKey && this.gameDataKey != "") {
    game.saveState();
  }
});
