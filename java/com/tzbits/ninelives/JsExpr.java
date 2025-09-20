package com.tzbits.ninelives;

import com.google.common.collect.ImmutableList;
import java.util.stream.Collectors;

/**
 * A fluent builder for creating JavaScript-like expressions and
 * statements.  This class provides a set of static factory methods to
 * start expression chains.
 */
public class JsExpr {

  // --- Public API for Fluent Building ---

  /** Starts a new function expression chain with the given name. */
  public static Fnc fnc(String name) {
    return new Fnc(name);
  }

  /** Starts a new operator expression chain with the given name. */
  public static Op op(String name) {
    return new Op(name);
  }

  /** Starts a new ternary expression. */
  public static Ter ter(Expr test, Expr consq, Expr alt) {
    return new Ter(test, consq, alt);
  }

  /** Wraps an expression in a statement. */
  public static Stm stm(Expr expr) {
    return new Stm(expr);
  }

  /** Creates a return statement. */
  public static Ret ret(Expr expr) {
    return new Ret(expr);
  }

  /** Creates an assignment statement. */
  public static Set set(Expr lhs, Expr rhs) {
    return new Set(lhs, rhs);
  }

  /** Creates a code block. */
  public static Blk blk(Js... statements) {
    return new Blk(ImmutableList.copyOf(statements));
  }

  /** Creates a literal string expression. */
  public static Expr str(String value) {
    return new Literal(value, LiteralType.STRING);
  }

  /** Creates a literal numeric expression. */
  public static Expr val(String value) {
    return new Literal(value, LiteralType.NUMBER);
  }

  /** Creates a literal variable expression. */
  public static Expr var(String value) {
    return new Literal(value, LiteralType.VARIABLE);
  }

  /** Creates a keyword expression. */
  public static Kwd kwd(String keyword) {
    return new Kwd(keyword);
  }

  /** Creates a grouped expression, wrapped in parentheses. */
  public static Grp grp(Expr expr) {
    return new Grp(expr);
  }

  /** Creates a sequence of JavaScript elements. */
  public static Seq seq(Js... elements) {
    return new Seq(ImmutableList.copyOf(elements));
  }

  // --- Core Expression Classes ---

  /** Base class for all JavaScript elements. */
  public static abstract class Js {
    /** Returns the JavaScript representation of this element. */
    public abstract String str();
  }

  /** Base class for all JavaScript expressions. */
  public static abstract class Expr extends Js {}

  /** Represents a single JavaScript statement, ending with a semicolon. */
  public static class Stm extends Js {
    private final Expr expr;

    Stm(Expr expr) {
      this.expr = expr;
    }

    @Override
    public String str() {
      return expr.str() + ";\n";
    }
  }

  /** Represents a function call. */
  public static class Fnc extends Expr {
    private final String name;
    private ImmutableList<Expr> args = ImmutableList.of();

    Fnc(String name) {
      this.name = name;
    }

    /** Appends arguments to the function call. */
    public Fnc args(Expr... args) {
      this.args = ImmutableList.copyOf(args);
      return this;
    }

    @Override
    public String str() {
      String argList = args
          .stream()
          .map(Expr::str)
          .collect(Collectors.joining(", "));
      return String.format("%s(%s)", name, argList);
    }
  }

  /** Represents an operator with one or more arguments. */
  public static class Op extends Expr {
    private final String name;
    private ImmutableList<Expr> args = ImmutableList.of();

    Op(String name) {
      this.name = name;
    }

    /** Appends arguments to the operator expression. */
    public Op args(Expr... args) {
      this.args = ImmutableList.copyOf(args);
      return this;
    }

    @Override
    public String str() {
      if (args.isEmpty()) {
        throw new IllegalArgumentException("Op without arguments.");
      }
      if (args.size() == 1) {
        return String.format("%s(%s)", name, args.get(0).str());
      }
      return args
          .stream()
          .map(exp -> String.format("(%s)", exp.str()))
          .collect(Collectors.joining(name));
    }
  }

  /** Represents a ternary (conditional) expression. */
  public static class Ter extends Expr {
    private final Expr test;
    private final Expr consq;
    private final Expr alt;

    Ter(Expr test, Expr consq, Expr alt) {
      this.test = test;
      this.consq = consq;
      this.alt = alt;
    }

    @Override
    public String str() {
      return String.format("(%s) ? (%s) : (%s)", test.str(), consq.str(), alt.str());
    }
  }

  /** Represents a return statement. */
  public static class Ret extends Js {
    private final Expr expr;

    Ret(Expr expr) {
      this.expr = expr;
    }

    @Override
    public String str() {
      return String.format("return %s;\n", expr.str());
    }
  }

  /** Represents an assignment statement. */
  public static class Set extends Js {
    private final Expr lhs;
    private final Expr rhs;

    Set(Expr lhs, Expr rhs) {
      this.lhs = lhs;
      this.rhs = rhs;
    }

    @Override
    public String str() {
      return String.format("%s = %s;\n", lhs.str(), rhs.str());
    }
  }

  /** Represents a code block. */
  public static class Blk extends Js {
    private final ImmutableList<Js> statements;

    Blk(ImmutableList<Js> statements) {
      this.statements = statements;
    }

    @Override
    public String str() {
      return statements.stream()
          .map(Js::str)
          .collect(Collectors.joining("", "{\n", "}"));
    }
  }

  /** Represents a keyword like 'if', 'else', or 'while'. */
  public static class Kwd extends Js {
    private final String keyword;

    Kwd(String keyword) {
      this.keyword = keyword;
    }

    @Override
    public String str() {
      return keyword;
    }
  }

  /** Represents a grouped expression in parentheses. */
  public static class Grp extends Expr {
    private final Expr expr;

    Grp(Expr expr) {
      this.expr = expr;
    }

    @Override
    public String str() {
      return String.format("(%s)", expr.str());
    }
  }

  /** Represents a sequence of JavaScript elements. */
  public static class Seq extends Js {
    private final ImmutableList<Js> elements;

    Seq(ImmutableList<Js> elements) {
      this.elements = elements;
    }

    @Override
    public String str() {
      return elements.stream()
          .map(Js::str)
          .collect(Collectors.joining(" "));
    }
  }

  /** Enum to distinguish between types of literals. */
  private enum LiteralType {
    STRING,
    NUMBER,
    VARIABLE
  }

  /** Represents a terminal literal value like a string, number, or variable. */
  private static class Literal extends Expr {
    private final String value;
    private final LiteralType type;

    Literal(String value, LiteralType type) {
      this.value = value;
      this.type = type;
    }

    @Override
    public String str() {
      if (type == LiteralType.STRING) {
        return String.format("\"%s\"", value);
      }
      return value;
    }
  }
}
