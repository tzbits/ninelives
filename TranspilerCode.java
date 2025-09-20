package com.tzbits.ninelives;

import static java.lang.Integer.min;

import com.google.common.collect.ImmutableList;

public class TranspilerCode {

  private final Transpiler transpiler;

  public TranspilerCode(Transpiler transpiler) {
    this.transpiler = transpiler;
  }

  public ImmutableList<SourceLine> transpile(
      StringBuilder out, ImmutableList<SourceLine> sourceLines) {
    return transpile(out, sourceLines, "");
  }

  public ImmutableList<SourceLine> transpile(
      StringBuilder out, ImmutableList<SourceLine> sourceLines, String indent) {
    for (int i = 0; i < sourceLines.size(); i++) {
      SourceLine sourceLine = sourceLines.get(i);
      if (!sourceLine.lineType().equals(LineType.CODE)) {
        return sourceLines.subList(i, sourceLines.size());
      }
      out.append(indent);
      out.append(sourceLine.line().substring(min(sourceLine.line().length(), 2))); // remove "| "
      out.append("\n");
    }
    return ImmutableList.of();
  }
}
