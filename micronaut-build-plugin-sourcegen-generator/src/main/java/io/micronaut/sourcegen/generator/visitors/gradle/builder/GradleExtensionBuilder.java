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
package io.micronaut.sourcegen.generator.visitors.gradle.builder;

import io.micronaut.core.annotation.Internal;
import io.micronaut.core.annotation.NonNull;
import io.micronaut.core.annotation.Nullable;
import io.micronaut.core.naming.NameUtils;
import io.micronaut.sourcegen.annotations.GenerateGradlePlugin.Type;
import io.micronaut.sourcegen.annotations.PluginTaskParameter.OutputType;
import io.micronaut.sourcegen.generator.visitors.PluginUtils.ParameterConfig;
import io.micronaut.sourcegen.generator.visitors.gradle.GradlePluginUtils;
import io.micronaut.sourcegen.generator.visitors.gradle.GradlePluginUtils.GradlePluginConfig;
import io.micronaut.sourcegen.generator.visitors.gradle.GradlePluginUtils.GradleTaskConfig;
import io.micronaut.sourcegen.model.ClassDef;
import io.micronaut.sourcegen.model.ClassDef.ClassDefBuilder;
import io.micronaut.sourcegen.model.ClassTypeDef;
import io.micronaut.sourcegen.model.ExpressionDef;
import io.micronaut.sourcegen.model.FieldDef;
import io.micronaut.sourcegen.model.InterfaceDef;
import io.micronaut.sourcegen.model.InterfaceDef.InterfaceDefBuilder;
import io.micronaut.sourcegen.model.MethodDef;
import io.micronaut.sourcegen.model.ObjectDef;
import io.micronaut.sourcegen.model.ParameterDef;
import io.micronaut.sourcegen.model.StatementDef;
import io.micronaut.sourcegen.model.TypeDef;
import io.micronaut.sourcegen.model.VariableDef;
import io.micronaut.sourcegen.model.VariableDef.Local;
import io.micronaut.sourcegen.model.VariableDef.MethodParameter;

import javax.lang.model.element.Modifier;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import static io.micronaut.sourcegen.generator.visitors.gradle.builder.GradleTaskBuilder.TASK_SUFFIX;
import static io.micronaut.sourcegen.generator.visitors.gradle.builder.GradleTaskBuilder.createGradleProperty;

/**
 * A builder for {@link Type#GRADLE_EXTENSION}.
 * Creates a Gradle extension for calling a gradle task with the specification.
 */
@Internal
public class GradleExtensionBuilder implements GradleTypeBuilder {

    /** The suffix to use for extension class. */
    public static final String EXTENSION_NAME_SUFFIX = "Extension";
    /** The prefix to use for default extension class. */
    public static final String DEFAULT_EXTENSION_NAME_PREFIX = "Default";
    /** The suffix to use for task configurator class. */
    public static final String TASK_CONFIGURATOR_SUFFIX = "TaskConfigurator";

    static final FieldDef CLASS_STATIC_FIELD = FieldDef.builder("class", TypeDef.CLASS).build();

    private static final String EXECUTE_METHOD = "execute";
    private static final String CLASSPATH_FIELD = "classpath";
    private static final String GET_EXTENSIONS_METHOD = "getExtensions";
    private static final TypeDef PROJECT_TYPE = TypeDef.of("org.gradle.api.Project");
    private static final TypeDef CONFIGURATION_TYPE = TypeDef.of("org.gradle.api.artifacts.Configuration");
    private static final FieldDef PROJECT_FIELD = FieldDef.builder("project").ofType(PROJECT_TYPE)
        .addJavadoc("The project that extension is applied to.")
        .addModifiers(Modifier.PROTECTED, Modifier.FINAL).build();
    private static final ClassTypeDef ACTION_TYPE = ClassTypeDef.of("org.gradle.api.Action");
    private static final ClassTypeDef PLUGIN_TYPE = ClassTypeDef.of("org.gradle.api.Plugin");
    private static final ClassTypeDef TASK_PROVIDER_TYPE = ClassTypeDef.of("org.gradle.api.tasks.TaskProvider");
    private static final ClassTypeDef EXTENSION_CONTAINER_TYPE = ClassTypeDef.of("org.gradle.api.plugins.ExtensionContainer");
    private static final ClassTypeDef SOURCE_DIRECTORY_SET_TYPE = ClassTypeDef.of("org.gradle.api.file.SourceDirectorySet");

    @Override
    public Type getType() {
        return Type.GRADLE_EXTENSION;
    }

    @Override
    @NonNull
    public List<ObjectDef> build(GradlePluginConfig pluginConfig) {
        return List.of(
            buildExtensionInterface(pluginConfig),
            buildDefaultExtension(pluginConfig)
        );
    }

    private ObjectDef buildExtensionInterface(GradlePluginConfig pluginConfig) {
        InterfaceDefBuilder builder = InterfaceDef.builder(pluginConfig.packageName() + "." + pluginConfig.namePrefix() + EXTENSION_NAME_SUFFIX)
            .addModifiers(Modifier.PUBLIC)
            .addJavadoc("Configures the " + pluginConfig.namePrefix() + " execution.");

        for (GradleTaskConfig taskConfig: pluginConfig.tasks()) {
            ClassTypeDef specificationType = ClassTypeDef.of(pluginConfig.packageName()
                + "." + taskConfig.namePrefix() + GradleSpecificationBuilder.SPECIFICATION_NAME_SUFFIX);
            ClassTypeDef actionType = TypeDef.parameterized(
                ACTION_TYPE, TypeDef.wildcardSupertypeOf(specificationType));

            builder.addMethod(MethodDef.builder(taskConfig.extensionMethodName())
                .addModifiers(Modifier.PUBLIC, Modifier.ABSTRACT)
                .addParameter("name", String.class)
                .addParameter(ParameterDef.builder("action", actionType).build())
                .addJavadoc("Create a task for " + taskConfig.extensionMethodName() + "." +
                    "\n" + taskConfig.methodJavadoc() +
                    "\n@param name   The unique identifier used to derive task names" +
                    "\n@param action The action to apply on the task specification"
                )
                .build()
            );
        }
        return builder.build();
    }

    private ObjectDef buildDefaultExtension(GradlePluginConfig pluginConfig) {
        ClassTypeDef interfaceType = ClassTypeDef.of(pluginConfig.packageName() + "." + pluginConfig.namePrefix() + EXTENSION_NAME_SUFFIX);

        ClassDefBuilder builder = ClassDef.builder(pluginConfig.packageName() +
                "." + DEFAULT_EXTENSION_NAME_PREFIX + pluginConfig.namePrefix() + EXTENSION_NAME_SUFFIX)
            .addJavadoc("Default implementation of the {@link " + interfaceType.getName() + "}.")
            .addModifiers(Modifier.PUBLIC, Modifier.ABSTRACT)
            .addSuperinterface(interfaceType)
            .addField(
                FieldDef.builder("names", TypeDef.parameterized(Set.class, String.class))
                    .addModifiers(Modifier.PROTECTED, Modifier.FINAL)
                    .initializer(ClassTypeDef.of(HashSet.class).instantiate())
                    .addJavadoc("A set containing all the registered task names to verify that none are duplicated.")
                    .build()
            )
            .addField(PROJECT_FIELD)
            .addField(FieldDef.builder(CLASSPATH_FIELD, CONFIGURATION_TYPE)
                .addJavadoc("The classpath used for running the tasks.")
                .addModifiers(Modifier.PROTECTED, Modifier.FINAL).build());

        builder.addMethod(MethodDef.builder(MethodDef.CONSTRUCTOR)
            .addModifiers(Modifier.PUBLIC)
            .addAnnotation("javax.inject.Inject")
            .addParameter(PROJECT_FIELD.getName(), PROJECT_TYPE)
            .addParameter(CLASSPATH_FIELD, CONFIGURATION_TYPE)
            .build((t, params) ->
                StatementDef.multi(
                    t.field(PROJECT_FIELD).assign(params.get(0)),
                    t.field(CLASSPATH_FIELD, CONFIGURATION_TYPE).assign(params.get(1))
                )
            ));

        for (GradleTaskConfig taskConfig: pluginConfig.tasks()) {
            ClassTypeDef specificationType = ClassTypeDef.of(pluginConfig.packageName()
                + "." + taskConfig.namePrefix() + GradleSpecificationBuilder.SPECIFICATION_NAME_SUFFIX);
            ClassTypeDef actionType = TypeDef.parameterized(
                ACTION_TYPE, TypeDef.wildcardSupertypeOf(specificationType));

            ClassTypeDef javaPluginConsumerType;
            if (taskConfig.parameters().stream().anyMatch(p -> p.output() != OutputType.NONE && p.output() != OutputType.CUSTOM)) {
                ObjectDef javaPluginConsumer = buildJavaPluginConsumer(pluginConfig, taskConfig);
                builder.addInnerType(javaPluginConsumer);
                javaPluginConsumerType = javaPluginConsumer.asTypeDef();
            } else {
                javaPluginConsumerType = null;
            }

            builder.addMethod(MethodDef.builder(taskConfig.extensionMethodName())
                .overrides()
                .addModifiers(Modifier.PUBLIC)
                .addParameter("name", String.class)
                .addParameter(ParameterDef.builder("action", actionType).build())
                .build((t, params) -> buildExtensionMethod(t, params, pluginConfig, taskConfig, specificationType, javaPluginConsumerType))
            );
            builder.addMethod(buildCreateTaskMethod(pluginConfig, taskConfig));
            builder.addMethod(MethodDef.builder("configureSpec")
                .addModifiers(Modifier.PROTECTED)
                .addParameter("spec", specificationType)
                .addJavadoc("Configure the defaults for the {@link " + specificationType.getName() + "} specification.")
                .build((t, params) -> buildConfigureSpecMethod(taskConfig, params))
            );

            builder.addInnerType(buildTaskConfigurator(pluginConfig, taskConfig, specificationType));
        }

        return builder.build();
    }

    private ClassDef buildTaskConfigurator(
            GradlePluginConfig pluginConfig, GradleTaskConfig taskConfig, TypeDef specificationType
    ) {
        ClassTypeDef taskType = ClassTypeDef.of(pluginConfig.packageName() + "." + taskConfig.namePrefix() + TASK_SUFFIX);
        FieldDef specField = FieldDef.builder("spec", specificationType).build();
        FieldDef classpathField = FieldDef.builder(CLASSPATH_FIELD, CONFIGURATION_TYPE).build();

        MethodDef execute = MethodDef.builder(EXECUTE_METHOD)
            .addParameter(taskType)
            .overrides()
            .addModifiers(Modifier.PUBLIC)
            .build((t, params) -> {
                List<StatementDef> statements = new ArrayList<>();
                MethodParameter task = params.get(0);
                if (pluginConfig.taskGroup() != null) {
                    statements.add(task.invoke("setGroup", TypeDef.VOID, ExpressionDef.constant(pluginConfig.taskGroup())));
                }
                statements.add(task.invoke("getClasspath", ClassTypeDef.of("org.gradle.api.file.ConfigurableFileLocation"))
                    .invoke("from", TypeDef.VOID, t.field(classpathField))
                );
                statements.add(task.invoke("setDescription", TypeDef.VOID,
                    ExpressionDef.constant("Configure the " + taskConfig.extensionMethodName())));
                for (ParameterConfig parameter: taskConfig.parameters()) {
                    String getterName = "get" + NameUtils.capitalize(parameter.source().getName());
                    TypeDef getterType = createGradleProperty(parameter);
                    if (parameter.isPOJO()) {
                        statements.add(t.field(specField).invoke(getterName, getterType)
                            .invoke("copyTo", TypeDef.VOID, task.invoke(getterName, getterType)));
                    } else if (!parameter.internal()) {
                        StatementDef convention = task
                            .invoke(getterName, getterType)
                            .invoke("convention", getterType, t.field(specField).invoke(getterName, getterType));
                        statements.add(convention);
                    }
                }
                return StatementDef.multi(statements);
            });
        return ClassDef.builder(taskConfig.namePrefix() + TASK_CONFIGURATOR_SUFFIX)
            .addModifiers(Modifier.STATIC, Modifier.PROTECTED)
            .addSuperinterface(TypeDef.parameterized(ACTION_TYPE, taskType))
            .addField(specField)
            .addField(classpathField)
            .addAllFieldsConstructor()
            .addMethod(execute)
            .addJavadoc("The configurator for " + pluginConfig.namePrefix() + " task.")
            .build();
    }

    private MethodDef buildCreateTaskMethod(GradlePluginConfig pluginConfig, GradleTaskConfig taskConfig) {
        ClassTypeDef taskType = ClassTypeDef.of(pluginConfig.packageName() + "." + taskConfig.namePrefix() + TASK_SUFFIX);
        TypeDef taskProviderType = TypeDef.parameterized(TASK_PROVIDER_TYPE, TypeDef.wildcardSubtypeOf(taskType));
        TypeDef taskContainerType = TypeDef.of("org.gradle.api.tasks.TaskContainer");
        TypeDef pluginConfiguratorType = TypeDef.parameterized(ACTION_TYPE, taskType);

        return MethodDef.builder("create" + taskConfig.namePrefix() + "Task")
            .returns(taskProviderType)
            .addParameter("name", String.class)
            .addParameter("configurator", pluginConfiguratorType)
            .addJavadoc("Create the {@link " + taskType.getName() + "} task and configure it based on the specification.")
            .build((t, params) ->
                    t.field(PROJECT_FIELD)
                    .invoke("getTasks", taskContainerType)
                    .invoke("register", taskProviderType,
                        params.get(0),
                        taskType.getStaticField(CLASS_STATIC_FIELD),
                        params.get(1)
                    ).returning()
            );
    }

    private StatementDef buildConfigureSpecMethod(GradleTaskConfig taskConfig, List<VariableDef.MethodParameter> params) {
        List<StatementDef> statements = new ArrayList<>();
        for (ParameterConfig parameter: taskConfig.parameters()) {
            String getterName = "get" + NameUtils.capitalize(parameter.source().getName());
            TypeDef getterType = createGradleProperty(parameter);
            if (parameter.defaultValue() != null && !parameter.internal()) {
                TypeDef type = parameter.type();
                StatementDef convention = params.get(0)
                    .invoke(getterName, getterType)
                    .invoke("convention", getterType, GradlePluginUtils.createDefault(type, parameter.defaultValue()));
                statements.add(convention);
            }
        }
        return StatementDef.multi(statements);
    }

    private StatementDef buildExtensionMethod(
        VariableDef t, List<VariableDef.MethodParameter> params,
        GradlePluginConfig pluginConfig, GradleTaskConfig taskConfig,
        ClassTypeDef specificationType, @Nullable ClassTypeDef javaPluginConsumer
    ) {
        StatementDef ifStatement = new StatementDef.If(
            t.field("names", TypeDef.of(String.class)).invoke("add", TypeDef.of(boolean.class), params.get(0)).isFalse(),
            new StatementDef.Throw(ClassTypeDef.of("org.gradle.api.GradleException")
                .instantiate(TypeDef.STRING.invokeStatic("format", TypeDef.STRING,
                    ExpressionDef.constant("An " + taskConfig.extensionMethodName() + " definition with name '%s' was already created"),
                    params.get(0))
                )
            )
        );
        TypeDef objectFactoryType = TypeDef.of("org.gradle.api.type.ObjectFactory");
        Local spec = new Local("spec", specificationType);
        StatementDef specCreation = new StatementDef.DefineAndAssign(
            spec,
            t.field(PROJECT_FIELD)
                .invoke("getObjects", objectFactoryType)
                .invoke("newInstance", specificationType, specificationType.getStaticField(CLASS_STATIC_FIELD))
        );
        StatementDef configureSpec = t.invoke("configureSpec", TypeDef.VOID, spec);
        StatementDef actionCall = params.get(1).invoke(EXECUTE_METHOD, TypeDef.VOID, spec);

        ClassTypeDef taskType = ClassTypeDef.of(pluginConfig.packageName() + "." + taskConfig.namePrefix() + TASK_SUFFIX);
        TypeDef taskProviderType = TypeDef.parameterized(
            ClassTypeDef.of("org.gradle.api.tasks.TaskProvider"),
            TypeDef.wildcardSubtypeOf(taskType)
        );
        ExpressionDef pluginConfigurator = ClassTypeDef.of(taskConfig.namePrefix() + TASK_CONFIGURATOR_SUFFIX)
           .instantiate(spec, t.field(CLASSPATH_FIELD, CONFIGURATION_TYPE));
        Local task = new Local("task", taskProviderType);
        StatementDef taskCreation = new StatementDef.DefineAndAssign(
            task,
            t.invoke("create" + taskConfig.namePrefix() + "Task", taskProviderType, params.get(0), pluginConfigurator)
        );
        if (javaPluginConsumer != null) {
            taskCreation = StatementDef.multi(
                taskCreation,
                t.field(PROJECT_FIELD)
                    .invoke("getPlugins", TypeDef.of("org.gradle.api.plugins.PluginContainer"))
                    .invoke("withId", TypeDef.VOID, ExpressionDef.constant("java"),
                        javaPluginConsumer.instantiate(t.field(PROJECT_FIELD), task)
                    )
            );
        }
        return StatementDef.multi(
            ifStatement,
            specCreation,
            configureSpec,
            actionCall,
            taskCreation
        );
    }

    private ClassDef buildJavaPluginConsumer(GradlePluginConfig pluginConfig, GradleTaskConfig taskConfig) {
        ClassTypeDef sourceSetType = ClassTypeDef.of("org.gradle.api.tasks.SourceSet");
        TypeDef sourceSetContainerType = TypeDef.of("org.gradle.api.tasks.SourceSetContainer");
        ClassTypeDef taskType = ClassTypeDef.of(pluginConfig.packageName() + "." + taskConfig.namePrefix() + TASK_SUFFIX);
        TypeDef taskProviderType = TypeDef.parameterized(TASK_PROVIDER_TYPE, TypeDef.wildcardSubtypeOf(taskType));
        FieldDef taskField = FieldDef.builder("task").ofType(taskProviderType).build();

        List<ObjectDef> innerTypes = new ArrayList<>();

        return ClassDef.builder(taskConfig.namePrefix() + "JavaPluginConsumer")
            .addJavadoc("Consumer of the java plugin that configures source sets generated by the {@link " + taskType.getName() + " } task.")
            .addSuperinterface(TypeDef.parameterized(ACTION_TYPE, PLUGIN_TYPE))
            .addModifiers(Modifier.PRIVATE, Modifier.STATIC)
            .addField(PROJECT_FIELD)
            .addField(taskField)
            .addAllFieldsConstructor()
            .addMethod(MethodDef.builder(EXECUTE_METHOD)
                .addParameter("ignored", PLUGIN_TYPE)
                .addModifiers(Modifier.PUBLIC)
                .overrides()
                .build((t, params) -> {
                    ClassTypeDef extensionType = ClassTypeDef.of("org.gradle.api.plugins.JavaPluginExtension");
                    Local extension = new Local("extension", extensionType);
                    Local sourceSet = new Local("sourceSet", sourceSetType);

                    List<StatementDef> statements = new ArrayList<>();
                    statements.add(extension.defineAndAssign(t.field(PROJECT_FIELD)
                        .invoke(GET_EXTENSIONS_METHOD, EXTENSION_CONTAINER_TYPE)
                        .invoke("findByType", extensionType, extensionType.getStaticField(CLASS_STATIC_FIELD))
                    ));
                    statements.add(new StatementDef.If(extension.isNull(), ClassTypeDef.of("org.gradle.api.GradleException")
                        .instantiate(ExpressionDef.constant("No Java plugin extension found")).doThrow())
                    );
                    statements.add(sourceSet.defineAndAssign(extension.invoke("getSourceSets",  sourceSetContainerType)
                        .invoke("getByName", sourceSetType, sourceSetType.getStaticField("MAIN_SOURCE_SET_NAME", TypeDef.STRING)))
                    );

                    addSourceStatements(taskConfig.parameters(), sourceSet, taskField, taskType, t, statements, innerTypes, OutputType.JAVA_SOURCES);
                    addSourceStatements(taskConfig.parameters(), sourceSet, taskField, taskType, t, statements, innerTypes, OutputType.GROOVY_SOURCES);
                    addSourceStatements(taskConfig.parameters(), sourceSet, taskField, taskType, t, statements, innerTypes, OutputType.KOTLIN_SOURCES);
                    addSourceStatements(taskConfig.parameters(), sourceSet, taskField, taskType, t, statements, innerTypes, OutputType.RESOURCES);
                    return StatementDef.multi(statements);
                }))
            .addInnerType(innerTypes)
            .build();
    }

    private void addSourceStatements(
        List<ParameterConfig> parameters, Local sourceSet, FieldDef taskField, TypeDef taskType,
        VariableDef.This t, List<StatementDef> statements, List<ObjectDef> innerTypes, OutputType outputType
    ) {
        if (parameters.stream().noneMatch(p -> p.output().equals(outputType))) {
            return;
        }
        Local sourceDir;
        if (outputType == OutputType.JAVA_SOURCES) {
            sourceDir = new Local("java", SOURCE_DIRECTORY_SET_TYPE);
            statements.add(sourceDir.defineAndAssign(
                sourceSet.invoke("getJava", SOURCE_DIRECTORY_SET_TYPE)
            ));
        } else if (outputType == OutputType.GROOVY_SOURCES) {
            ClassTypeDef groovyDirSet = ClassTypeDef.of("org.gradle.api.tasks.GroovySourceDirectorySet");
            sourceDir = new Local("groovy", groovyDirSet);
            statements.add(sourceDir.defineAndAssign(
                sourceSet.invoke(GET_EXTENSIONS_METHOD, EXTENSION_CONTAINER_TYPE)
                    .invoke("findByType", groovyDirSet, groovyDirSet.getStaticField(CLASS_STATIC_FIELD))
            ));
        } else if (outputType == OutputType.KOTLIN_SOURCES) {
            sourceDir = new Local("kotlin", SOURCE_DIRECTORY_SET_TYPE);
            statements.add(sourceDir.defineAndAssign(
                sourceSet.invoke(GET_EXTENSIONS_METHOD, EXTENSION_CONTAINER_TYPE)
                    .invoke("findByName", TypeDef.OBJECT, ExpressionDef.constant("kotlin"))
                    .cast(SOURCE_DIRECTORY_SET_TYPE)
            ));
        } else {
            sourceDir = new Local("resources", SOURCE_DIRECTORY_SET_TYPE);
            statements.add(sourceDir.defineAndAssign(
                sourceSet.invoke("getResources", SOURCE_DIRECTORY_SET_TYPE)
            ));
        }
        List<StatementDef> innerStatements = new ArrayList<>();
        for (ParameterConfig parameter: parameters) {
            if (parameter.output() != outputType) {
                continue;
            }
            TypeDef propertyType = GradleTaskBuilder.createGradleProperty(parameter);
            ClassDef innerType = buildJavaPluginConsumerTransformer(parameter, propertyType, taskType);
            innerTypes.add(innerType);
            innerStatements.add(sourceDir.invoke("srcDir", TypeDef.VOID, t.field(taskField)
                .invoke("map", propertyType, innerType.asTypeDef().instantiate())));
        }
        if (outputType == OutputType.GROOVY_SOURCES || outputType == OutputType.KOTLIN_SOURCES) {
            statements.add(new StatementDef.If(sourceDir.isNonNull(), StatementDef.multi(innerStatements)));
        } else {
            statements.add(StatementDef.multi(innerStatements));
        }
    }

    private ClassDef buildJavaPluginConsumerTransformer(ParameterConfig parameter, TypeDef propertyType, TypeDef taskType) {
        return ClassDef
            .builder(NameUtils.capitalize(parameter.source().getName()) + "Transformer")
            .addModifiers(Modifier.PRIVATE, Modifier.STATIC)
            .addSuperinterface(TypeDef.parameterized(ClassTypeDef.of("org.gradle.api.Transformer"), propertyType, taskType))
            .addJavadoc("Transformer used for retrieving the " + parameter.source().getName()  + " property.")
            .addMethod(MethodDef.builder("transform")
                .addModifiers(Modifier.PUBLIC)
                .overrides()
                .returns(propertyType)
                .addParameter(taskType)
                .build((t1, params1) -> params1.get(0)
                    .invoke("get" + NameUtils.capitalize(parameter.source().getName()), propertyType)
                    .returning()
                )
            )
            .build();
    }

}
