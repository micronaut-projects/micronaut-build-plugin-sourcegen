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
package io.micronaut.oracle.codegen.internal;

import io.micronaut.core.annotation.Internal;
import io.micronaut.core.annotation.NonNull;
import io.micronaut.core.annotation.Nullable;

import java.util.List;

/**
 * SPI for inspecting Oracle schema metadata and deriving Java record specifications.
 * Implementations can connect to Oracle (JDBC) or read an offline snapshot.
 *
 * This avoids hard-coding fields like "id" and "name": the field list comes from Oracle metadata.
 */
@Internal
public interface OracleSchemaIntrospector {

    /**
     * Inspect Oracle Domain Definitions in a schema and derive record specifications.
     *
     * @param jdbcUrl JDBC URL
     * @param username Username
     * @param password Password
     * @param schema   Schema name
     * @param include  Optional filters (object names/patterns)
     * @return List of record specifications to render
     * @throws Exception If inspection fails
     */
    @NonNull List<RecordSpec> inspectDomains(@NonNull String jdbcUrl,
                                             @NonNull String username,
                                             @NonNull String password,
                                             @NonNull String schema,
                                             @Nullable List<String> include) throws Exception;

    /**
     * Inspect Oracle JSON Duality Views in a schema and derive record specifications.
     *
     * @param jdbcUrl JDBC URL
     * @param username Username
     * @param password Password
     * @param schema   Schema name
     * @param include  Optional filters (object/view names/patterns)
     * @return List of record specifications to render
     * @throws Exception If inspection fails
     */
    @NonNull List<RecordSpec> inspectDualityViews(@NonNull String jdbcUrl,
                                                  @NonNull String username,
                                                  @NonNull String password,
                                                  @NonNull String schema,
                                                  @Nullable List<String> include) throws Exception;
}
