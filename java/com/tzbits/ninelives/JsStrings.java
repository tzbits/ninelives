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
    return s;
  }

  /** Escapes text for inclusion in a double-quoted JS string. */
  static String forDoubleQuoted(String s) {
    return s.replace("\\", "\\\\").replace("\"", "\\\"");
  }
}
