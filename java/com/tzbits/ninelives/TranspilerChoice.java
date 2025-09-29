package com.tzbits.ninelives;

import com.google.common.collect.ImmutableList;

public class TranspilerChoice {
  private final Transpiler transpiler;

  public TranspilerChoice(Transpiler transpiler) {
    this.transpiler = transpiler;
  }

  public ImmutableList<SourceLine> transpile(
      StringBuilder out, ImmutableList<SourceLine> sourceLines) {
    out.append("game.choose(");
    int len = sourceLines.size();
    for (int i = 0; i < len; i++) {
      SourceLine sourceLine = sourceLines.get(i);

      if (!sourceLine.isType(LineType.CHOICE)) {
        out.append(");\n");
        return  sourceLines.subList(i, sourceLines.size());
      }

      boolean isLast = i == len - 1 || !sourceLines.get(i + 1).isType(LineType.CHOICE);
      out.append("\n").append(transpileChoice(sourceLine.line(), isLast));
    }

    out.append(");\n");
    return ImmutableList.of();
  }

  private String transpileChoice(String line, boolean isLast) {
    // Format:
    //  >some-node-id go there
    //  >some-node-id ? isThisTrue(foo); "go there"
    int spacePos = line.indexOf(' ');
    int idEnd = spacePos == -1 ? line.length() : spacePos;
    String nodeId = transpiler.resolveNodeId(line.substring(1, idEnd));
    String rest = line.substring(idEnd).trim();
    String txt = rest.isEmpty() ? "continue" :  rest;
    int qPos = rest.isEmpty() ? -1 : rest.charAt(0) == '?' ? 0 : -1;
    int scPos = rest.isEmpty() ? -1 : rest.indexOf(';');

    if (qPos == -1 && scPos == -1) {
      return String.format("game.choice(\"%s\", `%s`)%s",
                           nodeId,
                           txt,
                           isLast ? "" : ",");
    }

    if (qPos == -1 && scPos != -1) {
      return String.format("game.choice(\"%s\", `%s`, %s)%s",
                           nodeId,
                           txt.substring(0, scPos).trim(),
                           txt.substring(scPos + 1).trim(),
                           isLast ? "" : ",");
    }

    String condition = rest.substring(qPos + 1, scPos);
    txt = rest.substring(scPos + 1).trim();
    // Note: second arg to choice is not quoted
    // so the user can put js expressions there.
    return String.format(
        "(%s) ? game.choice(\"%s\", %s) : false%s",
        condition.trim(),
        nodeId,
        txt,
        isLast ? "" : ",");
  }
}
