/* story.js */

import {game, GameItem} from "./game.js";

console.log("-- helpers.js loaded")

class GameState {
  constructor(id, defaults = {}) {
    this.objId = id;
    const proxy = new Proxy(this, {
      get(target, prop) {
        if (prop in target || typeof prop === 'symbol' || prop === 'then') {
          return target[prop];
        }
        return game.state[target.key(prop)];
      },
      set(target, prop, value) {
        if (prop in target) {
          target[prop] = value;
          return true;
        }
        game.state[target.key(prop)] = value;
        return true;
      }
    });

    for (const [prop, value] of Object.entries(defaults)) {
      if (game.state[this.key(prop)] === undefined) {
        game.state[this.key(prop)] = value;
      } else {
        throw new Error(`State property "${prop}" for object "${id}" is already initialized`);
      }
    }

    return proxy;
  }

  key(prop) {
    return "_" + this.objId + '_' + prop;
  }

}

class Cloak extends GameState {
  constructor(defaults) {
    super("cloak", defaults);
  }

  take() {
    this.location = "=g:player=";
    return this;
  }

  dropTo(nodeId) {
    this.location = game.resolveScope(nodeId);
    return this;
  }

  isLocation(nodeId) {
    return this.location === game.resolveScope(nodeId);
  }

  isPlayerWearing() {
    return this.isLocation("=g:player=")
  }

  isDropAllowed() {
    return this.noticed
      && this.isPlayerWearing()
      && game.currentNode() == "=cloak:cloakroom=";
  }

  isTakeAllowed() {
    return !this.isPlayerWearing()
      && this.isLocation(game.currentNode())
  }

  isVisible() {
    return this.noticed
      && (this.isLocation(game.currentNode())
          || this.isPlayerWearing());
  }
}

export const story = {
  cloak: new Cloak({
    noticed: false,
    location: "=g:player="
  }),
  message: new GameState("message", {trampled: false}),
  player: new GameState("player", {inventoryChanged: true}),
}


window.onload = function () {
  story.message.trampled = false;
  story.player.inventoryChanged = true;
  if (game.restoreFromStorage()) {
    return;
  }
  game.step(game.choice('=0=', ''))
}
