/*
 * Copyright 2025 original authors
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * https://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package io.micronaut.sourcegen.example.plugin;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

// tag::content[]
import io.micronaut.sourcegen.annotations.PluginTask;
import io.micronaut.sourcegen.annotations.PluginTaskExecutable;
import io.micronaut.sourcegen.annotations.PluginTaskParameter;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * This is a configuration for a plugin task run.
 * The properties are parameters and the single method defines the task execution.
 * The plugin generates a simple record.
 *
 * @param typeName The generated class name
 * @param version The version
 * @param packageName The package name
 * @param properties The properties
 * @param javadoc The javadoc
 * @param outputFolder The output folder
 */
@PluginTask // <1>
public record GenerateSimpleRecordTask(
    @PluginTaskParameter(required = true, globalProperty = "typeName")
    String typeName, // <2>
    @PluginTaskParameter(defaultValue = "1", globalProperty = "version")
    Integer version, // <3>
    @PluginTaskParameter(defaultValue = "com.example", globalProperty = "packageName")
    String packageName,
    Map<String, String> properties,
    List<String> javadoc,
    @PluginTaskParameter(output = true, directory = true, required = true)
    File outputFolder // <4>
) {

    private static final Logger LOG = LoggerFactory.getLogger(GenerateSimpleRecordTask.class);

    private static final String CONTENT = """
package %s;

/**
 * Version: %s
%s
 */
public record %s(
%s
) {
}
""";

    /**
     * Generate a simple record in the supplied package and with the specified version.
     * This javadoc will be copied to the respected plugin implementations.
     */
    @PluginTaskExecutable // <5>
    public void generateSimpleRecord() {
        LOG.info("Generating record {}", typeName);

        File packageFolder = new File(outputFolder, "src" + File.separator
            + "main" + File.separator + "java" + File.separator
            + packageName.replace(".", File.separator));
        packageFolder.mkdirs();
        // Create the content of the file using the CONTENT template
        String content = String.format(
            CONTENT,
            packageName,
            version,
            javadoc.stream().map(v -> " * " + v).collect(Collectors.joining("\n")),
            typeName,
            properties.entrySet().stream().map(e -> "    " + e.getValue() + " " + e.getKey())
                .collect(Collectors.joining(",\n"))
        );
        File outputFile = new File(packageFolder, typeName + ".java");

        // Write the file
        try (BufferedWriter writer = new BufferedWriter(new FileWriter(outputFile))) {
            writer.write(content);
        } catch (IOException e) {
            throw new RuntimeException(e);
        }

        LOG.info("Finished record {}", typeName);
    }

}
// end::content[]
