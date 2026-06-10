package com.tzbits.ninelives;

import com.google.common.collect.ImmutableList;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;

public class Transpiler {

  /**
   * The declared scope for nodes. See !scope handler in
   * TranspilerCommand, TranspilerNode.parseId, and
   * TranspilerChoice.transpileChoice.
   */
  public String scope = "g";  // g for global scope

  private final ImmutableList<String> fileLines;

  private Transpiler(ImmutableList<String> fileLines) {
    this.fileLines = fileLines;
  }

  public static Transpiler forFile(Path inFile) throws IOException {
    return new Transpiler(ImmutableList.copyOf(Files.readAllLines(inFile, StandardCharsets.UTF_8)));
  }

  public static Transpiler forSource(String sourceString) {
    return new Transpiler(ImmutableList.copyOf(sourceString.split("\\R")));
  }

  public String transpile() {
    ImmutableList.Builder<SourceLine> builder = ImmutableList.builder();
    for (int i = 0; i < fileLines.size(); i++) {
      SourceLine sourceLine = SourceLine.newInstance(fileLines.get(i), i + 1);
      if (sourceLine.lineType() != LineType.COMMENT) {
        builder.add(sourceLine);
      }
    }
    ImmutableList<SourceLine> sourceLines = builder.build();

    StringBuilder out = new StringBuilder();

    out.append("import {game, back, visited, GameNode} from \"./game.js\";\n");
    out.append("import {story} from \"./story.js\";\n");

    ImmutableList<SourceLine> restLines = sourceLines;
    while (!restLines.isEmpty()) {
      restLines = transpileTopLevel(out, restLines);
    }
    out.append("game.scope = \"g\";\n"); // restore scope to global at end of file.
    return out.toString();
  }

  public ImmutableList<SourceLine> transpileTopLevel(
      StringBuilder out, ImmutableList<SourceLine> sourceLines) {
    return transpile(out, sourceLines, /*level=*/ 0);
  }

  public ImmutableList<SourceLine> transpileInNode(
      StringBuilder out, ImmutableList<SourceLine> sourceLines) {
    return transpile(out, sourceLines, /*level=*/ 1);
  }

  /**
   * Writes transpilation of {@code sourceLines} to {@code out}.
   *
   * @param level is 0 if this is the top-level and 1 if part of a node
   * @return the remaining lines to be transpiled
   */
  public ImmutableList<SourceLine> transpile(
      StringBuilder out, ImmutableList<SourceLine> sourceLines, int level) {
    boolean isTopLevel = level == 0;
    if (sourceLines.isEmpty()) {
      return ImmutableList.of();
    }
    SourceLine sourceLine = sourceLines.getFirst();
      return switch (sourceLine.lineType()) {
          case NODE -> {
              if (!isTopLevel) {
                  throw sourceLine.bug("Nested nodes are not possible!");
              }
              yield transpileNode(out, sourceLines);
          }
          case COMMAND -> transpileCommand(out, sourceLines);
          case CODE -> transpileCode(out, sourceLines);
          case CHOICE -> {
              if (level == 0) {
                  throw sourceLine.fatalError("Choice (>) found before the beginning of a node.");
              }
              yield transpileChoice(out, sourceLines);
          }
          case TEXT -> transpileText(out, sourceLines);
          default -> throw sourceLine.bug("Unhandled line type.");
      };
  }

  private ImmutableList<SourceLine> transpileNode(
      StringBuilder out, ImmutableList<SourceLine> sourceLines) {
    SourceLine nodeLine = sourceLines.getFirst();
    ImmutableList<SourceLine> rest = sourceLines.subList(1, sourceLines.size());
    String id = JsStrings.forDoubleQuoted(parseNodeId(nodeLine));

    out.append(String.format("\n/* %s */\n", nodeLine.line()));
    out.append(String.format("game.gameNodes[\"%s\"] =\n", id));
    out.append(String.format("new GameNode(\"%s\").setExecFn(function(game, choice) {\n", id));
    out.append(String.format("game.player.location = \"%s\";\n", id));
    out.append(String.format("game.scope = \"%s\";\n", JsStrings.forDoubleQuoted(scope)));

    while (!rest.isEmpty()
           && !rest.getFirst().lineType().equals(LineType.NODE)) {
      rest = transpileInNode(out, rest);
    }
    out.append("game.scope = \"g\";\n");
    out.append("});\n");

    return rest;
  }

  private String parseNodeId(SourceLine nodeLine) {
    String line = nodeLine.line();
    int secondEquals = line.indexOf('=', 1);
    if (!nodeLine.isType(LineType.NODE) || !line.startsWith("=") || secondEquals == -1) {
      throw nodeLine.bug("Expected node line of the form =id=.");
    }
    return resolveNodeId(line.substring(1, secondEquals));
  }

  private ImmutableList<SourceLine> transpileChoice(
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
      out.append("\n").append(renderChoice(sourceLine, isLast));
    }

    out.append(");\n");
    return ImmutableList.of();
  }

  private String renderChoice(SourceLine sourceLine, boolean isLast) {
    // Format:
    //  >some-node-id go there
    //  >some-node-id ? isThisTrue(foo); "go there"
    String line = sourceLine.line();
    int spacePos = line.indexOf(' ');
    int idEnd = spacePos == -1 ? line.length() : spacePos;
    String nodeId = JsStrings.forDoubleQuoted(resolveNodeId(line.substring(1, idEnd)));
    String rest = line.substring(idEnd).trim();
    String comma = isLast ? "" : ",";

    if (rest.isEmpty() || rest.charAt(0) != '?') {
      // Plain choice; any ';' is just prose.
      String txt = rest.isEmpty() ? "continue" : rest;
      return String.format("game.choice(\"%s\", `%s`)%s",
                           nodeId,
                           JsStrings.forTemplateLiteral(txt),
                           comma);
    }

    int scPos = rest.indexOf(';');
    if (scPos == -1) {
      throw sourceLine.fatalError("Conditional choice (?) is missing ';'.");
    }
    String condition = rest.substring(1, scPos).trim();
    String txt = rest.substring(scPos + 1).trim();
    // Note: second arg to choice is not quoted
    // so the user can put js expressions there.
    return String.format(
        "(%s) ? game.choice(\"%s\", %s) : false%s",
        condition,
        nodeId,
        txt,
        comma);
  }

  private ImmutableList<SourceLine> transpileCode(
      StringBuilder out, ImmutableList<SourceLine> sourceLines) {
    for (int i = 0; i < sourceLines.size(); i++) {
      SourceLine sourceLine = sourceLines.get(i);
      if (!sourceLine.lineType().equals(LineType.CODE)) {
        return sourceLines.subList(i, sourceLines.size());
      }
      String code = sourceLine.line();
      if (code.startsWith("| ")) {
        code = code.substring(2);
      } else if (code.startsWith("|")) {
        code = code.substring(1); // tolerate "|}" with no space
      }
      out.append(code).append("\n");
    }
    return ImmutableList.of();
  }

  private record Cmd(String name, String body) {}

  private ImmutableList<SourceLine> transpileCommand(
      StringBuilder out, ImmutableList<SourceLine> sourceLines) {
    String line = sourceLines.getFirst().line();
    Cmd cmd = parseCmd(line);

    if (cmd.name.equals("img")) {
      String fmt = "game.img(\"%s\");\n";
      out.append(String.format(fmt, JsStrings.forDoubleQuoted(cmd.body)));
      return sourceLines.subList(1, sourceLines.size());
    }

    if (cmd.name.equals("scope")) {
      scope = cmd.body;
      String fmt = "game.scope = \"%s\";\n";
      out.append(String.format(fmt, cmd.body));
      return sourceLines.subList(1, sourceLines.size());
    }

    String fmt = "game.sayWith(\"%s\", `%s`);\n";
    out.append(String.format(fmt,
        JsStrings.forDoubleQuoted(cmd.name), JsStrings.forTemplateLiteral(cmd.body)));
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

  private ImmutableList<SourceLine> transpileText(
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
      out.append(JsStrings.forTemplateLiteral(line.line()));
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

  /**
   * Returns the node id in the fully qualified form used in the
   * Game.gameNodes JavaScript object.
   */
  public String resolveNodeId(String id) {
    int sep = id.indexOf(':');
    if (sep != -1) {
      // already fully qualified.
      return "=" + id + "=";
    }
    if (scope.isEmpty()) {
      // no scope in use
      return "=" + id + "=";
    }
    return "=" + scope + ":" + id + "=";
  }
}
