# BUILD.bazel
load("//java/com/tzbits/ninelives:rules.bzl", "ninelives_story")

ninelives_story(
    name = "cloak",
    srcs = glob(["*.9l"]),
    static = glob(["img/*.png"]),
    story_js = "story.js",
)
