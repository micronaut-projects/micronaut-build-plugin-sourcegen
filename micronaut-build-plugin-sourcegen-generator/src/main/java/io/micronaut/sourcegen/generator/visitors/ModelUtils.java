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
package io.micronaut.sourcegen.generator.visitors;

import io.micronaut.core.annotation.Internal;
import io.micronaut.inject.ast.ClassElement;

/**
 * A utility class for working with complex types, like enums and POJOs.
 */
@Internal
public class ModelUtils {

    /**
     * Whether it is considered a POJO.
     *
     * @param element The type
     * @return Whether it is POJO
     */
    public static boolean isPOJO(ClassElement element) {
        return !element.isEnum()
            && !element.isPrimitive()
            && !element.getPackageName().equals("java.util")
            && !element.getPackageName().equals("java.lang")
            && !element.getPackageName().equals("java.io");
    }

    /**
     * Whether the type is a model, in which case it will be copied.
     *
     * @param type The type
     * @return Whether it is a model
     */
    public static boolean isModel(ClassElement type) {
        return type.isEnum() || isPOJO(type);
    }

}
