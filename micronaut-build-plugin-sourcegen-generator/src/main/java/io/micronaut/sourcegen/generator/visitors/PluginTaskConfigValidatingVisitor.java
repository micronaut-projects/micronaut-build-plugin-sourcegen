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
package io.micronaut.sourcegen.generator.visitors;

import io.micronaut.core.annotation.Internal;
import io.micronaut.core.annotation.NonNull;
import io.micronaut.core.util.StringUtils;
import io.micronaut.inject.ast.ClassElement;
import io.micronaut.inject.ast.PropertyElement;
import io.micronaut.inject.processing.ProcessingException;
import io.micronaut.inject.visitor.TypeElementVisitor;
import io.micronaut.inject.visitor.VisitorContext;
import io.micronaut.sourcegen.annotations.PluginTask;
import io.micronaut.sourcegen.annotations.PluginTaskParameter.OutputType;
import io.micronaut.sourcegen.generator.visitors.JavadocUtils.TypeJavadoc;
import io.micronaut.sourcegen.generator.visitors.PluginUtils.ParameterConfig;
import io.micronaut.sourcegen.model.ClassTypeDef;

import java.io.File;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * The visitor that validates a PluginTaskConfig annotated type.
 * It also creates a META-INF file with javadoc that will be used for plugin generation.
 *
 * @author Andriy Dmytruk
 * @since 1.0.x
 */
@Internal
public final class PluginTaskConfigValidatingVisitor implements TypeElementVisitor<PluginTask, Object> {

    private final Set<String> processed = new HashSet<>();
    private final DocumentationAggregator aggregator = new DocumentationAggregator();

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
        return Set.of(PluginTask.class.getName());
    }

    @Override
    public void visitClass(ClassElement element, VisitorContext context) {
        if (processed.contains(element.getName())) {
            return;
        }

        // Verify that method is present
        PluginUtils.getTaskExecutable(element);
        ModelBuilder modelBuilder = new EmptyModelBuilder();
        List<ParameterConfig> parameters = new ArrayList<>();
        TypeJavadoc javadoc = JavadocUtils.getSourceJavadoc(element);
        for (PropertyElement property: element.getBeanProperties()) {
            ParameterConfig parameter = modelBuilder.getParameterConfig(context, javadoc, property);
            validateParameter(parameter, property, context);
            parameters.add(parameter);
        }

        writeJavaDocForType(context, element);
        aggregator.aggregate(element, parameters, javadoc);
    }

    @Override
    public void finish(VisitorContext visitorContext) {
        for (DocumentationAggregator.TaskInfo taskInfo: aggregator.getAllTasks()) {
            String path = "docs" + File.separator + taskInfo.name() + ".adoc";
            visitorContext.visitMetaInfFile(path).ifPresent(file -> {
                try {
                    file.write(writer -> writer.write(aggregator.toAsciidoc(taskInfo)));
                } catch (Exception e) {
                    visitorContext.warn("Failed to generate '" + path + "': " + e.getMessage(), null);
                }
            });
        }
    }

    private void validateParameter(ParameterConfig parameter, PropertyElement property, VisitorContext context) {
        if (parameter.output() != OutputType.NONE && parameter.output() != OutputType.CUSTOM) {
            if (!property.getType().isAssignable(File.class)) {
                context.fail("Sources output must be of type " + File.class.getName(), property);
            }
            if (!parameter.directory()) {
                context.fail("Sources output type must be a directory", property);
            }
        }
    }

    private void writeJavaDocForType(VisitorContext context, ClassElement element) {
        writeJavaDocMetaInfFile(element, context);

        for (PropertyElement property: element.getBeanProperties()) {
            ClassElement propertyType = property.getType();
            if (processed.contains(propertyType.getName())) {
                continue;
            }
            if (!ModelUtils.isModel(propertyType)) {
                continue;
            }
            processed.add(propertyType.getName());
            writeJavaDocForType(context, propertyType);
        }
    }

    private void writeJavaDocMetaInfFile(ClassElement element, VisitorContext context) {
        context.info("Writing javadoc META-INF file for " + element.getName());
        String fileName = JavadocUtils.META_INF_FOLDER + element.getName() + JavadocUtils.META_INF_EXTENSION;
        context.visitMetaInfFile(fileName, element)
            .ifPresent(generatedFile -> {
                try {
                    generatedFile.write(writer ->
                        writer.write(JavadocUtils.writeJavadocInfo(element))
                    );
                } catch (Exception e) {
                    throw new ProcessingException(element, "Failed to generate '" + fileName + "': " + e.getMessage(), e);
                }
            });
    }

    private final class DocumentationAggregator {

        Map<String, TaskInfo> allTasks = new HashMap<>();

        public List<TaskInfo> getAllTasks() {
            return allTasks.values().stream().sorted(Comparator.comparing(TaskInfo::name)).toList();
        }

        public String toAsciidoc(TaskInfo task) {
            StringBuilder result = new StringBuilder();
            result.append(".Configuration for ").append(task.name).append("\n");
            result.append(task.description()).append("\n");
            result.append("[cols=\"1,1,2\"]\n|===\n");
            result.append("|Property\n|Default\n|Description\n\n");
            for (PropertyInfo property: task.properties) {
                result.append("|").append(property.path()).append("\n");
                if (property.parameter.defaultValue() != null) {
                    result.append("|").append(property.parameter.defaultValue()).append("\n");
                } else {
                    result.append("|\n");
                }
                result.append("|");
                if (property.description() != null) {
                    result.append(property.description());
                }
                if (property.parameter().required()) {
                    result.append("\n\n*Required*.");
                }
                switch (property.parameter.output()) {
                    case JAVA_SOURCES: result.append("\n\nWill be added to Java sources."); break;
                    case GROOVY_SOURCES: result.append("\n\nWill be added to Groovy sources."); break;
                    case KOTLIN_SOURCES: result.append("\n\nWill be added to Kotlin sources."); break;
                    case RESOURCES: result.append("\n\nWill be added to resources."); break;
                    default: break;
                }
                if (property.parameter().directory()) {
                    result.append("\n\nSupply directory path.");
                }
                result.append("\n");
            }
            result.append("|===\n\n");
            return result.toString();
        }

        public void aggregate(ClassElement taskElement, List<ParameterConfig> parameters, TypeJavadoc javadoc) {
            if (allTasks.containsKey(taskElement.getName())) {
                return;
            }

            String name = taskElement.getSimpleName();
            String description = javadoc.javadoc().orElse(null);
            List<PropertyInfo> properties = new ArrayList<>();
            for (ParameterConfig parameter : parameters) {
                aggregateProperty(parameter, "", properties);
            }
            properties.sort(Comparator.comparing(PropertyInfo::path));
            allTasks.put(taskElement.getName(), new TaskInfo(name, description, properties));
        }

        private void aggregateProperty(ParameterConfig parameter, String path, List<PropertyInfo> properties) {
            String name = parameter.source().getName();
            String propPath = StringUtils.isEmpty(path) ? name : path + "." + name;
            properties.add(new PropertyInfo(
                name, propPath, parameter.javadoc(), parameter)
            );
            if (parameter.isPOJO()) {
                for (ParameterConfig subParameter: parameter.pojoParameters()) {
                    aggregateProperty(subParameter, propPath, properties);
                }
            }
        }

        record TaskInfo(
            String name,
            String description,
            List<PropertyInfo> properties
        ) {
        }

        record PropertyInfo(
            String name,
            String path,
            String description,
            ParameterConfig parameter
        ) {
        }
    }

    private class EmptyModelBuilder extends ModelBuilder {

        /**
         * Create the model builder.
         */
        public EmptyModelBuilder() {
            super(null);
        }

        @Override
        protected ClassTypeDef copyPOJO(VisitorContext context, ClassElement element, List<ParameterConfig> parameters) {
            return ClassTypeDef.of(element.getType());
        }

        @Override
        protected ClassTypeDef copyEnum(VisitorContext context, ClassElement element) {
            return ClassTypeDef.of(element.getType());
        }
    }

}
