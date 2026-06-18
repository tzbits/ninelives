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
  public void transpile_textWithMarkdown() {
    String src =
        """
                =d50= It's happening!
                **bold**, _italic_, ***bold-italic***, ~~strikethrough~~
                """;
    Transpiler tr = Transpiler.forSource(src);
    String out = tr.transpile();
    assertThat(out)
        .isEqualTo(
            imports +
            nodePreamble("=d50= It's happening!", "=g:d50=") +
            "game.say(`<b>bold</b>, <i>italic</i>, <b><i>bold-italic</i></b>, <del>strikethrough</del>`);\n" +
            nodeEnd +
            trailer);
  }

  @Test
  public void transpile_textWithMarkdownAndInterpolation() {
    String src =
        """
                =d50= It's happening!
                **bold** ${"keep_me_as_is_"} and _italic_
                Nested: ${ `${"inner"}` } **more**
                """;
    Transpiler tr = Transpiler.forSource(src);
    String out = tr.transpile();
    assertThat(out)
        .isEqualTo(
            imports +
            nodePreamble("=d50= It's happening!", "=g:d50=") +
            "game.say(`<b>bold</b> ${\"keep_me_as_is_\"} and <i>italic</i>\n" +
            "Nested: ${ `${\"inner\"}` } <b>more</b>`);\n" +
            nodeEnd +
            trailer);
  }

  @Test
  public void transpile_unclosedInterpolationFails() {
    String src =
        """
                =d50= It's happening!
                Unclosed: ${ some **bold**
                Still bold: **yes**
                """;
    Transpiler tr = Transpiler.forSource(src);
    assertThrows(RuntimeException.class, tr::transpile);
  }

  @Test
  public void transpile_multilineInterpolation() {
    String src =
        """
                =d50=
                You are at the cemetery gate. There is a path leading back uphill
                to the east${allowDownhill ? ` and a path leading downhill to the
                west` : ``}.
                """;
    Transpiler tr = Transpiler.forSource(src);
    String out = tr.transpile();
    assertThat(out).contains("to the east${allowDownhill ? ` and a path leading downhill to the\nwest` : ``}.");
  }

  @Test
  public void transpile_markdownWrappingInterpolation() {
    String src =
        """
                =d50= It's happening!
                **${howBad}**
                _foo ${bar} baz_
                """;
    Transpiler tr = Transpiler.forSource(src);
    String out = tr.transpile();
    assertThat(out)
        .isEqualTo(
            imports +
            nodePreamble("=d50= It's happening!", "=g:d50=") +
            "game.say(`<b>${howBad}</b>\n" +
            "<i>foo ${bar} baz</i>`);\n" +
            nodeEnd +
            trailer);
  }

  @Test
  public void transpile_choicesWithMarkdown() {
    String src =
        """
                =d50= It's happening!
                >node **bold** choice
                >node2 _italic_ choice
                """;
    Transpiler tr = Transpiler.forSource(src);
    String out = tr.transpile();
    assertThat(out)
        .isEqualTo(
            imports +
            nodePreamble("=d50= It's happening!", "=g:d50=") +
            "game.choose(\n" +
            "game.choice(\"=g:node=\", `<b>bold</b> choice`),\n" +
            "game.choice(\"=g:node2=\", `<i>italic</i> choice`));\n" +
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
  public void transpile_choicesWithIfCondition() {
    String src =
        """
                =d50=
                >node :if papersRead(); "Go home"
                >node2 :if !papersRead(); "Stay here"
                """;
    Transpiler tr = Transpiler.forSource(src);
    String out = tr.transpile();
    assertThat(out).contains("(papersRead()) ? game.choice(\"=g:node=\", \"Go home\") : false");
    assertThat(out).contains("(!papersRead()) ? game.choice(\"=g:node2=\", \"Stay here\") : false");
  }

  @Test
  public void transpile_choiceWithData() {
    String src =
        """
        =start=
        >node choice text; 'DATA'
        """;
    Transpiler tr = Transpiler.forSource(src);
    String out = tr.transpile();
    // It should be passed as a separate argument to game.choice
    assertThat(out).contains("game.choice(\"=g:node=\", `choice text`, 'DATA')");
  }

  @Test
  public void transpile_choiceWithDataInIfCondition() {
    String src =
        """
        =start=
        >node :if true; "choice text"; 'DATA'
        """;
    Transpiler tr = Transpiler.forSource(src);
    String out = tr.transpile();
    assertThat(out).contains("game.choice(\"=g:node=\", \"choice text\", 'DATA')");
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
  public void transpile_codeWithMarkdownPassedThrough() {
    String src =
        """
                =d50=
                | game.say("This is **bold** in code");
                """;
    Transpiler tr = Transpiler.forSource(src);
    String out = tr.transpile();
    assertThat(out)
        .isEqualTo(
            imports +
            nodePreamble("=d50=", "=g:d50=") +
            "game.say(\"This is **bold** in code\");\n" +
            nodeEnd +
            trailer);
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
  @Test
  public void transpile_choicesWrap() {
    String src =
        """
        =start=
        !choices wrap
        >node1 Choice 1
        >node2 Choice 2
        """;
    Transpiler tr = Transpiler.forSource(src);
    String out = tr.transpile();
    assertThat(out)
        .isEqualTo(
            imports +
            nodePreamble("=start=", "=g:start=") +
            "game.chooseWrap(\n" +
            "game.choice(\"=g:node1=\", `Choice 1`),\n" +
            "game.choice(\"=g:node2=\", `Choice 2`));\n" +
            nodeEnd +
            trailer);
  }

  @Test
  public void transpile_choicesNowrap() {
    String src =
        """
        =start=
        !choices nowrap
        >node1 Choice 1
        >node2 Choice 2
        """;
    Transpiler tr = Transpiler.forSource(src);
    String out = tr.transpile();
    assertThat(out)
        .isEqualTo(
            imports +
            nodePreamble("=start=", "=g:start=") +
            "game.chooseNowrap(\n" +
            "game.choice(\"=g:node1=\", `Choice 1`),\n" +
            "game.choice(\"=g:node2=\", `Choice 2`));\n" +
            nodeEnd +
            trailer);
  }

  @Test
  public void transpile_choicesGlobalDefault() {
    String src =
        """
        !choices nowrap
        =start=
        >node1 Choice 1
        """;
    Transpiler tr = Transpiler.forSource(src);
    String out = tr.transpile();
    assertThat(out)
        .contains("game.wrapChoices = false;");
    assertThat(out)
        .contains("game.choose(\n" +
                  "game.choice(\"=g:node1=\", `Choice 1`));");
  }

  @Test
  public void transpile_choiceVisitLimit() {
    String src =
        """
        =start=
        >node/1 This can only be visited once.
        """;
    Transpiler tr = Transpiler.forSource(src);
    String out = tr.transpile();

    assertThat(out)
        .isEqualTo(
            imports +
            nodePreamble("=start=", "=g:start=") +
            "game.choose(\n" +
            "(game.visitCount(\"=g:node=\") < 1) ? game.choice(\"=g:node=\", `This can only be visited once.`) : false);\n" +
            nodeEnd +
            trailer);
  }

  @Test
  public void transpile_choiceVisitLimitWithExistingCondition() {
    String src =
        """
        =start=
        >node/1 ? someCondition(); "This is conditional and limited"
        """;
    Transpiler tr = Transpiler.forSource(src);
    String out = tr.transpile();

    assertThat(out)
        .isEqualTo(
            imports +
            nodePreamble("=start=", "=g:start=") +
            "game.choose(\n" +
            "(game.visitCount(\"=g:node=\") < 1 && someCondition()) ? game.choice(\"=g:node=\", \"This is conditional and limited\") : false);\n" +
            nodeEnd +
            trailer);
  }

  @Test
  public void transpile_choiceVisitLimitWithSlashInProse() {
    String src =
        """
        =start=
        >node This is a choice / with a slash
        """;
    Transpiler tr = Transpiler.forSource(src);
    String out = tr.transpile();
    assertThat(out)
        .isEqualTo(
            imports +
            nodePreamble("=start=", "=g:start=") +
            "game.choose(\n" +
            "game.choice(\"=g:node=\", `This is a choice / with a slash`));\n" +
            nodeEnd +
            trailer);
  }
}
