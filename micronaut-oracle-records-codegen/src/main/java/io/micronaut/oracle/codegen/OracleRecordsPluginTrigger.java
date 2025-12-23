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

import io.micronaut.sourcegen.annotations.GenerateGradlePlugin;
import io.micronaut.sourcegen.annotations.GenerateGradlePlugin.GenerateGradleTask;
import io.micronaut.sourcegen.annotations.GenerateMavenMojo;

/**
 * Triggers generation of Gradle and Maven plugins for generating Java records from Oracle JSON schema sources.
 *
 * Single task/mojo with a 'source' parameter to choose between DOMAIN_DEFINITION and DUALITY_VIEW.
 */
@GenerateGradlePlugin(
    namePrefix = "GenerateOracleJsonRecords",
    micronautPlugin = false,
    tasks = {
        @GenerateGradleTask(
            namePrefix = "GenerateOracleJsonRecords",
            extensionMethodName = "generateOracleRecords",
            source = "io.micronaut.oracle.codegen.GenerateJavaRecordFromOracleJsonSchemaTask"
        )
    }
)
@GenerateMavenMojo(
    namePrefix = "AbstractGenerateOracleJsonRecords",
    micronautPlugin = false,
    source = "io.micronaut.oracle.codegen.GenerateJavaRecordFromOracleJsonSchemaTask",
    parameterPrefix = "oracle.codegen",
    // Common parameters users may want to set via -Doracle.codegen.*
    // Including 'source' to allow switching DOMAIN_DEFINITION vs DUALITY_VIEW from properties
    globalParameters = { "source", "sources", "jdbcUrl", "username", "password", "schema", "targetPackage", "skip" },
    enabledPropertyName = "oracle.codegen.enabled"
)
public final class OracleRecordsPluginTrigger {
}
