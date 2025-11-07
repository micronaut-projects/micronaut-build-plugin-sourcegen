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
package io.micronaut.sourcegen.generator.visitors.gradle;

import io.micronaut.core.annotation.Internal;
import io.micronaut.core.naming.NameUtils;
import io.micronaut.core.reflect.ClassUtils;
import io.micronaut.inject.ast.ClassElement;
import io.micronaut.inject.visitor.VisitorContext;
import io.micronaut.sourcegen.generator.visitors.JavadocUtils;
import io.micronaut.sourcegen.generator.visitors.JavadocUtils.TypeJavadoc;
import io.micronaut.sourcegen.generator.visitors.ModelBuilder;
import io.micronaut.sourcegen.generator.visitors.PluginUtils;
import io.micronaut.sourcegen.generator.visitors.PluginUtils.ParameterConfig;
import io.micronaut.sourcegen.model.ClassTypeDef;
import io.micronaut.sourcegen.model.ExpressionDef;
import io.micronaut.sourcegen.model.InterfaceDef;
import io.micronaut.sourcegen.model.InterfaceDef.InterfaceDefBuilder;
import io.micronaut.sourcegen.model.MethodDef;
import io.micronaut.sourcegen.model.StatementDef;
import io.micronaut.sourcegen.model.TypeDef;
import io.micronaut.sourcegen.model.TypeDef.Primitive;

import javax.lang.model.element.Modifier;
import java.util.ArrayList;
import java.util.List;

import static io.micronaut.sourcegen.generator.visitors.gradle.builder.GradleSpecificationBuilder.SPECIFICATION_NAME_SUFFIX;
import static io.micronaut.sourcegen.generator.visitors.gradle.builder.GradleTaskBuilder.createGradleProperty;
import static io.micronaut.sourcegen.generator.visitors.gradle.builder.GradleTaskBuilder.createParameterGetter;

/**
 * A utility class for building Gradle models.
 */
@Internal
public class GradleModelBuilder extends ModelBuilder {

    /**
     * Create the model builder.
     *
     * @param packageName The package name to use for new created models
     */
    public GradleModelBuilder(String packageName) {
        super(packageName);
    }

    @Override
    public TypeDef getType(VisitorContext context, ClassElement element) {
        if (element.isPrimitive()) {
            return switch (element.getName()) {
                case "float" -> Primitive.FLOAT_WRAPPER;
                case "double" -> Primitive.DOUBLE_WRAPPER;
                case "boolean" -> Primitive.BOOLEAN_WRAPPER;
                case "byte" -> Primitive.BYTE_WRAPPER;
                case "int" -> Primitive.INT_WRAPPER;
                case "long" -> Primitive.LONG_WRAPPER;
                case "char" -> Primitive.CHAR_WRAPPER;
                case "short" -> Primitive.SHORT_WRAPPER;
                case "void" -> TypeDef.VOID;
                default -> throw new IllegalStateException("Unexpected primitive: " + element.getName());
            };
        }
        return super.getType(context, element);
    }

    @Override
    protected ClassTypeDef copyPOJO(VisitorContext context, ClassElement element, List<ParameterConfig> parameters) {
        String simpleName = getSimpleName(element);
        TypeJavadoc javadoc = JavadocUtils.getTaskJavadoc(context, element);
        InterfaceDefBuilder builder = InterfaceDef.builder(packageName + "." + simpleName + SPECIFICATION_NAME_SUFFIX)
            .addModifiers(Modifier.PUBLIC)
            .addJavadoc(javadoc.javadoc().orElse(element.getName() + " specification that used for configuring tasks."));
        for (ParameterConfig parameter : parameters) {
            MethodDef getter = createParameterGetter(parameter);
            builder.addMethod(getter);
        }
        builder.addMethod(copyPOJOMethod(parameters));
        InterfaceDef interfaceDef = builder.build();
        generatedModels.put(element.getName(), new GeneratedModel(
            interfaceDef, element, convertPOJOMethod(interfaceDef.asTypeDef(), element, parameters), interfaceDef.asTypeDef()
        ));
        return interfaceDef.asTypeDef();
    }

    @Override
    protected ExpressionDef convertPOJOParameter(
        ParameterConfig parameter, List<StatementDef> statements, ExpressionDef owner
    ) {
        ExpressionDef ownerProperty = owner.invoke("get" + NameUtils.capitalize(parameter.source().getName()), TypeDef.OBJECT);
        return convertParameterIfRequired(
            parameter.source().getType(),
            NameUtils.capitalize(parameter.source().getName()) + "Param",
            statements,
            ownerProperty.invoke("getOrNull", TypeDef.OBJECT)
        );
    }

    private MethodDef copyPOJOMethod(List<ParameterConfig> parameters) {
        return MethodDef.builder("copyTo")
            .addParameter(TypeDef.THIS)
            .addModifiers(Modifier.PUBLIC, Modifier.DEFAULT)
            .returns(TypeDef.VOID)
            .build((t, p) -> {
                List<StatementDef> statements = new ArrayList<>();
                for (ParameterConfig parameter: parameters) {
                    String getterName = "get" + NameUtils.capitalize(parameter.source().getName());
                    TypeDef getterType = createGradleProperty(parameter);
                    ExpressionDef def = t.invoke(getterName, getterType);
                    if (!parameter.required()) {
                        if (parameter.defaultValue() != null) {
                            TypeDef type = parameter.type();
                            def = def.invoke(
                                "orElse",
                                type,
                                PluginUtils.createDefault(type, parameter.defaultValue())
                            );
                        } else {
                            def = def.invoke("getOrNull", parameter.type());
                        }
                    }

                    statements.add(p.get(0).invoke(getterName, getterType)
                        .invoke("convention", TypeDef.VOID, def));
                }
                return StatementDef.multi(statements);
            });
    }
}
