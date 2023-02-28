# YAML-Path Command Line

[YAML-Path](https://github.com/yaml-path/YamlPath) Command Line is a command line utility where you can read YAMLPath expressions or manipulate YAML files directly from your terminal.

## Installation

YAML-Path command line is installed via [JBang](https://www.jbang.dev/). So, if you haven't installed it yet, you need to install it:

```
curl -Ls https://sh.jbang.dev | bash -s - app setup
```

Note that you can find more information about how to install it in [here](https://www.jbang.dev/documentation/guide/latest/installation.html#using-jbang).

Next, you need to register the YAMLPath jbang repository:

```
curl -Ls https://sh.jbang.dev | bash -s - app install --fresh --force yamlpath@yaml-path/jbang
```

This call does the following things:

1.- It goes to `yaml-path/jbang` repo on `github.com` and downloads github.com/yaml-path/jbang/jbang-catalog.json and adds it as an implicit catalog.
2.- Then from that catalog, it runs the 'yamlpath' script.

And now you have `yamlpath` available on your `PATH`.

## Usage

- Help

```
> yamlpath --help
Usage: yamlpath [-hV] [-o=<output>] [-r=<replacement>] -e=<expressions>
                [-e=<expressions>]... [file]
YAML-Path Expression Language Parser
      [file]              YAML file
  -e, --expression=<expressions>
                          YAMLPath expression
  -h, --help              Show this help message and exit.
  -o, --output=<output>   Sets the output file
  -r, --replace-with=<replacement>
                          Replace matching locations with this value
  -V, --version           Print version information and exit.
```

- Find elements using YAMLPath expressions

```
> yamlpath -e "spec.selector.matchLabels.'app.kubernetes.io/name'" examples/test.yaml 
[example]
```

Where the first parameter is the YAMLPath expression and the second parameter is the YAML file (we can also specify a folder).

- Find elements and replace with a supplied property

```
> yamlpath --replace-with="anotherValue" -e metadata.name examples/test.yaml 
---
apiVersion: v1
kind: Service
metadata:
  name: anotherValue
spec:
  ports:
    - name: http
      port: 80
      targetPort: 8080
  type: ClusterIP
```

In this example, the updated YAML resource will be printed into the standard output. If you want to write the output into a separated file, you can specify the location using the parameter `--output`:

```
> yamlpath --replace-with="anotherValue" --output=target/result.yaml -e metadata.name examples/test.yaml 
Output written in 'target/result.yaml'
```

- Find several elements from input

```
> cat examples/test.yaml | yamlpath -e metadata.name -e metadata.kind
---
kind:
  - Service
  - Deployment
metadata.name: example
```

- Usage in pipelines
- 
```
> cat examples/test.yaml | yamlpath -e "(kind==Service)" | yamlpath -e metadata.name -e metadata.kind
---
kind: Service
metadata.name: example
```

# Development

You can develop the command line using your favorite IDE. For example, using Intellij:

```
jbang edit --open=idea yamlpath.java
```