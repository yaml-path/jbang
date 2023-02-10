///usr/bin/env jbang "$0" "$@" ; exit $?
//DEPS info.picocli:picocli:4.5.0
//DEPS io.github.yaml-path:yaml-path:0.0.5

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
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
import java.util.concurrent.Callable;

import io.github.yamlpath.YamlExpressionParser;
import io.github.yamlpath.YamlPath;
import io.github.yamlpath.utils.SerializationUtils;
import picocli.CommandLine;
import picocli.CommandLine.Command;
import picocli.CommandLine.Parameters;

@Command(name = "yamlpath",
        mixinStandardHelpOptions = true,
        version = "yamlpath 0.1",
        description = "YAML-Path Expression Language Parser")
class yamlpath implements Callable<Integer> {

    @Parameters(index = "0", paramLabel = "expression", description = "YAMLPath expression")
    private String expression;

    @Parameters(index = "1", paramLabel = "file", description = "YAML file")
    private List<String> sources;

    @CommandLine.Option(names = {"-r", "--replace-with"}, description = "Replace matching locations with this value")
    private String replacement;

    @CommandLine.Option(names = {"-o", "--output"}, description = "Sets the output file")
    private String output;

    public static void main(String... args) {
        int exitCode = new CommandLine(new yamlpath()).execute(args);
        System.exit(exitCode);
    }

    @Override
    public Integer call() throws Exception {
        List<InputStream> inputs = new ArrayList<>();
        for (String source : sources) {
            File file = Paths.get(source).toFile();
            if (!file.exists()) {
                throw new RuntimeException("File '" + source + "' does not exist!");
            }

            inputs.addAll(findFiles(file));
        }

        YamlExpressionParser parser = YamlPath.from(inputs.toArray(new InputStream[0]));

        if (replacement == null) {
            // simply print value
            System.out.println(parser.read(expression));
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
            parser.write(expression, replacement);
            for (Map<Object, Object> resource : parser.getResources()) {
                os.write(SerializationUtils.yamlMapper().writeValueAsString(resource).getBytes(StandardCharsets.UTF_8));
            }

            if (output != null) {
                System.out.println("Output written in '" + output + "'");
            }
        }

        return 0;
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
}
