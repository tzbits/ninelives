package com.tzbits.ninelives;

import com.google.auto.value.AutoValue;

@AutoValue
public abstract class SourceLine {

  public abstract LineType lineType();

  public abstract String line();

  public abstract int lineNumber();

  public static SourceLine newInstance(String line, int lineNumber) {
    if (line.isEmpty()) {
      return new AutoValue_SourceLine(LineType.TEXT, line, lineNumber);
    }
    if (line.charAt(0) == '\\') {
      return new AutoValue_SourceLine(LineType.TEXT, line.substring(1), lineNumber);
    }
    // The node indicator, '=', is a special case, the node id must begin and end with '=',
    // so we want to treat lines like "== win ? 'happy' : 'sad'} days ahead" as text.
    if (line.startsWith("=") && !line.matches("^=[^ ]+=.*")) {
      return new AutoValue_SourceLine(LineType.TEXT, line.substring(1), lineNumber);
    }
    return new AutoValue_SourceLine(LineType.forCharacter(line.charAt(0)), line, lineNumber);
  }

  public boolean isEmpty() {
    return line().trim().isEmpty();
  }

  public boolean isType(LineType type) {
    return lineType().equals(type);
  }

  public void fatalError(String message) {
    System.err.printf("%d: %s%n", lineNumber(), message);
    throw new IllegalStateException(message);
  }

  public void bug(String message) {
    System.err.printf("%d: Bug: %s%n", lineNumber(), message);
    throw new RuntimeException(message);
  }

}
