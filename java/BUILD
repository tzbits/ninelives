# Targets used by all java projects.

load("@rules_java//java:defs.bzl", "java_plugin")

java_plugin(
    name = "auto_value_plugin",
    processor_class = "com.google.auto.value.processor.AutoValueProcessor",
    visibility = ["//visibility:public"],
    deps = [
        "@tzbits_maven//:com_google_auto_value_auto_value",
        "@tzbits_maven//:com_google_auto_value_auto_value_annotations",
    ],
)
