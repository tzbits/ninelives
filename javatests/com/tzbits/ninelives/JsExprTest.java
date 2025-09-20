package com.tzbits.ninelives;

import static com.google.common.truth.Truth.assertThat;
import static org.junit.Assert.assertThrows;

import static com.tzbits.ninelives.JsExpr.Js;
import static com.tzbits.ninelives.JsExpr.fnc;
import static com.tzbits.ninelives.JsExpr.op;
import static com.tzbits.ninelives.JsExpr.ter;
import static com.tzbits.ninelives.JsExpr.stm;
import static com.tzbits.ninelives.JsExpr.ret;
import static com.tzbits.ninelives.JsExpr.set;
import static com.tzbits.ninelives.JsExpr.blk;
import static com.tzbits.ninelives.JsExpr.str;
import static com.tzbits.ninelives.JsExpr.val;
import static com.tzbits.ninelives.JsExpr.var;
import static com.tzbits.ninelives.JsExpr.kwd;
import static com.tzbits.ninelives.JsExpr.grp;
import static com.tzbits.ninelives.JsExpr.seq;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.JUnit4;

@RunWith(JUnit4.class)
public class JsExprTest {

  @Test
  public void testFnc() {
    // Tests a simple function call with string and variable arguments
    String expected = "console.log(\"Hello, World!\", x)";
    String generated = fnc("console.log").args(
        str("Hello, World!"), var("x"))
        .str();
    assertThat(generated).isEqualTo(expected);
  }

  @Test
  public void testFnc_noArgs() {
    // Tests a function call with no arguments
    assertThat(fnc("myFunction").str()).isEqualTo("myFunction()");
  }

  @Test
  public void testOp() {
    // Tests a binary operation with multiple arguments
    assertThat(op("*").args(val("2"), var("pi"), var("r")).str()).isEqualTo("(2)*(pi)*(r)");
  }

  @Test
  public void testOp_singleArg() {
    // Tests an operator with a single argument, ensuring no extra parentheses
    assertThat(op("-").args(var("x")).str()).isEqualTo("-(x)");
  }

  @Test
  public void testOp_noArgs() {
    // Tests that an operator without arguments throws an exception
    assertThrows(IllegalArgumentException.class, () -> op("+").str());
  }

  @Test
  public void testTer() {
    // Tests a ternary operator expression
    String expected = "((x)==(0)) ? (\"zero\") : (\"not zero\")";
    Js generated = ter(op("==")
                       .args(
                           var("x"),
                           val("0")),
                       str("zero"),
                       str("not zero"));
    assertThat(generated.str()).isEqualTo(expected);
  }

  @Test
  public void testStm() {
    // Tests a simple statement
    assertThat(stm(fnc("alert").args(str("Hello"))).str()).isEqualTo("alert(\"Hello\");\n");
  }

  @Test
  public void testRet() {
    // Tests a return statement
    assertThat(ret(val("true")).str()).isEqualTo("return true;\n");
  }

  @Test
  public void testSet() {
    // Tests an assignment statement
    assertThat(set(var("a"), op("+").args(var("b"), val("10"))).str()).isEqualTo("a = (b)+(10);\n");
  }

  @Test
  public void testBlk() {
    // Tests a block containing multiple statements
    String expected = "{\na = 1;\nb = 2;\n}";
    String generated = blk(
        set(var("a"), val("1")),
        set(var("b"), val("2"))).str();
    assertThat(generated).isEqualTo(expected);
  }

  @Test
  public void testIfStatement() {
    // Tests the complex 'if' statement logic
    String expected = "if ((x)>(10)) {\nconsole.log(\"x is bigger than 10.\");\n}";
    Js generated = seq(
        kwd("if"),
        grp(op(">").args(var("x"), val("10"))),
        blk(
            stm(fnc("console.log").args(str("x is bigger than 10.")))));
    assertThat(generated.str()).isEqualTo(expected);
  }
}
