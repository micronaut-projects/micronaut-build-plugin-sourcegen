/*
 * Copyright 2017-2025 original authors
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

import io.micronaut.sourcegen.annotations.PluginTask;
import io.micronaut.sourcegen.annotations.PluginTaskExecutable;
import io.micronaut.sourcegen.annotations.PluginTaskParameter;
import io.micronaut.sourcegen.annotations.PluginTaskParameter.OutputType;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;

/**
 * This is a configuration for another plugin task run.
 * In this case it is a resource instead of a record.
 * The properties are parameters and the single method defines the task execution.
 */
@PluginTask
public final class GenerateSimpleResourceTask {

    private static final Logger LOG = LoggerFactory.getLogger(GenerateSimpleResourceTask.class.getName());

    /**
     * The generated file name.
     */
    @PluginTaskParameter(required = true)
    private String fileName;

    /**
     * The content of the file.
     */
    @PluginTaskParameter(required = true)
    private String content;

    /**
     * The output folder.
     */
    @PluginTaskParameter(output = OutputType.RESOURCES, directory = true, required = true, internal = true)
    private File outputFolder;

    /**
     * How the file ends.
     */
    @PluginTaskParameter(defaultValue = "NONE")
    private Ending ending;

    /**
     * Configure generating repeated file content.
     */
    @PluginTaskParameter()
    private Repeat repeat;

    /**
     * Generate a simple record in the supplied package and with the specified version.
     * This javadoc will be copied to the respected plugin implementations.
     */
    @PluginTaskExecutable
    public void generateSimpleResource() {
        LOG.info("Generating resource {}", fileName);

        StringBuilder fileContent = new StringBuilder();
        for (int i = 0; i < repeat.number(); i++) {
            if (i != 0 && repeat.delimiter() != null) {
                fileContent.append(repeat.delimiter());
            }
            fileContent.append(content);
            if (ending == Ending.NEWLINE) {
                if (i == repeat.number() - 1 || repeat.ending == RepeatEnding.EVERY) {
                    fileContent.append("\n");
                }
            }
        }

        File outputFile = new File(outputFolder.getAbsolutePath() + File.separator + fileName);
        outputFile.getParentFile().mkdirs();
        try (BufferedWriter writer = new BufferedWriter(new FileWriter(outputFile))) {
            writer.write(fileContent.toString());
        } catch (IOException e) {
            throw new RuntimeException(e);
        }

        LOG.info("Finished resource {}", fileName);
    }

    public void setFileName(String fileName) {
        this.fileName = fileName;
    }

    public void setContent(String content) {
        this.content = content;
    }

    public void setOutputFolder(File outputFolder) {
        this.outputFolder = outputFolder;
    }

    public void setEnding(Ending ending) {
        this.ending = ending;
    }

    public void setRepeat(Repeat repeat) {
        this.repeat = repeat;
    }

    /**
     * An enum representing how the file ends.
     */
    public enum Ending {
        NONE,
        NEWLINE
    }

    /**
     * Configuration for repeating the file content.
     *
     * @param number Number of repeats
     * @param delimiter The file delimiter
     * @param ending The file ending
     */
    public record Repeat(
        @PluginTaskParameter(defaultValue = "1")
        Integer number,
        String delimiter,
        @PluginTaskParameter(defaultValue = "ONCE")
        RepeatEnding ending
    ) {
    }

    /**
     * A configuration specifying how to repeat the ending.
     */
    public enum RepeatEnding {
        EVERY, ONCE
    }
}
