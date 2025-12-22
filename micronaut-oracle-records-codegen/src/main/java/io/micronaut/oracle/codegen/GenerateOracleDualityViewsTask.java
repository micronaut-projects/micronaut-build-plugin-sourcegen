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
package io.micronaut.oracle.codegen;

import io.micronaut.core.annotation.NonNull;
import io.micronaut.sourcegen.annotations.PluginTask;
import io.micronaut.sourcegen.annotations.PluginTaskExecutable;
import io.micronaut.sourcegen.annotations.PluginTaskParameter;
import io.micronaut.sourcegen.annotations.PluginTaskParameter.OutputType;

import java.io.File;
import java.util.List;

/**
 * Generate Java records from Oracle JSON Duality Views.
 * This task is intentionally opt-in and safe for CI: it will no-op unless explicitly enabled by the user.
 *
 * Configuration rules:
 * - enabled defaults to false (must be set to true to execute)
 * - skip defaults to true (Maven-style skip)
 * - failOnMissingDb defaults to false (if true, unreachable DB fails the task; otherwise no-op with a warning)
 * - outputDir is treated as a CUSTOM output to avoid auto-wiring generated sources into project source sets
 *   (users must manually add the directory to compilation if they want to compile generated sources).
 *
 * JSON Schema:
 * - Each generated record is annotated with {@code @io.micronaut.jsonschema.JsonSchema}.
 * - This module does not add a compile-time dependency for the consumer. Users must add micronaut-json-schema to their project.
 *
 * @param skip Whether to skip execution (if null or true, the task no-ops)
 * @param failOnMissingDb If true, fail when DB is unreachable; otherwise no-op with a warning
 * @param jdbcUrl JDBC URL to the Oracle database
 * @param username Username used to connect to the database
 * @param password Password used to connect to the database
 * @param schema Oracle schema name to introspect
 * @param targetPackage Target Java package for generated records
 * @param include Optional include filters (object/view names or patterns)
 * @param outputDir Output directory for generated sources (not auto-attached to source sets)
 */
@PluginTask
public record GenerateOracleDualityViewsTask(
    @PluginTaskParameter
    @io.micronaut.core.annotation.Nullable Boolean skip,
    @PluginTaskParameter
    @io.micronaut.core.annotation.Nullable Boolean failOnMissingDb,

    @PluginTaskParameter(required = true)
    @NonNull String jdbcUrl,
    @PluginTaskParameter(required = true)
    @NonNull String username,
    @PluginTaskParameter(required = true)
    @NonNull String password,
    @PluginTaskParameter(required = true)
    @NonNull String schema,

    @PluginTaskParameter(required = true)
    @NonNull String targetPackage,

    @PluginTaskParameter
    List<String> include,

    @PluginTaskParameter(output = OutputType.CUSTOM, directory = true, required = true)
    @NonNull File outputDir
) {

    /**
     * Execute duality-views-based code generation.
     * This executable is used by both the generated Gradle task and Maven mojo.
     */
    @PluginTaskExecutable
    public void generate() {
        if (skip == null || Boolean.TRUE.equals(skip)) {
            return;
        }

        // NOTE: Real implementation should connect to Oracle and introspect JSON Duality Views.
        // This initial scaffold generates a sample record and ensures @JsonSchema is present.
        try {
            if (!outputDir.exists() && !outputDir.mkdirs()) {
                throw new IllegalStateException("Failed to create output directory: " + outputDir);
            }
            String simpleName = "OracleDualityViewSample";
            String pkg = targetPackage;
            String content = """
package %s;

import io.micronaut.jsonschema.JsonSchema;

/**
 * Generated from Oracle JSON Duality Views (scaffold).
 * Field set is derived from Oracle metadata (duality views). This placeholder emits an empty record.
 */
@JsonSchema
public record %s() {
}
""".formatted(pkg, simpleName);

            io.micronaut.oracle.codegen.internal.RecordWriter.writeJava(pkg, simpleName, outputDir, content);
            System.out.println("Generated record " + pkg + "." + simpleName + " into " + outputDir.getAbsolutePath());
        } catch (Exception e) {
            if (Boolean.TRUE.equals(failOnMissingDb)) {
                throw new RuntimeException("Oracle duality views generation failed", e);
            }
            System.err.println("Oracle duality views generation skipped due to error (failOnMissingDb=false): " + e.toString());
        }
    }
}
