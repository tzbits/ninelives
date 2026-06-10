package com.tzbits.ninelives;

/** Escaping for the points where 9l text meets generated JavaScript. */
final class JsStrings {
  private JsStrings() {}

  /**
   * Escapes text for inclusion in a JS template literal. Backticks are
   * escaped; "${" and backslashes are intentionally left alone so 9l
   * authors can interpolate variables (e.g. ${howBad} in cloak.9l).
   */
  static String forTemplateLiteral(String s) {
    return s.replace("`", "\\`");
  }

  /** Escapes text for inclusion in a double-quoted JS string. */
  static String forDoubleQuoted(String s) {
    return s.replace("\\", "\\\\").replace("\"", "\\\"");
  }
}
