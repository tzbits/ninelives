# ninelives - transpiler for the 9l text game file format

load("@rules_java//java:defs.bzl", "java_binary")
load("@rules_java//java:defs.bzl", "java_library")

package_group(
    name = "tzbits_pkgs",
    packages = [
        "//java/com/tzbits/ninelives/...",
        "//javatests/com/tzbits/ninelives/...",
    ],
)

package(
    default_visibility = [":tzbits_pkgs"],
)

java_library(
    name = "ninelives",
    srcs = glob(
        ["*.java"],
        exclude = ["NineLives.java"],
    ),
    data = [":game_html_runfiles"],
    plugins = ["//java:auto_value_plugin"],
    deps = [
        "@tzbits_maven//:com_google_auto_value_auto_value_annotations",
        "@tzbits_maven//:com_google_guava_guava",
        "@tzbits_maven//:org_jcommander_jcommander",
    ],
)

java_binary(
    name = "NineLives",
    srcs = ["NineLives.java"],
    main_class = "com.tzbits.ninelives.NineLives",
    deps = [
        ":ninelives",
        "//third_party:guava",
        "//third_party:jcommander",
    ],
)

filegroup(
    name = "game_html_runfiles",
    srcs = [
        "data/game.css",
        "data/game.js",
        "data/index.htm",
    ],
)

# story.js is not in the file group because it is only included
# in the runfiles if the project doesn't already have a story.js
# file that the story author provided.
exports_files(["data/story.js"])
