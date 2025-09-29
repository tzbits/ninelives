/* story.js */

import {game, GameItem} from "./game.js";

console.log("-- helpers.js loaded")

class Cloak {
  noticed = false;
  location = "=g:player="

  take() {
    return this.location = "=g:player=";
  }

  dropTo(nodeId) {
    return this.location = game.resolveScope(nodeId);
  }

  locationIs(nodeId) {
    return this.location === game.resolveScope(nodeId);
  }

  playerIsWearing() {
    return this.locationIs("=g:player=")
  }

  canDrop() {
    return this.noticed
      && this.playerIsWearing()
      && game.currentNode() == "=cloak:cloakroom=";
  }

  canTake() {
    return !this.playerIsWearing()
      && this.locationIs(game.currentNode())
  }

  isVisible() {
    return this.noticed
      && (this.locationIs(game.currentNode())
          || this.playerIsWearing());
  }
}

class Message {
  isTrampled = false;
}

export const story = {
  cloak: new Cloak(),
  message: new Message(),
  playerChanged: true,
}

window.onload = function () {
  game.step(game.choice('=0=', ''))
}
