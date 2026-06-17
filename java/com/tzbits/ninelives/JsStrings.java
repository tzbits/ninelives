package com.tzbits.ninelives;

/** Escaping for the points where 9l text meets generated JavaScript. */
final class JsStrings {
  private JsStrings() {}

  /**
   * Prepares a string for inclusion in a JS template literal.
   *
   * <p>Returns the string as is to allow 9l authors to use
   * interpolation and nested template literals.
   */
  static String forTemplateLiteral(String s) {
    java.util.List<String> interpolations = new java.util.ArrayList<>();
    StringBuilder placeholderString = new StringBuilder();

    int last = 0;
    int pos = s.indexOf("${");
    while (pos != -1) {
      placeholderString.append(s.substring(last, pos));

      // Find the end of the interpolation
      int braceCount = 1;
      int end = -1;
      for (int i = pos + 2; i < s.length(); i++) {
        char c = s.charAt(i);
        if (c == '{') {
          braceCount++;
        } else if (c == '}') {
          braceCount--;
          if (braceCount == 0) {
            end = i;
            break;
          }
        }
      }

      if (end != -1) {
        String interpolation = s.substring(pos, end + 1);
        interpolations.add(interpolation);
        placeholderString.append("\u0000").append(interpolations.size() - 1).append("\u0000");
        last = end + 1;
        pos = s.indexOf("${", last);
      } else {
        throw new IllegalArgumentException("Unclosed interpolation ${...} in string: " + s);
      }
    }
    placeholderString.append(s.substring(last));

    String result = applyMarkdown(placeholderString.toString());

    for (int i = 0; i < interpolations.size(); i++) {
      result = result.replace("\u0000" + i + "\u0000", interpolations.get(i));
    }

    return result;
  }

  private static String applyMarkdown(String s) {
    return s.replaceAll("\\*\\*\\*(.*?)\\*\\*\\*", "<b><i>$1</i></b>")
        .replaceAll("\\*\\*(.*?)\\*\\*", "<b>$1</b>")
        .replaceAll("_(.*?)_", "<i>$1</i>")
        .replaceAll("~~(.*?)~~", "<del>$1</del>");
  }

  /** Escapes text for inclusion in a double-quoted JS string. */
  static String forDoubleQuoted(String s) {
    return s.replace("\\", "\\\\").replace("\"", "\\\"");
  }
}
