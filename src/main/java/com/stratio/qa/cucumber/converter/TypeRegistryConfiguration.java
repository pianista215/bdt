/*
 * Copyright (C) 2014 Stratio (http://stratio.com)
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *         http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.stratio.qa.cucumber.converter;

import cucumber.api.TypeRegistry;
import cucumber.api.TypeRegistryConfigurer;
import io.cucumber.cucumberexpressions.ParameterType;
import io.cucumber.cucumberexpressions.Transformer;

import java.util.Locale;

import static java.util.Locale.ENGLISH;

public class TypeRegistryConfiguration implements TypeRegistryConfigurer {
    public static final int DEFAULT_RADIX = 16;

    @Override
    public Locale locale() {
        return ENGLISH;
    }

    @Override
    public void configureTypeRegistry(TypeRegistry typeRegistry) {
        typeRegistry.defineParameterType(new ParameterType<>(
                "nullablestring",
                "(.+?)",
                String.class,
                new Transformer<String>() {
                    @Override
                    public String transform(String input) throws Throwable {
                        if ("//NONE//".equals(input)) {
                            return "";
                        } else if ("//NULL//".equals(input)) {
                            return null;
                        } else if (input.startsWith("0x")) {
                            int cInt = Integer.parseInt(input.substring(2), DEFAULT_RADIX);
                            char[] cArr = Character.toChars(cInt);
                            return String.valueOf(cArr);
                        } else {
                            return input;
                        }
                    }
                }
        ));

        typeRegistry.defineParameterType(new ParameterType<>(
                "strokes",
                "(.+?)",
                Strokes.class,
                Strokes::new
        ));

        typeRegistry.defineParameterType(new ParameterType<>(
                "isornot",
                "(IS|IS NOT)",
                Boolean.class,
                new Transformer<Boolean>() {
                    @Override
                    public Boolean transform(String input) throws Throwable {
                        return "IS".equals(input);
                    }
                }
        ));
    }
}