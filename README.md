# YAML-Path Command Line

YAML-Path Command Line is a command line utility where you can read YAMLPath expressions or manipulate YAML files directly from your terminal.

## Installation

YAML-Path command line is installed via [JBang](https://www.jbang.dev/). So, if you haven't installed it yet, you need to install it:

```
curl -Ls https://sh.jbang.dev | bash -s - app setup
```

Note that you can find more information about how to install it in [here](https://www.jbang.dev/documentation/guide/latest/installation.html#using-jbang).

Next, you need to register the YAMLPath jbang repository:

```
jbang yamlpath@yaml-path/jbang
```

This call does the following things:

1.- It goes to `yaml-path/jbang` repo on `github.com` and downloads github.com/yaml-path/jbang/jbang-catalog.json and adds it as an implicit catalog.
2.- Then from that catalog, it runs the 'yamlpath' script.

And now you have `yamlpath` available on your `PATH`.