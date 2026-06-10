package com.tzbits.ninelives;

import static com.google.common.truth.Truth.assertThat;
import static org.junit.Assert.assertThrows;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.JUnit4;

@RunWith(JUnit4.class)
public class TranspilerTest {
  private static final String imports = "import {game, back, visited, GameNode} from \"./game.js\";\nimport {story} from \"./story.js\";\n";
  private static final String trailer = "game.scope = \"g\";\n";

  /** Returns the lines emitted at the start of every node. */
  private static String nodePreamble(String comment, String id) {
    return "\n/* " + comment + " */\n" +
        "game.gameNodes[\"" + id + "\"] =\n" +
        "new GameNode(\"" + id + "\").setExecFn(function(game, choice) {\n" +
        "game.player.location = \"" + id + "\";\n" +
        "game.scope = \"g\";\n";
  }

  private static final String nodeEnd = "game.scope = \"g\";\n});\n";

  @Test
  public void transpile_withValidSource() {
    String src =
            """
                    | console.log('this just becomes output.');
                    | if(foo == bar) {\s
                    
                    Something seems fishy.
                    | }
                    """;
    Transpiler tr = Transpiler.forSource(src);
    String out = tr.transpile();
    assertThat(out)
        .isEqualTo(
            imports +
            "console.log('this just becomes output.');\n" +
            "if(foo == bar) { \n" +
            "game.say(`Something seems fishy.`);\n" +
            "}\n" +
            trailer);
  }

  @Test
  public void transpile_withEmptySource() {
    String src = "";
    Transpiler tr = Transpiler.forSource(src);
    String out = tr.transpile();
    assertThat(out).isEqualTo(imports + trailer);
  }

  @Test
  public void transpile_forSource_withNodeAndText() {
    String src =
            """
                    =d50= It's happening!
                    Something seems fishy.
                    Then you see the tree fall.
                    """;
    Transpiler tr = Transpiler.forSource(src);
    String out = tr.transpile();
    assertThat(out)
        .isEqualTo(
            imports +
            nodePreamble("=d50= It's happening!", "=g:d50=") +
            "game.say(`Something seems fishy.\n" +
            "Then you see the tree fall.`);\n" +
            nodeEnd +
            trailer);
  }

  @Test
  public void transpile_withCommentsOnly() {
    String src =
            """
                    # This is a comment.
                    # This is another comment.
                    
                    """;
    Transpiler tr = Transpiler.forSource(src);
    String out = tr.transpile();
    assertThat(out).isEqualTo(imports + trailer);
  }

  @Test
  public void transpile_withComments() {
    String src =
            """
                    | console.log('this just becomes output.');
                    # this is a comment and should be ignored.
                    Something seems fishy.
                    """;
    Transpiler tr = Transpiler.forSource(src);
    String out = tr.transpile();
    assertThat(out)
        .isEqualTo(
            imports +
            "console.log('this just becomes output.');\n" +
            "game.say(`Something seems fishy.`);\n" +
            trailer);
  }

  @Test
  public void transpile_withCommand() {
    String src =
            """
                    =d50= It's happening!
                    Something seems fishy.
                    
                    !c Then you see the tree fall.
                    """;
    Transpiler tr = Transpiler.forSource(src);
    String out = tr.transpile();
    assertThat(out)
        .isEqualTo(
            imports +
            nodePreamble("=d50= It's happening!", "=g:d50=") +
            "game.say(`Something seems fishy.`);\n" +
            "game.sayWith(\"c\", `Then you see the tree fall.`);\n" +
            nodeEnd +
            trailer);
  }

  @Test
  public void transpile_withImgCommand() {
    String src =
            """
                    =d50= It's happening!
                    !img img/banner-ch2-s1-west.jpg
                    Something seems fishy.
                    """;
    Transpiler tr = Transpiler.forSource(src);
    String out = tr.transpile();
    assertThat(out)
        .isEqualTo(
            imports +
            nodePreamble("=d50= It's happening!", "=g:d50=") +
            "game.img(\"img/banner-ch2-s1-west.jpg\");\n" +
            "game.say(`Something seems fishy.`);\n" +
            nodeEnd +
            trailer);
  }


  @Test
  public void transpile_withChoices() {
    String src =
            """
                    =d50= It's happening!
                    
                    Something seems fishy.
                    
                    >in-the-pond jump into the pond
                    >on-the-shore walk along the shore
                    """;
    Transpiler tr = Transpiler.forSource(src);
    String out = tr.transpile();
    assertThat(out)
        .isEqualTo(
            imports +
            nodePreamble("=d50= It's happening!", "=g:d50=") +
            "game.say(`Something seems fishy.`);\n" +
            "game.choose(\n" +
            "game.choice(\"=g:in-the-pond=\", `jump into the pond`),\n" +
            "game.choice(\"=g:on-the-shore=\", `walk along the shore`));\n" +
            nodeEnd +
            trailer);
  }

  @Test
  public void transpile_withConditionalChoice() {
    String src =
            """
                    =d50= It's happening!
                    
                    >in-the-pond ? game.isWet(); "jump into the pond"
                    """;
    Transpiler tr = Transpiler.forSource(src);
    String out = tr.transpile();
    assertThat(out)
        .isEqualTo(
            imports +
            nodePreamble("=d50= It's happening!", "=g:d50=") +
            "game.choose(\n" +
            "(game.isWet()) ? game.choice(\"=g:in-the-pond=\", \"jump into the pond\") : false);\n" +
            nodeEnd +
            trailer);
  }

  @Test
  public void transpile_conditionalChoiceWithoutSemicolonFails() {
    String src =
            """
                    =d50= It's happening!
                    >in-the-pond ? game.isWet()
                    """;
    Transpiler tr = Transpiler.forSource(src);
    IllegalStateException e =
        assertThrows(IllegalStateException.class, tr::transpile);
    assertThat(e).hasMessageThat()
        .contains("Conditional choice (?) is missing ';'.");
  }

  @Test
  public void transpile_choiceWithSemicolonInProse() {
    String src =
            """
                    =d50= It's happening!
                    >in-the-pond jump in; get wet
                    """;
    Transpiler tr = Transpiler.forSource(src);
    String out = tr.transpile();
    assertThat(out)
        .isEqualTo(
            imports +
            nodePreamble("=d50= It's happening!", "=g:d50=") +
            "game.choose(\n" +
            "game.choice(\"=g:in-the-pond=\", `jump in; get wet`));\n" +
            nodeEnd +
            trailer);
  }

  @Test
  public void transpile_textStartingWithEquals() {
    String src =
            """
                    =d50= It's happening!
                    == win ? 'happy' : 'sad'} days ahead
                    """;
    Transpiler tr = Transpiler.forSource(src);
    String out = tr.transpile();
    assertThat(out)
        .isEqualTo(
            imports +
            nodePreamble("=d50= It's happening!", "=g:d50=") +
            "game.say(`== win ? 'happy' : 'sad'} days ahead`);\n" +
            nodeEnd +
            trailer);
  }

  @Test
  public void transpile_textWithBacktickIsNOTEscaped() {
    String src =
            """
                    =d50= It's happening!
                    A `backtick` but ${ok} stays.
                    """;
    Transpiler tr = Transpiler.forSource(src);
    String out = tr.transpile();
    assertThat(out)
        .isEqualTo(
            imports +
            nodePreamble("=d50= It's happening!", "=g:d50=") +
            "game.say(`A `backtick` but ${ok} stays.`);\n" +
            nodeEnd +
            trailer);
  }

  @Test
  public void transpile_codeWithNoSpaceAfterBar() {
    String src =
            """
                    | if (x) {
                    |}
                    """;
    Transpiler tr = Transpiler.forSource(src);
    String out = tr.transpile();
    assertThat(out)
        .isEqualTo(
            imports +
            "if (x) {\n" +
            "}\n" +
            trailer);
  }

  @Test
  public void transpile_choiceWithoutNodeFails() {
    String src = "> I'm a choice without a node.\n";
    Transpiler tr = Transpiler.forSource(src);
    IllegalStateException e =
        assertThrows(IllegalStateException.class, tr::transpile);
    assertThat(e).hasMessageThat()
        .contains("Choice (>) found before the beginning of a node.");
  }

  @Test
  public void transpile_multiNodeWithAllLineTypes() {
    String src =
            """
                    =d50= First Node
                    This is some text for the first node.
                    !c This is a command.
                    >choice1 A choice for the first node.
                    
                    =d51= Second Node
                    | console.log('This is some code.');
                    This is some text for the second node.
                    """;
    Transpiler tr = Transpiler.forSource(src);
    String out = tr.transpile();
    assertThat(out)
        .isEqualTo(
            imports +
            nodePreamble("=d50= First Node", "=g:d50=") +
            "game.say(`This is some text for the first node.`);\n" +
            "game.sayWith(\"c\", `This is a command.`);\n" +
            "game.choose(\n" +
            "game.choice(\"=g:choice1=\", `A choice for the first node.`));\n" +
            nodeEnd +
            nodePreamble("=d51= Second Node", "=g:d51=") +
            "console.log('This is some code.');\n" +
            "game.say(`This is some text for the second node.`);\n" +
            nodeEnd +
            trailer);
  }
}
