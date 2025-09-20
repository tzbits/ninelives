package com.tzbits.ninelives;

import static com.google.common.truth.Truth.assertThat;
import static org.junit.Assert.assertThrows;

import com.google.common.truth.Truth;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.JUnit4;

@RunWith(JUnit4.class)
public class TranspilerTest {
  private static final String imports = "import {game, back, visited, GameNode} from \"./game.js\";\nimport {story} from \"./story.js\";\n";

  @Test
  public void transpile_withValidSource() {
    String src =
        "| console.log('this just becomes output.');\n" +
        "| if(foo == bar) { \n" +
        "\n" +
        "Something seems fishy.\n" +
        "| }\n" +
        "";
    Transpiler tr = Transpiler.forSource(src);
    String out = tr.transpile();
    assertThat(out)
        .isEqualTo(
            imports +
            "console.log('this just becomes output.');\n" +
            "if(foo == bar) { \n" +
            "game.say(`Something seems fishy.`);\n" +
            "}\n");
  }

  @Test
  public void transpile_withEmptySource() {
    String src = "";
    Transpiler tr = Transpiler.forSource(src);
    String out = tr.transpile();
    assertThat(out).isEqualTo(imports);
  }

  @Test
  public void transpile_forSource_withNodeAndText() {
    String src =
        "=d50= It's happening!\n" +
        "Something seems fishy.\n" +
        "Then you see the tree fall.\n" +
        "";
    Transpiler tr = Transpiler.forSource(src);
    String out = tr.transpile();
    assertThat(out)
        .isEqualTo(
            imports +
            "\n/* =d50= It's happening! */\n" +
            "game.gameNodes['=g:d50='] =\n" +
            "new GameNode('=g:d50=').setExecFn(function(game, choice) {\n" +
            "game.player.location = '=g:d50=';\n" +
            "game.say(`Something seems fishy.\n" +
            "Then you see the tree fall.`);\n" +
            "});\n");
  }

  @Test
  public void transpile_withCommentsOnly() {
    String src =
        "# This is a comment.\n" +
        "# This is another comment.\n" +
        "\n";
    Transpiler tr = Transpiler.forSource(src);
    String out = tr.transpile();
    assertThat(out).isEqualTo(imports);
  }

  @Test
  public void transpile_withComments() {
    String src =
        "| console.log('this just becomes output.');\n" +
        "# this is a comment and should be ignored.\n" +
        "Something seems fishy.\n" +
        "";
    Transpiler tr = Transpiler.forSource(src);
    String out = tr.transpile();
    assertThat(out)
        .isEqualTo(
            imports +
            "console.log('this just becomes output.');\n" +
            "game.say(`Something seems fishy.`);\n"
                   );
  }

  @Test
  public void transpile_withCommand() {
    String src =
        "=d50= It's happening!\n" +
        "Something seems fishy.\n" +
        "\n" +
        "!c Then you see the tree fall.\n" +
        "";
    Transpiler tr = Transpiler.forSource(src);
    String out = tr.transpile();
    assertThat(out)
        .isEqualTo(
            imports +
            "\n/* =d50= It's happening! */\n" +
            "game.gameNodes['=g:d50='] =\n" +
            "new GameNode('=g:d50=').setExecFn(function(game, choice) {\n" +
            "game.player.location = '=g:d50=';\n" +
            "game.say(`Something seems fishy.`);\n" +
            "game.sayWith(\"c\", `Then you see the tree fall.`);\n" +
            "});\n");
  }

  @Test
  public void transpile_withImgCommand() {
    String src =
        "=d50= It's happening!\n" +
        "!img img/banner-ch2-s1-west.jpg\n" +
        "Something seems fishy.\n" +
        "";
    Transpiler tr = Transpiler.forSource(src);
    String out = tr.transpile();
    assertThat(out)
        .isEqualTo(
            imports +
            "\n/* =d50= It's happening! */\n" +
            "game.gameNodes['=g:d50='] =\n" +
            "new GameNode('=g:d50=').setExecFn(function(game, choice) {\n" +
            "game.player.location = '=g:d50=';\n" +
            "game.img(\"img/banner-ch2-s1-west.jpg\");\n" +
            "game.say(`Something seems fishy.`);\n" +
            "});\n");
  }


  @Test
  public void transpile_withChoices() {
    String src =
        "=d50= It's happening!\n" +
        "\n" +
        "Something seems fishy.\n" +
        "\n" +
        ">in-the-pond jump into the pond\n" +
        ">on-the-shore walk along the shore\n" +
        "";
    Transpiler tr = Transpiler.forSource(src);
    String out = tr.transpile();
    assertThat(out)
        .isEqualTo(
            imports +
            "\n/* =d50= It's happening! */\n" +
            "game.gameNodes['=g:d50='] =\n" +
            "new GameNode('=g:d50=').setExecFn(function(game, choice) {\n" +
            "game.player.location = '=g:d50=';\n" +
            "game.say(`Something seems fishy.`);\n" +
            "game.choose(\n" +
            "game.choice(\"=g:in-the-pond=\", `jump into the pond`),\n" +
            "game.choice(\"=g:on-the-shore=\", `walk along the shore`));\n" +
            "});\n");
  }

  @Test
  public void transpile_choiceWithoutNodeFails() {
    String src = "> I'm a choice without a node.\n";
    Transpiler tr = Transpiler.forSource(src);
    IllegalStateException e =
        assertThrows(IllegalStateException.class, () -> tr.transpile());
    assertThat(e).hasMessageThat()
        .contains("Choice (>) found before the beginning of a node.");
  }

  @Test
  public void transpile_multiNodeWithAllLineTypes() {
    String src =
        "=d50= First Node\n" +
        "This is some text for the first node.\n" +
        "!c This is a command.\n" +
        ">choice1 A choice for the first node.\n" +
        "\n" +
        "=d51= Second Node\n" +
        "| console.log('This is some code.');\n" +
        "This is some text for the second node.\n" +
        "";
    Transpiler tr = Transpiler.forSource(src);
    String out = tr.transpile();
    assertThat(out)
        .isEqualTo(
            imports +
            "\n/* =d50= First Node */\n" +
            "game.gameNodes['=g:d50='] =\n" +
            "new GameNode('=g:d50=').setExecFn(function(game, choice) {\n" +
            "game.player.location = '=g:d50=';\n" +
            "game.say(`This is some text for the first node.`);\n" +
            "game.sayWith(\"c\", `This is a command.`);\n" +
            "game.choose(\n" +
            "game.choice(\"=g:choice1=\", `A choice for the first node.`));\n" +
            "});\n" +
            "\n/* =d51= Second Node */\n" +
            "game.gameNodes['=g:d51='] =\n" +
            "new GameNode('=g:d51=').setExecFn(function(game, choice) {\n" +
            "game.player.location = '=g:d51=';\n" +
            "console.log('This is some code.');\n" +
            "game.say(`This is some text for the second node.`);\n" +
            "});\n");
  }
}
