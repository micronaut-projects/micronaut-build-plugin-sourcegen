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
package io.micronaut.sourcegen.generator.visitors.gradle;

import io.micronaut.core.annotation.Internal;
import io.micronaut.core.annotation.NonNull;
import io.micronaut.inject.ast.ClassElement;
import io.micronaut.inject.processing.ProcessingException;
import io.micronaut.inject.visitor.TypeElementVisitor;
import io.micronaut.inject.visitor.VisitorContext;
import io.micronaut.sourcegen.annotations.GenerateGradlePlugin;
import io.micronaut.sourcegen.annotations.GenerateGradlePlugin.Type;
import io.micronaut.sourcegen.generator.SourceGenerator;
import io.micronaut.sourcegen.generator.SourceGenerators;
import io.micronaut.sourcegen.generator.visitors.ModelUtils.GeneratedModel;
import io.micronaut.sourcegen.generator.visitors.gradle.GradlePluginUtils.GradlePluginConfig;
import io.micronaut.sourcegen.generator.visitors.gradle.GradlePluginUtils.GradleTaskConfig;
import io.micronaut.sourcegen.generator.visitors.gradle.builder.GradleExtensionBuilder;
import io.micronaut.sourcegen.generator.visitors.gradle.builder.GradlePluginBuilder;
import io.micronaut.sourcegen.generator.visitors.gradle.builder.GradleSpecificationBuilder;
import io.micronaut.sourcegen.generator.visitors.gradle.builder.GradleTaskBuilder;
import io.micronaut.sourcegen.generator.visitors.gradle.builder.GradleTypeBuilder;
import io.micronaut.sourcegen.model.ObjectDef;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

/**
 * The visitor for generating Gradle plugins.
 *
 * @author Andriy Dmytruk
 * @since 1.0.x
 */
@Internal
public final class GradlePluginGenerationTriggerAnnotationVisitor implements TypeElementVisitor<GenerateGradlePlugin, Object> {

    private static final List<GradleTypeBuilder> BUILDERS = List.of(
        new GradleTaskBuilder(),
        new GradleExtensionBuilder(),
        new GradleSpecificationBuilder(),
        new GradlePluginBuilder()
    );

    private final Set<String> generated = new HashSet<>();
    private final Set<String> processed = new HashSet<>();

    @Override
    public @NonNull VisitorKind getVisitorKind() {
        return VisitorKind.ISOLATING;
    }

    @Override
    public void start(VisitorContext visitorContext) {
        processed.clear();
    }

    @Override
    public Set<String> getSupportedAnnotationNames() {
        return Set.of(GenerateGradlePlugin.class.getName());
    }

    @Override
    public void visitClass(ClassElement element, VisitorContext context) {
        context.info("Creating plugin classes");
        if (processed.contains(element.getName()) || !element.hasAnnotation(GenerateGradlePlugin.class)) {
            return;
        }
        try {
            List<ObjectDef> definitions = createDefinitions(context, element);
            SourceGenerator sourceGenerator = SourceGenerators.findByLanguage(context.getLanguage()).orElse(null);
            if (sourceGenerator == null) {
                throw new ProcessingException(element, "Could not find SourceGenerator for language " + context.getLanguage());
            }
            processed.add(element.getName());
            for (ObjectDef definition : definitions) {
                if (generated.contains(definition.getName())) {
                    continue;
                }
                generated.add(definition.getName());
                sourceGenerator.write(definition, context, element);
            }
        } catch (ProcessingException e) {
            throw e;
        } catch (Exception e) {
            SourceGenerators.handleFatalException(element, GenerateGradlePlugin.class, e,
                (exception -> {
                    processed.remove(element.getName());
                    throw exception;
                })
            );
        }
    }

    private List<ObjectDef> createDefinitions(VisitorContext context, ClassElement element) {
        List<ObjectDef> definitions = new ArrayList<>();
        GradlePluginConfig pluginConfig = GradlePluginUtils.getPluginConfig(element, context);
        for (GradleTaskConfig taskConfig : pluginConfig.tasks()) {
            definitions.addAll(taskConfig.generatedModels().stream().map(GeneratedModel::model).toList());
        }
        for (Type type : pluginConfig.types()) {
            List<ObjectDef> typeDefinitions = null;
            for (GradleTypeBuilder gradleTypeBuilder : BUILDERS) {
                if (gradleTypeBuilder.getType().equals(type)) {
                    typeDefinitions = gradleTypeBuilder.build(pluginConfig);
                }
            }
            if (typeDefinitions == null) {
                throw new ProcessingException(element, "Building plugin sources of type " + type + " not supported!");
            }
            definitions.addAll(typeDefinitions);
        }

        return definitions;
    }

}
