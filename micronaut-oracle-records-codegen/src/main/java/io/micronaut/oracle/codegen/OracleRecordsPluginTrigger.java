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
 * Triggers generation of Gradle and Maven plugins for Oracle->Java records codegen.
 *
 * Gradle:
 * - Creates an extension with two methods: generateOracleDomains and generateOracleDualityViews
 * - Creates two tasks and their specs.
 *
 * Maven:
 * - Generates two mojos (domains and duality views). Users must bind executions explicitly.
 */
@GenerateGradlePlugin(
    namePrefix = "OracleRecords",
    micronautPlugin = false,
    tasks = {
        @GenerateGradleTask(
            namePrefix = "GenerateOracleDomains",
            extensionMethodName = "generateOracleDomains",
            source = "io.micronaut.oracle.codegen.GenerateOracleDomainsTask"
        ),
        @GenerateGradleTask(
            namePrefix = "GenerateOracleDualityViews",
            extensionMethodName = "generateOracleDualityViews",
            source = "io.micronaut.oracle.codegen.GenerateOracleDualityViewsTask"
        )
    }
)
@GenerateMavenMojo(
    namePrefix = "AbstractGenerateOracleDomains",
    micronautPlugin = false,
    source = "io.micronaut.oracle.codegen.GenerateOracleDomainsTask",
    parameterPrefix = "oracle.codegen.domains",
    // Common parameters users may want to set via -Doracle.codegen.domains.*
    globalParameters = { "jdbcUrl", "username", "password", "schema", "targetPackage", "skip" },
    enabledPropertyName = "oracle.codegen.enabled"
)
@GenerateMavenMojo(
    namePrefix = "AbstractGenerateOracleDualityViews",
    micronautPlugin = false,
    source = "io.micronaut.oracle.codegen.GenerateOracleDualityViewsTask",
    parameterPrefix = "oracle.codegen.duality",
    globalParameters = { "jdbcUrl", "username", "password", "schema", "targetPackage", "skip" },
    enabledPropertyName = "oracle.codegen.enabled"
)
public final class OracleRecordsPluginTrigger {
}
