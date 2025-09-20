// game.js

class GameView {
  constructor() {
    /** The outer div element where each new StoryElt gets appended. */
    this.story = document.getElementById('story');

    /**
     * The current output element, created by startStoryElt.
     * @private {HTMLDivElement}
     */
    this.storyElt = null;
  }

  startStoryElt(timeStep) {
    this.storyElt = this.newStoryElt(timeStep)
    this.story.append(this.storyElt)
  }

  newStoryElt(timeStep) {
    const eltId = `t${timeStep}`
    const ret = document.createElement('div')
    ret.classList.add('StoryElt')
    ret.setAttribute('id', eltId)
    return ret
  }

  finishStoryElt() {
    this.scrollToElt(this.storyElt)
  }

  /** Scrolls to the HTML element, `storyElt`. */
  scrollToElt(storyElt) {
    const bannerAdjust = parseInt(window.getComputedStyle(this.story).marginTop, 10);
    const offsetBottom = storyElt.offsetTop + storyElt.offsetHeight
    window.scrollTo({
      top: storyElt.offsetTop - bannerAdjust, left: 0, behavior: 'smooth'
    })
  }

  say(txt) {
    this.storyElt.innerHTML += `<p>${txt}</p>`
  }

  sayWith(styleCode, txt) {
    this.storyElt.innerHTML += `<p class='${this.codeToStyle(styleCode)}'>${txt}</p>`
  }

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
      if (!choice || !choice.nodeId) { continue }
      const choiceElt = document.createElement("div")
      choiceElt.classList.add('choice')
      const linkElt = document.createElement("span")
      linkElt.classList.add('link')
      linkElt.innerHTML = game.enableDebug ? `${choice.txt} (${choice.nodeId})` : `${choice.txt}`
      linkElt.onclick = function () {
        choicesElt.remove()
        game.step(choice)
      }
      choiceElt.append(linkElt)
      choicesElt.append(choiceElt)
    }
    gameView.storyElt.append(choicesElt)
  }

  removeChoices() {
    var element = document.querySelector('.choices');
    if (element) {
      element.parentNode.removeChild(element);
    }
  }

  img(url) {
    let bannerElt = document.getElementById('banner');
    if (bannerElt !== null) {
      bannerElt.setAttribute('src', url);
    }
  }
}

export class AbstractGameNode {
  constructor(nodeId) {
    this.nodeId = nodeId
    this.location = "none"
    this.label = "GameNode_undefined"
    this.execFn = function(game, choice) {
      game.say(
        `Executing choice but the gameNode exec method for "${choice.nodeId}"`
          + ` is not implemented. Choice text was: "${choice.txt}".`)
    }
  }
  exec(game, choice) { return this.execFn(game, choice) }
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
}

export class GameNode extends AbstractGameNode {
  constructor(nodeId) {
    super(nodeId)
  }
}

class Player extends AbstractGameNode {
  constructor() {
    super("=g:player=")
    // Player always starts at node "=g:0="
    this.location = "=g:0="
    this.label = "yourself"
  }
}

export class GameItem extends AbstractGameNode {
  constructor(nodeId, label, location) {
    super(nodeId)
    this.location = location
    this.label = label
  }
}

class Choice {
  constructor(toNodeId, txt) {
    this.nodeId = toNodeId
    this.txt = txt
  }
}

class Game {
  constructor() {
    /** current scope for functions that use unqualified node ids. */
    this.scope = "g";  // g for global scope

    /** Key that game state is stored under. */
    this.gameDataKey = "";

    /** Game state that can be updated by game nodes. */
    this.state = {};

    /** Whether to randomize the order of choices. Defaults to true. */
    this.useRandomChoiceOrder = true;

    /**
     * Whether to put the choices on a wrapping horizontal vs listing
     * them vertically.
     */
    this.wrapChoices = true;

    /** Includes debug info when clicking through the game. */
    this.enableDebug = false

    /** Set this function to run on load and take control of stepping through a path in the story. */
    this.debugOnLoadFn = null

    this.gameView = new GameView()

    const thePlayer = new Player()
    this.player = thePlayer

    /**
     * Node handlers indexed by node id.
     * @private {Object<string, Function>}
     */
    this.gameNodes = { '=g:player=': thePlayer }

    /**
     * The current time, a strictly increasing int.
     * @private {number}
     */
    this.timeStep = 0

    /**
     * The node id that was last run.
     * '' is the starting state before any nodes have run. First node is '=0='.
     * @private {string}
     */
    this.atNodeId = ''

    /** Node ids are added to this array as they are stepped into. */
    this.gamePath = []

    /**
     * The count of visits to particular nodes, keyed by node id.
     * @private {Object<string, number>}
     */
    this.nodeVisits = {}

    /** Place to hold game items. */
    this.items = {}
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
    let retrievedString = "";
    try {
      retrievedString = localStorage.getItem(gameDataKey);
      const retrievedState = JSON.parse(retrievedString);
      if (typeof retrievedState === "object" && retrievedState !== null) {
        this.state = retrievedState;
      } else {
        throw `Failed to parse game state from "${retrievedString}"`;
      }
    } catch (err) {
      console.log(`Error loading game state from (${retrievedString}): ${err}.`)
      this.state = {};
    }
  }

  saveState() {
    if (this.gameDataKey) {
      localStorage.setItem(this.gameDataKey, JSON.stringify(this.state));
    }
  }

  /**
   * Move one step forward in time and call the game node for `toNodeId`.
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
    // TODO: how to get choice args here?
    this.gameNodes[this.atNodeId].exec(this, choice)

    this.gameView.finishStoryElt()
  }

  /**
   * Returns the nodeId set that `step` is running. This is meant to be referred to
   * from other downstream methods called within the nodes's exec method.
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
    const ret = this.nodeVisits[id];
    if (!ret) {
      throw `${nodeId} (${id}) not found.`;
    }
    return ret;
  }

  isVisited(nodeId) {
    const id = this.resolveScope(nodeId);
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

  outputDebugInfo(choice) {
    const elt = document.createElement('pre')
    elt.innerText = `choice: ${JSON.stringify(choice)}`
    document.getElementById('story').append(elt)
    console.log(`game.choice('${choice.nodeId}', '${choice.txt}')`)
  }

  say(txt) {
    this.gameView.say(txt)
  }

  sayWith(styleCode, txt) {
    this.gameView.sayWith(styleCode, txt)
  }

  choice(nodeId, txt) {
    return new Choice(this.resolveScope(nodeId), txt)
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
      if (item instanceof GameItem && item.location == "=g:player=") {
        choices.push(game.choice(item.nodeId, item.label))
      }
    }
    choices.push(game.choice(this.resolveScope(returnToNodeId), "back"))
    game.chooseFrom(choices)
  }
}
export const game = new Game();

window.addEventListener('beforeunload', function (event) {
  game.saveState();
});

export function visited(nodeId) {
  return game.isVisited(nodeId)
}

export function back() {
  game.choose(game.choice(game.priorNode(), '< back'));
}
