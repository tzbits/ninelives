package com.tzbits.ninelives;

import com.google.common.collect.ImmutableList;

public class TranspilerText {

  private final Transpiler transpiler;

  public TranspilerText(Transpiler transpiler) {
    this.transpiler = transpiler;
  }

  public ImmutableList<SourceLine> transpile(
      StringBuilder out, ImmutableList<SourceLine> sourceLines) {
    int j = firstNonEmpty(sourceLines);

    if (j == sourceLines.size() || !sourceLines.get(j).isType(LineType.TEXT)) {
      // Nothing to say.
      return sourceLines.subList(j, sourceLines.size());
    }

    out.append("game.say(`");

    for (int i = j; i < sourceLines.size(); i++) {
      SourceLine line = sourceLines.get(i);

      if (!line.isType(LineType.TEXT) || line.isEmpty()) {
        out.append("`);\n");
        return sourceLines.subList(i, sourceLines.size());
      }

      if (i != j) {
        out.append("\n");
      }
      out.append(line.line());
    }

    out.append("`);\n");
    return ImmutableList.of();
  }

  private static int firstNonEmpty(ImmutableList<SourceLine> sourceLines) {
    for (int i = 0; i < sourceLines.size(); i++) {
      SourceLine line = sourceLines.get(i);
      if (!line.isEmpty() && line.isType(LineType.TEXT)) {
        return i;
      }
      if (!line.isType(LineType.TEXT)) {
        return i;
      }
    }
    return sourceLines.size();
  }
}
