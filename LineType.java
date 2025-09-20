package com.tzbits.ninelives;

import java.util.Arrays;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

public enum LineType {
  NODE('='),
  CHOICE('>'),
  CODE('|'),
  COMMAND('!'),
  COMMENT('#'),
  TEXT('\0');

  private static final Map<Character, LineType> ENUM_MAP;

  static {
    ENUM_MAP = Arrays.stream(LineType.values())
        .collect(Collectors.toMap(lineType -> lineType.character, Function.identity()));
  }

  private final char character;

  LineType(char character) {
    this.character = character;
  }

  public static LineType forCharacter(char character) {
    return ENUM_MAP.getOrDefault(character, TEXT);
  }
}
