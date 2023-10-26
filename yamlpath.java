///usr/bin/env jbang "$0" "$@" ; exit $?
//DEPS info.picocli:picocli:4.5.0
//DEPS io.github.yaml-path:yaml-path:0.0.10

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.PrintStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.Callable;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;

import io.github.yamlpath.YamlExpressionParser;
import io.github.yamlpath.utils.SerializationUtils;
import io.github.yamlpath.utils.StringUtils;
import picocli.CommandLine;
import picocli.CommandLine.Command;
import picocli.CommandLine.Parameters;

@Command(name = "yamlpath",
        mixinStandardHelpOptions = true,
        version = "yamlpath 0.1",
        description = "YAML-Path Expression Language Parser")
class yamlpath implements Callable<Integer> {

    @Parameters(index = "0", paramLabel = "file", description = "YAML file")
    private List<String> sources;

    @CommandLine.Option(names = {"-e", "--expression"}, description = "YAMLPath expression", required = true)
    private List<String> expressions;

    @CommandLine.Option(names = {"-r", "--replace-with"}, description = "Replace matching locations with this value")
    private String replacement;

    @CommandLine.Option(names = {"-o", "--output"}, description = "Sets the output file")
    private String output;

    @CommandLine.Option(names = {"-f", "--format"}, description = "Sets the output format", type = Format.class, defaultValue = "YAML")
    private Format format;

    @CommandLine.Option(names = {"-s", "--single"}, description = "Unify result into a single result", fallbackValue = "true", defaultValue = "false")
    private boolean single;

    public static void main(String... args) {
        int exitCode = new CommandLine(new yamlpath()).execute(args);
        System.exit(exitCode);
    }

    @Override
    public Integer call() throws Exception {
        List<InputStream> inputs = new ArrayList<>();
        if (sources != null) {
            for (String source : sources) {
                File file = Paths.get(source).toFile();
                if (!file.exists()) {
                    throw new RuntimeException("File '" + source + "' does not exist!");
                }

                inputs.addAll(findFiles(file));
            }
        } else {
            inputs.add(System.in);
        }

        YamlExpressionParser parser = new YamlExpressionParser(toResources(inputs));

        if (replacement == null) {
            Object value;
            // simply print value
            if (expressions.size() == 1) {
                value = parser.read(expressions.get(0));
            } else {
                value = parser.read(expressions);
            }

            System.out.println(parse(value));
        } else {
            OutputStream os;
            if (output != null) {
                Path outputLocation = Paths.get(output);
                File outputFile = outputLocation.toFile();
                if (!outputFile.exists()) {
                    Files.createDirectories(outputLocation.getParent());
                    outputFile.createNewFile();
                } else if (outputFile.isDirectory()) {
                    throw new RuntimeException("Output '" + output + "' is a directory!");
                }

                os = new FileOutputStream(outputFile, false);

            } else {
                os = new PrintStream(System.out);
            }

            // Otherwise, print modified yaml files
            parser.write(expressions, replacement);

            for (Map<Object, Object> resource : parser.getResources()) {
                os.write(parse(resource).getBytes(StandardCharsets.UTF_8));
            }

            if (output != null) {
                System.out.println("Output written in '" + output + "'");
            }
        }

        return 0;
    }

    private String parse(Object value) throws JsonProcessingException {
        if (single) {
            // deduplicate sets when there is a single result
            while (value instanceof Set set && set.size() == 1) {
                value = set.iterator().next();
            }
        }

        if (Format.YAML.equals(format)) {
            return toYaml(value);
        }

        return value.toString();
    }

    private List<Map<Object, Object>> toResources(List<InputStream> inputs) throws IOException {
        List<Map<Object, Object>> resources = new ArrayList();
        for (InputStream is : inputs) {
            List<Map<Object, Object>> resourcesInInput = fromYaml(is);
            if (resourcesInInput.size() == 1) {
                Map<Object, Object> first = resourcesInInput.get(0);
                Object kind = first.get("kind");
                if (kind != null && "List".equalsIgnoreCase(kind.toString())) {
                    resources.addAll((Collection<? extends Map<Object, Object>>) first.get("items"));
                } else {
                    resources.add(first);
                }
            } else {
                resources.addAll(resourcesInInput);
            }
        }
        return resources;
    }

    private List<Map<Object, Object>> fromYaml(InputStream is) throws IOException {
        String content = StringUtils.readAllBytes(is);
        try {
            return SerializationUtils.unmarshalAsListOfMaps(content);
        } catch (IOException e) {
            return SerializationUtils.yamlMapper().readValue(content, new TypeReference<List<Map<Object, Object>>>() {
            });
        }
    }

    private Collection<InputStream> findFiles(File file) throws FileNotFoundException {
        List<InputStream> inputs = new ArrayList<>();
        if (file.isDirectory()) {
            for (File subFile : file.listFiles()) {
                inputs.addAll(findFiles(subFile));
            }
        } else if (file.exists()) {
            inputs.add(new FileInputStream(file));
        }

        return inputs;
    }

    private String toYaml(Object object) throws JsonProcessingException {
        return SerializationUtils.yamlMapper().writeValueAsString(object);
    }

    enum Format {
        YAML,
        PLAIN
    }
}
