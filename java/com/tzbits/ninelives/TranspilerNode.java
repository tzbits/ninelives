package com.tzbits.ninelives;

import com.google.common.base.Preconditions;
import com.google.common.collect.ImmutableList;

public class TranspilerNode {
  private final Transpiler transpiler;

  public TranspilerNode(Transpiler transpiler) {
    this.transpiler = transpiler;
  }

  public ImmutableList<SourceLine> transpile(
      StringBuilder out, ImmutableList<SourceLine> sourceLines) {
    SourceLine nodeLine = sourceLines.getFirst();
    ImmutableList<SourceLine> rest = sourceLines.subList(1, sourceLines.size());
    String id = parseId(nodeLine);

    out.append(String.format("\n/* %s */\n", nodeLine.line()));
    out.append(String.format("game.gameNodes['%s'] =\n", id));
    out.append(String.format("new GameNode('%s').setExecFn(function(game, choice) {\n", id));
    out.append(String.format("game.player.location = '%s';\n", id));
    out.append(String.format("game.scope = '%s';\n", transpiler.scope));

    Preconditions.checkNotNull(rest, "rest cannot be null");

    while (!rest.isEmpty()
           && !rest.getFirst().lineType().equals(LineType.NODE)) {
      rest = transpiler.transpileInNode(out, rest);
    }
    out.append("game.scope = 'g';\n");
    out.append("});\n");

    return rest;
  }

  public String parseId(SourceLine nodeLine) {
    if (!nodeLine.lineType().equals(LineType.NODE)) {
      nodeLine.bug("Expected node line.");
    }
    if (!nodeLine.line().startsWith("=")) {
      nodeLine.bug("Encountered node line that does not start with =.");
    }
    String line = nodeLine.line();

    // Find the index of the first '=' and the second '='
    int firstEquals = line.indexOf('=');
    int secondEquals = line.indexOf('=', firstEquals + 1);

    // Ensure both '=' signs are found and in the correct order
    if (firstEquals != -1 && secondEquals != -1 && secondEquals > firstEquals) {
      // Extract the substring, excluding the two '='
      String id = line.substring(firstEquals + 1, secondEquals);
      return transpiler.resolveNodeId(id);
    }

    nodeLine.fatalError("TranspilerNode failed to parse an id from node line.");
    return "";
  }
}
