# Rules for building and deploying interactive fiction stories
# using //java/com/tzbits/ninelives:NineLives

load("@rules_python//python:py_binary.bzl", "py_binary")
load("@rules_pkg//pkg:tar.bzl", "pkg_tar")

## Transpile

def _transpile_impl(ctx):
    outputs = []
    for src in ctx.files.srcs:
        # Declare the output file with the .js extension.
        # Use `src.basename` to get just the filename,
        # preventing the path from being duplicated.
        out = ctx.actions.declare_file(src.basename + ".js")

        # Register an action to run the `NineLives` command.
        ctx.actions.run(
            executable = ctx.executable._transpiler,
            inputs = [src, ctx.executable._transpiler],
            outputs = [out],
            arguments = [
                "--in",
                src.path,
                "--out",
                out.path,
            ],
        )
        outputs.append(out)

    # Return a DefaultInfo provider with the generated files.
    return [DefaultInfo(files = depset(outputs))]


_transpile = rule(
    implementation = _transpile_impl,
    attrs = {
        "srcs": attr.label_list(
            allow_files = True,
            mandatory = True,
            doc = "The list of 9c files to transpile",
        ),
        "_transpiler": attr.label(
            executable = True,
            cfg = "exec",
            default = "//java/com/tzbits/ninelives:NineLives",
        ),
    },
)

def _story_imports_runfile_impl(ctx):
    out = ctx.actions.declare_file("story-imports.js")

    # 1. Gather the depset of all files from dependencies.
    # This  flattens the list of depsets into a single depset of File objects.
    all_files_depset = depset(
        transitive=[dep[DefaultInfo].files for dep in ctx.attr.deps],
    )

    # 1a. Convert the depset to a list of files first, then iterate.
    files_list = all_files_depset.to_list()

    # 2. Use a list comprehension to create a list of all import statements.
    #    We use `f.basename` to get just the file name, since the story-imports.js
    #    file is in the same package next to the transpiled sources.
    import_statements = [
        """import \"./%s\";""" % f.basename
        for f in files_list
    ]

    # 3. Join the statements into a single string.
    imports_content = "\n".join(import_statements)

    # 4. Register a *single* action to write all the content to the output file.
    #    Note: The `inputs` attribute is crucial. It tells Bazel that this action depends on all
    #    the files, ensuring they are built before the command runs.
    ctx.actions.run_shell(
        outputs = [out],
        inputs = all_files_depset,
        command = "echo '%s' > %s" % (imports_content, out.path)
    )

    # 5. Return the story-imports.js file as an output of this rule.
    return [DefaultInfo(files = depset([out]))]

_story_imports_runfile = rule(
    implementation = _story_imports_runfile_impl,
    attrs = {
        "deps": attr.label_list(
            allow_empty = False,
            allow_files = True,
            mandatory = True,
            doc = "The js files generate imports for.",
        ),
    }
)

## game_html

def _game_html_runfiles_impl(ctx):
    outputs = []
    for src in ctx.files.srcs:
        dest = ctx.actions.declare_file(src.basename)
        ctx.actions.run_shell(
            inputs = [src],
            outputs = [dest],
            command = "cp %s %s" % (src.path, dest.path),
        )
        outputs.append(dest)
    return [DefaultInfo(files = depset(outputs))]

_game_html_runfiles = rule(
    implementation = _game_html_runfiles_impl,
    attrs = {
        "srcs": attr.label_list(
            allow_empty = False,
            allow_files = True,
            mandatory = True,
            doc = "Copies the pre-written files that make up the game environment.",
        ),
    }
)

## Story macro

def _ninelives_story_macro_impl(name, visibility, srcs, static, story_js=None):
    """
    Args:
        name: The name of the story.
        srcs: A list of 9l source files.
        static: A list of static files (e.g., HTML, CSS, images).
        story_js: Optional custom story code that is loaded by the index.html file.
    """
    transpiled_target = ":" + name + "_transpiled"
    _transpile(
        name = transpiled_target[1:],
        srcs = srcs,
    )

    story_imports_runfile_target = ":" + name + "_story_imports_js"
    _story_imports_runfile(
        name = story_imports_runfile_target[1:],
        deps = [
            transpiled_target,
        ]
    )

    story_game_html_runfiles_target = ":" + name + "_story_game_html_runfiles";
    game_html_srcs = ["//java/com/tzbits/ninelives:game_html_runfiles"]
    # If the user provides a custom story.js, use it
    if story_js:
        game_html_srcs.append(story_js)
    else:
        game_html_srcs.append("//java/com/tzbits/ninelives:data/story.js")

    _game_html_runfiles(
        name = story_game_html_runfiles_target[1:],
        srcs = game_html_srcs,
    )

    # Define a filegroup to collect all files to be accessed as runfiles
    # by the preview binary.
    runfiles_target = ":" + name + "_story"
    native.filegroup(
        name = runfiles_target[1:],
        srcs = static + [
            transpiled_target,
            story_imports_runfile_target,
            story_game_html_runfiles_target,
        ],
    )

    # Define the local serving executable.
    py_binary(
        name = name + "_local_server",
        srcs = ["//tools:local_www_server_bin.py"],
        main = "//tools:local_www_server_bin.py",
        data = [
            story_imports_runfile_target,
            runfiles_target,
            transpiled_target,
        ],
        args = [
            "$(location " + story_imports_runfile_target + ")",
            "8080"
        ],
    )

    pkg_tar(
        name = name + "_release",
        # Strip the path up to the runfiles root to keep the
        # repository path intact within the tarball.
        strip_prefix = "$(rootpath " + runfiles_target + ")",
        srcs = [runfiles_target],
    )


ninelives_story = macro(
    attrs = {
        "srcs": attr.label_list(
            allow_files = True,
            mandatory = True,
            configurable = False,
        ),
        "static": attr.label_list(
            allow_files = True,
            mandatory = True,
            configurable = False,
        ),
        "story_js": attr.label(
            allow_files = True,
            mandatory = False,
            configurable = False,
        ),
    },
    implementation = _ninelives_story_macro_impl,
)
