# Nine Lives

Nine lives is a Javascript dsl for creating choice-based interactive fiction.

# TODO

* add a new conditional choice format
  * >some-node-id !if isThisTrue(foo) !then "go happily" !else
* support line continuation with \\\n
* redo inventory: make it something you import separately
  * e.g. pair down game.js to the bare minimum
  * provide other modules that can be imported into story.js
