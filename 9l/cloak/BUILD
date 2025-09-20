# BUILD.bazel
load("//java/com/tzbits/ninelives:rules.bzl", "ninelives_story")

ninelives_story(
  name = "cloak",
  srcs = glob(["*.9l"]),
  story_js = "story.js",
  static = glob(["img/*.png"]),
)
