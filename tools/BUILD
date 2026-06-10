load("@rules_java//java:defs.bzl", "java_binary")
load("@rules_python//python:py_binary.bzl", "py_binary")

#
# simple web server for testing
#

py_binary(
    name = "local_www_server_bin",
    srcs = ["local_www_server_bin.py"],
)

exports_files(["local_www_server_bin.py"])
