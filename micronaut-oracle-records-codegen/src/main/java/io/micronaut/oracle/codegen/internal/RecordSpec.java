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

import io.micronaut.core.annotation.NonNull;
import io.micronaut.core.annotation.Nullable;

import java.util.List;

/**
 * Describes a Java record to generate from Oracle metadata.
 * The field list can represent primitives, simple reference types,
 * or nested object structures for complex Oracle OBJECT types / JSON sub-views.
 * @param packageName The target Java package for the generated record
 * @param simpleName The simple record name
 * @param fields The list of fields to generate in the record
 */
public record RecordSpec(
    @NonNull String packageName,
    @NonNull String simpleName,
    @NonNull List<Field> fields
) {
    /**
     * Field descriptor. For nested objects, {@code nested} is non-null and
     * javaType should represent the nested simple name to render (e.g. "Address").
     * @param name The field name
     * @param javaType For leaf fields: fully-qualified Java type; for nested objects: simple type name to generate
     * @param nested The nested fields when this field represents an object; null for leaf fields
     * @param collection Whether this represents a collection type; if true, javaType is the element type
     */
    public record Field(
        @NonNull String name,
        /**
         * Fully-qualified Java type name for leaf fields (e.g. "java.lang.String", "java.math.BigDecimal"),
         * or simple type name for nested object references that will also be generated (e.g. "Address").
         */
        @NonNull String javaType,
        /**
         * Nested fields when this represents an object/record to be generated inline.
         * If null, this is a leaf field.
         */
        @Nullable List<Field> nested,
        /**
         * Whether the field is a collection (e.g., VARRAY/NESTED TABLE). If true,
         * javaType applies to the element type.
         */
        boolean collection
    ) {
    }
}
