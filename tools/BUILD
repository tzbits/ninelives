load("@rules_java//java:defs.bzl", "java_binary")
load("@rules_python//python:py_binary.bzl", "py_binary")

load("@rules_shell//shell:sh_binary.bzl", "sh_binary")

#
# simple web server for testing
#

py_binary(
    name = "local_www_server_bin",
    srcs = ["local_www_server_bin.py"],
)

sh_binary(
    name = "local_dev_server_bin",
    srcs = ["local_dev_server_bin.sh"],
    data = ["//third_party:entr_tool"],
)

exports_files([
    "local_www_server_bin.py",
    "local_dev_server_bin.sh",
])
