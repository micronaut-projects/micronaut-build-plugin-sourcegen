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
package io.micronaut.sourcegen.generator.visitors.maven;

import io.micronaut.core.annotation.Internal;
import io.micronaut.core.naming.NameUtils;
import io.micronaut.sourcegen.annotations.PluginTaskParameter.OutputType;
import io.micronaut.sourcegen.generator.visitors.ModelUtils;
import io.micronaut.sourcegen.generator.visitors.ModelUtils.GeneratedModel;
import io.micronaut.sourcegen.generator.visitors.PluginUtils;
import io.micronaut.sourcegen.generator.visitors.maven.MavenPluginUtils.MavenTaskConfig;
import io.micronaut.sourcegen.generator.visitors.PluginUtils.ParameterConfig;
import io.micronaut.sourcegen.model.AnnotationDef;
import io.micronaut.sourcegen.model.AnnotationDef.AnnotationDefBuilder;
import io.micronaut.sourcegen.model.ClassDef;
import io.micronaut.sourcegen.model.ClassDef.ClassDefBuilder;
import io.micronaut.sourcegen.model.ClassTypeDef;
import io.micronaut.sourcegen.model.ExpressionDef;
import io.micronaut.sourcegen.model.FieldDef;
import io.micronaut.sourcegen.model.MethodDef;
import io.micronaut.sourcegen.model.StatementDef;
import io.micronaut.sourcegen.model.TypeDef;
import io.micronaut.sourcegen.model.VariableDef;
import io.micronaut.sourcegen.model.VariableDef.Local;

import javax.lang.model.element.Modifier;
import java.io.File;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;


/**
 * A builder for Maven Mojos.
 */
@Internal
public class MavenMojoBuilder {

    /** The suffix to use for Mojo class. */
    public static final String MOJO_SUFFIX = "Mojo";
    private static final ClassTypeDef PARAMETER_ANNOTATION_TYPE =
        ClassTypeDef.of("org.apache.maven.plugins.annotations.Parameter");
    private static final FieldDef PROJECT_FIELD = FieldDef
        .builder("project", ClassTypeDef.of("org.apache.maven.project.MavenProject"))
        .addModifiers(Modifier.PROTECTED)
        .addAnnotation(AnnotationDef.builder(PARAMETER_ANNOTATION_TYPE)
            .addMember("defaultValue", "${project}")
            .addMember("required", true)
            .addMember("readonly", true)
            .build()
        )
        .build();
    private static final String ENABLED_FIELD_NAME = "enabled";

    /**
     * Method for building the Maven mojo.
     *
     * @param taskConfig The config
     * @return The class
     */
    public ClassDef build(MavenTaskConfig taskConfig) {
        String mojoName = taskConfig.packageName() + "." + taskConfig.namePrefix() + MOJO_SUFFIX;
        ClassDefBuilder builder = ClassDef.builder(mojoName)
            .addModifiers(Modifier.PUBLIC, Modifier.ABSTRACT);
        if (taskConfig.micronautPlugin()) {
            builder.superclass(ClassTypeDef.of("io.micronaut.maven.AbstractMicronautMojo"));
        } else {
            builder.superclass(ClassTypeDef.of("org.apache.maven.plugin.AbstractMojo"));
        }

        builder.addField(PROJECT_FIELD);
        builder.addField(FieldDef.builder(ENABLED_FIELD_NAME, TypeDef.of(boolean.class))
            .addModifiers(Modifier.PROTECTED)
            .addJavadoc("Determines if this mojo must be executed. The value is true if the mojo is enabled.")
            .addAnnotation(AnnotationDef.builder(PARAMETER_ANNOTATION_TYPE)
                .addMember("property", taskConfig.enabledPropertyName())
                .addMember("defaultValue", "true")
                .build())
            .build()
        );
        for (ParameterConfig parameter : taskConfig.parameters()) {
            addParameter(taskConfig, parameter, builder);
        }
        builder.addMethods(taskConfig.generatedModels().stream().map(GeneratedModel::convertorMethod).toList());
        builder.addMethod(createExecuteMethod(taskConfig));
        builder.addJavadoc(taskConfig.taskJavadoc());

        return builder.build();
    }

    private void addParameter(MavenTaskConfig taskConfig, ParameterConfig parameter, ClassDefBuilder builder) {
        if (parameter.internal()) {
            builder.addMethod(MethodDef
                .builder("get" + NameUtils.capitalize(parameter.source().getName()))
                .returns(parameter.type())
                .addModifiers(Modifier.PROTECTED, Modifier.ABSTRACT)
                .addJavadoc(parameter.javadoc())
                .build()
            );
        } else {
            AnnotationDefBuilder ann = AnnotationDef.builder(PARAMETER_ANNOTATION_TYPE);
            if (parameter.defaultValue() != null) {
                ann.addMember("defaultValue", parameter.defaultValue());
            }
            if (parameter.required()) {
                ann.addMember("required", true);
            }
            if (parameter.globalProperty() != null) {
                ann.addMember("property",  taskConfig.propertyPrefix()
                    + "." + MavenPluginUtils.toDotSeparated(parameter.globalProperty()));
            }
            FieldDef field = FieldDef.builder(parameter.source().getName())
                .ofType(parameter.type())
                .addModifiers(Modifier.PROTECTED)
                .addAnnotation(ann.build())
                .addJavadoc(parameter.javadoc())
                .build();
            builder.addField(field);
        }
    }

    private MethodDef createExecuteMethod(MavenTaskConfig taskConfig) {
        return MethodDef.builder("execute")
            .overrides()
            .addModifiers(Modifier.PUBLIC)
            .addJavadoc(taskConfig.methodJavadoc())
            .build((t, params) -> {
                List<StatementDef> mainStatements = new ArrayList<>();
                for (ParameterConfig parameter : taskConfig.parameters()) {
                    addExecuteStatementsForParameter(parameter, t, mainStatements);
                }
                mainStatements.add(runTask(taskConfig, t));
                return t.field(ENABLED_FIELD_NAME, TypeDef.of(boolean.class))
                    .ifFalse(
                        t.invoke("getLog", ClassTypeDef.of("org.apache.maven.plugin.logging.Log"))
                            .invoke("debug", TypeDef.VOID, ExpressionDef.constant(taskConfig.namePrefix() + MOJO_SUFFIX + " is disabled")),
                        StatementDef.multi(mainStatements)
                    );
            });
    }

    private void addExecuteStatementsForParameter(
            ParameterConfig parameter, VariableDef.This t, List<StatementDef> statements
    ) {
        ExpressionDef value = getParameterValue(parameter, t);
        if (parameter.source().getType().isAssignable(File.class)) {
            value = value.invoke("getAbsolutePath", TypeDef.STRING);
        }
        if (parameter.output() == OutputType.RESOURCES) {
            ClassTypeDef resourceType = ClassTypeDef.of("org.apache.maven.model.Resource");
            Local resource = new Local(parameter.source().getName() + "Resource", resourceType);
            statements.add(resource.defineAndAssign(resourceType.instantiate()));
            statements.add(resource.invoke("setTargetPath", TypeDef.VOID, value));
            statements.add(t.field(PROJECT_FIELD).invoke("addResource", TypeDef.VOID, resource));
        } else if (parameter.output() == OutputType.JAVA_SOURCES
            || parameter.output() == OutputType.GROOVY_SOURCES
            || parameter.output() == OutputType.KOTLIN_SOURCES
        ) {
            statements.add(t.field(PROJECT_FIELD).invoke("addCompileSourceRoot", TypeDef.VOID, value));
        }
    }

    private StatementDef runTask(MavenTaskConfig taskConfig, VariableDef.This t) {
        Map<String, ExpressionDef> params = new HashMap<>();
        List<StatementDef> statements = new ArrayList<>();
        for (ParameterConfig parameter: taskConfig.parameters()) {
            ExpressionDef expression = getParameterValue(parameter, t);
            params.put(
                parameter.source().getName(),
                ModelUtils.convertParameterIfRequired(
                    parameter.source().getType(), parameter.source().getName() + "Param", statements, expression
                )
            );
        }
        statements.add(PluginUtils.executeTaskMethod(taskConfig.source(), taskConfig.methodName(), params));
        return StatementDef.multi(statements);
    }

    private ExpressionDef getParameterValue(ParameterConfig parameter, VariableDef.This t) {
        if (parameter.internal()) {
            String getter = "get" + NameUtils.capitalize(parameter.source().getName());
            return t.invoke(getter, parameter.type());
        } else {
            return t.field(parameter.source().getName(), parameter.type());
        }
    }

}
