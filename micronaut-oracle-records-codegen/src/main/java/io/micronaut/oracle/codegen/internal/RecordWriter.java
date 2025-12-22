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

import java.io.File;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;

/**
 * Internal utility to write generated Java sources.
 */
@Internal
public final class RecordWriter {

    private RecordWriter() {
    }

    /**
     * Write a Java source file into the target package under the provided output directory.
     *
     * @param pkg        The Java package
     * @param simpleName The simple type name
     * @param outputDir  The root output directory
     * @param content    The full source content
     * @throws IOException If writing fails
     */
    public static void writeJava(@NonNull String pkg,
                                 @NonNull String simpleName,
                                 @NonNull File outputDir,
                                 @NonNull String content) throws IOException {
        File pkgDir = new File(outputDir, pkg.replace('.', File.separatorChar));
        Files.createDirectories(pkgDir.toPath());
        File javaFile = new File(pkgDir, simpleName + ".java");
        Files.writeString(javaFile.toPath(), content, StandardCharsets.UTF_8);
    }
}
