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


import io.micronaut.core.naming.NameUtils;
import io.micronaut.inject.ast.ClassElement;
import io.micronaut.inject.visitor.VisitorContext;
import io.micronaut.sourcegen.generator.visitors.ModelBuilder;
import io.micronaut.sourcegen.generator.visitors.PluginUtils;
import io.micronaut.sourcegen.generator.visitors.PluginUtils.ParameterConfig;
import io.micronaut.sourcegen.model.ClassTypeDef;
import io.micronaut.sourcegen.model.ExpressionDef;
import io.micronaut.sourcegen.model.MethodDef;
import io.micronaut.sourcegen.model.StatementDef;
import io.micronaut.sourcegen.model.TypeDef;
import io.micronaut.sourcegen.model.VariableDef;

import javax.lang.model.element.Modifier;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * A specific implementation of model builder for Maven.
 * In particular, it will generate the setDefaults method for POJO parameters
 */
public class MavenModelBuilder extends ModelBuilder {

    private final Map<String, MethodDef> setDefaultsMethods = new LinkedHashMap<>();

    /**
     * Create the model builder.
     *
     * @param packageName The package name to use for new created models
     */
    public MavenModelBuilder(String packageName) {
        super(packageName);
    }

    @Override
    protected ClassTypeDef copyPOJO(VisitorContext context, ClassElement element, List<ParameterConfig> parameters) {
        ClassTypeDef type = super.copyPOJO(context, element, parameters);
        if (!setDefaultsMethods.containsKey(element.getName())) {
            setDefaultsMethods.put(element.getName(), createSetDefaultsMethod(type, element, parameters));
        }
        return type;
    }

    /**
     * Get the method definitions for setting defaults.
     * @return The method definitions
     */
    public Collection<MethodDef> getDefaultsMethods() {
        return setDefaultsMethods.values();
    }

    /**
     * Invokes the setDefaults method for a particular POJO parameter.
     * If method does not exist, returns the original value.
     *
     * @param parameter The parameter
     * @param value Original value
     * @return The new value with default parameters
     */
    public ExpressionDef invokeSetDefaultsMethod(ParameterConfig parameter, ExpressionDef value) {
        String typeName = parameter.source().getType().getName();
        if (setDefaultsMethods.containsKey(typeName)) {
            return ClassTypeDef.THIS.invokeStatic(setDefaultsMethods.get(typeName), value);
        }
        return value;
    }

    /**
     * Create a method definition for setting the defaults of a POJO.
     *
     * @param type The parameter POJO type.
     * @param element The original POJO type.
     * @param parameters The parameters of the POJO.
     * @return The method definition
     */
    private MethodDef createSetDefaultsMethod(ClassTypeDef type, ClassElement element, List<ParameterConfig> parameters) {
        return MethodDef.builder("set" + getSimpleName(element) + "Defaults")
            .returns(type)
            .addParameter("value", type)
            .addModifiers(Modifier.PROTECTED, Modifier.STATIC)
            .build((t, params) -> {
                List<StatementDef> statements = new ArrayList<>();
                VariableDef.Local value = new VariableDef.Local("defined", type);
                statements.add(value.defineAndAssign(params.get(0)));
                statements.add(value.isNull().doIf(
                    value.assign(type.instantiate())
                ));
                for (ParameterConfig parameter : parameters) {
                    String getterName = "get" + NameUtils.capitalize(parameter.source().getName());
                    String setterName = "set" + NameUtils.capitalize(parameter.source().getName());
                    ExpressionDef currentValue = value.invoke(getterName, parameter.type());
                    if (parameter.isPOJO()) {
                        statements.add(value.invoke(setterName, TypeDef.VOID, invokeSetDefaultsMethod(parameter, currentValue)));
                    } else if (parameter.defaultValue() != null) {
                        statements.add(currentValue.ifNull(
                            value.invoke(setterName, TypeDef.VOID,
                                PluginUtils.createDefault(parameter.type(), parameter.defaultValue())
                            )
                        ));
                    } else if (parameter.required()) {
                        statements.add(currentValue.ifNull(
                            ClassTypeDef.of(IllegalStateException.class)
                                .instantiate(ExpressionDef.constant("Parameter " + parameter.source().getName()
                                    + " of " + type.getSimpleName() + " is required but was not specified"))
                                .doThrow()
                        ));
                    }
                }
                statements.add(value.returning());
                return StatementDef.multi(statements);
            });
    }

}
