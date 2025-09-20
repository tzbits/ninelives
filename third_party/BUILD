package_group(
    name = "tzbits_pkgs",
    packages = [
        "//java/com/tzbits/...",
        "//javatests/com/tzbits/...",
    ],
)

package(
    default_visibility = [":tzbits_pkgs"],
)

java_library(
    name = "guava",
    exports = [
        "@tzbits_maven//:com_google_guava_guava",
    ],
)

java_library(
    name = "jcommander",
    exports = [
        "@tzbits_maven//:org_jcommander_jcommander",
    ],
)

java_library(
    name = "junit",
    exports = [
        "@tzbits_maven//:junit_junit",
    ],
)

java_library(
    name = "truth",
    exports = [
        "@tzbits_maven//:com_google_truth_truth",
    ],
)

sh_binary(
    name = "rsync_tool",
    srcs = ["rsync_wrapper.sh"],
    data = [],  # No data needed as the tool is on the local system
    visibility = ["//visibility:public"]
)
