package com.tzbits.ninelives;

import com.google.common.base.Preconditions;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.Streams;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.stream.IntStream;
import java.util.stream.Stream;

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

  public static Transpiler forFile(Path inFile) {
    return new Transpiler(getFileLInes(inFile));
  }

  public static Transpiler forSource(String sourceString) {
    return new Transpiler(ImmutableList.copyOf(sourceString.split("\n")));
  }

  public String transpile() {
    Stream<String> lineStream = fileLines.stream();
    IntStream ordinalStream = IntStream.range(1, fileLines.size() + 1);
    ImmutableList<SourceLine> sourceLines =
        Streams.zip(ordinalStream.boxed(),
                    lineStream,
                    (lineno, line) -> SourceLine.newInstance(line, lineno))
        .filter(sl -> sl.lineType() != LineType.COMMENT)
        .collect(ImmutableList.toImmutableList());
    Preconditions.checkNotNull(sourceLines, "Source lines cannot be null");

    StringBuilder out = new StringBuilder();

    out.append("import {game, back, visited, GameNode} from \"./game.js\";\n");
    out.append("import {story} from \"./story.js\";\n");

    ImmutableList<SourceLine> restLines = sourceLines;
    while (!restLines.isEmpty()) {
      restLines = transpileTopLevel(out, restLines);
    }
    out.append("game.scope = \"g\";"); // restore scope to global at end of file.
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
    switch (sourceLine.lineType()) {
      case NODE:
        if (!isTopLevel) {
          sourceLine.bug("Nested nodes are not possible!");
          // unreachable
          return ImmutableList.of();
        }
        return new TranspilerNode(this).transpile(out, sourceLines);
      case COMMAND:
        return new TranspilerCommand(this).transpile(out, sourceLines);
      case CODE:
        return new TranspilerCode(this).transpile(out, sourceLines);
      case CHOICE:
        if (level == 0) {
          sourceLine.fatalError("Choice (>) found before the beginning of a node.");
          // unreachable
          return ImmutableList.of();
        }
        return new TranspilerChoice(this).transpile(out, sourceLines);
      case TEXT:
        return new TranspilerText(this).transpile(out, sourceLines);
    }
    sourceLine.fatalError("Bug: unhandled line type.");
    // unreachable
    return ImmutableList.of();
  }

  /**
   * Returns the node id in the fully qualified form used in the
   * Game.gameNodes javascript object.
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

  private static ImmutableList<String> getFileLInes(Path path) {
    try {
      return ImmutableList.copyOf(Files.readAllLines(path, StandardCharsets.UTF_8));
    } catch (IOException e) {
      System.err.println("Error reading file: " + e.getMessage());
      System.exit(-1);
      // unreachable
      return  ImmutableList.of();
    }
  }


}
