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
import io.micronaut.core.naming.NameUtils;
import io.micronaut.inject.ast.ClassElement;
import io.micronaut.inject.ast.PropertyElement;
import io.micronaut.inject.visitor.VisitorContext;
import io.micronaut.sourcegen.annotations.PluginTaskParameter.OutputType;
import io.micronaut.sourcegen.annotations.PluginTaskParameter.PathSensitivity;
import io.micronaut.sourcegen.generator.visitors.ModelBuilder;
import io.micronaut.sourcegen.generator.visitors.ModelUtils;
import io.micronaut.sourcegen.generator.visitors.PluginUtils.ParameterConfig;
import io.micronaut.sourcegen.model.ExpressionDef;
import io.micronaut.sourcegen.model.InterfaceDef;
import io.micronaut.sourcegen.model.InterfaceDef.InterfaceDefBuilder;
import io.micronaut.sourcegen.model.MethodDef;
import io.micronaut.sourcegen.model.MethodDef.MethodDefBuilder;
import io.micronaut.sourcegen.model.StatementDef;
import io.micronaut.sourcegen.model.TypeDef;

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

    @Override
    protected TypeDef copyPOJO(VisitorContext context, String packageName, ClassElement element) {
        String simpleName = getSimpleName(element);
        InterfaceDefBuilder builder = InterfaceDef.builder(packageName + "." + simpleName + SPECIFICATION_NAME_SUFFIX)
            .addModifiers(Modifier.PUBLIC)
            .addJavadoc(element.getSimpleName() + " specification that used for configuring tasks.");
        for (PropertyElement property: element.getBeanProperties()) {
            ParameterConfig parameter = createParameter(property, context, packageName);
            MethodDef getter = createParameterGetter(parameter);
            builder.addMethod(getter);
        }
        builder.addMethod(copyPOJOMethod(element, context, packageName));
        InterfaceDef interfaceDef = builder.build();
        generatedModels.put(element.getName(), new GeneratedModel(
            interfaceDef, element, convertPOJOMethod(interfaceDef.asTypeDef(), element), interfaceDef.asTypeDef()
        ));
        return interfaceDef.asTypeDef();
    }

    private ParameterConfig createParameter(PropertyElement property, VisitorContext context, String packageName) {
        return new ParameterConfig(
            property,
            false,
            null,
            false,
            false,
            OutputType.NONE,
            property.getDocumentation().orElse(property.getName() + " configuration property."),
            getType(context, packageName, property.getType()),
            PathSensitivity.RELATIVE,
            ModelUtils.isPOJO(property.getType())
        );
    }

    @Override
    protected ExpressionDef convertPOJOParameter(
        PropertyElement property, List<StatementDef> statements, ExpressionDef owner
    ) {
        ExpressionDef ownerProperty = owner.invoke("get" + NameUtils.capitalize(property.getName()), TypeDef.OBJECT);
        return convertParameterIfRequired(
            property.getType(),
            NameUtils.capitalize(property.getName()) + "Param",
            statements,
            ownerProperty.invoke("getOrNull", TypeDef.OBJECT)
        );
    }

    private MethodDef copyPOJOMethod(ClassElement element, VisitorContext context, String packageName) {
        return MethodDef.builder("copyTo")
            .addParameter(TypeDef.THIS)
            .addModifiers(Modifier.PUBLIC, Modifier.DEFAULT)
            .returns(TypeDef.VOID)
            .build((t, p) -> {
                List<StatementDef> statements = new ArrayList<>();
                for (PropertyElement property: element.getBeanProperties()) {
                    ParameterConfig parameter = createParameter(property, context, packageName);
                    String getterName = "get" + NameUtils.capitalize(parameter.source().getName());
                    TypeDef getterType = createGradleProperty(parameter);
                    ExpressionDef def = t.invoke(getterName, getterType);
                    statements.add(p.get(0).invoke(getterName, getterType)
                        .invoke("convention", TypeDef.VOID, def));
                }
                return StatementDef.multi(statements);
            });
    }
}
