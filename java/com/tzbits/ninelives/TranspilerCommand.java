package com.tzbits.ninelives;

import com.google.common.collect.ImmutableList;

public class TranspilerCommand {

  record Cmd(String name, String body) {}

  private final Transpiler transpiler;

  public TranspilerCommand(Transpiler transpiler) {
    this.transpiler = transpiler;
  }

  public ImmutableList<SourceLine> transpile(
      StringBuilder out, ImmutableList<SourceLine> sourceLines) {
    String line = sourceLines.getFirst().line();
    Cmd cmd = parseCmd(line);

    if (cmd.name.equals("img")) {
      String fmt = "game.img(\"%s\");\n";
      out.append(String.format(fmt, cmd.body));
      return sourceLines.subList(1, sourceLines.size());
    }

    if (cmd.name.equals("scope")) {
      transpiler.scope = cmd.body;
      String fmt = "game.scope = \"%s\";\n";
      out.append(String.format(fmt, cmd.body));
      return sourceLines.subList(1, sourceLines.size());
    }

    String fmt = "game.sayWith(\"%s\", `%s`);\n";
    out.append(String.format(fmt, cmd.name, cmd.body));
    return sourceLines.subList(1, sourceLines.size());
  }

  private static Cmd parseCmd(String line) {
    int len = line.length();
    int cmdEnd = line.indexOf(' ');
    String name = cmdEnd == -1
        ? line.substring(1, len)
        : line.substring(1, cmdEnd);
    String body = cmdEnd == -1
        ? ""
        : line.substring(cmdEnd, len).trim();
    return new Cmd(name, body);
  }
}
