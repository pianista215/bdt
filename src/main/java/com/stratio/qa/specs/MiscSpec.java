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

package com.stratio.qa.specs;

import com.stratio.qa.utils.ThreadProperty;
import cucumber.api.java.en.Given;
import cucumber.api.java.en.Then;
import cucumber.api.java.en.When;
import io.cucumber.datatable.DataTable;
import org.assertj.core.api.Assertions;
import org.assertj.core.api.Fail;
import org.hjson.JsonArray;
import org.hjson.JsonValue;

import java.lang.reflect.InvocationTargetException;
import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import static com.stratio.qa.assertions.Assertions.assertThat;

/**
 * Generic Miscellaneous Specs.
 */
public class MiscSpec extends BaseGSpec {

    public static final int DEFAULT_TIMEOUT = 1000;

    /**
     * Generic constructor.
     *
     * @param spec object
     */
    public MiscSpec(CommonG spec) {
        this.commonspec = spec;

    }

    /**
     * Save value for future use.
     * <p>
     * If element is a jsonpath expression (i.e. $.fragments[0].id), it will be
     * applied over the last httpResponse.
     * <p>
     * If element is a jsonpath expression preceded by some other string
     * (i.e. ["a","b",,"c"].$.[0]), it will be applied over this string.
     * This will help to save the result of a jsonpath expression evaluated over
     * previous stored variable.
     *
     * @param position position from a search result
     * @param element  key in the json response to be saved
     * @param envVar   thread environment variable where to store the value
     * @throws IllegalAccessException    exception
     * @throws IllegalArgumentException  exception
     * @throws SecurityException         exception
     * @throws NoSuchFieldException      exception
     * @throws ClassNotFoundException    exception
     * @throws InstantiationException    exception
     * @throws InvocationTargetException exception
     * @throws NoSuchMethodException     exception
     */
    @Given("^I save element (in position '(.+?)' in )?'(.+?)' in environment variable '(.+?)'$")
    public void saveElementEnvironment(String position, String element, String envVar) throws Exception {
        Pattern pattern = Pattern.compile("^((.*)(\\.)+)(\\$.*)$");
        Matcher matcher = pattern.matcher(element);
        String json;
        String parsedElement;

        if (matcher.find()) {
            json = matcher.group(2);
            parsedElement = matcher.group(4);
        } else {
            json = commonspec.getResponse().getResponse();
            parsedElement = element;
        }

        String value = commonspec.getJSONPathString(json, parsedElement, position);

        ThreadProperty.set(envVar, value.replaceAll("\n", ""));
    }


    /**
     * Save value for future use.
     *
     * @param value  value to be saved
     * @param envVar thread environment variable where to store the value
     */
    @Given("^I save '(.+?)' in variable '(.+?)'$")
    public void saveInEnvironment(String value, String envVar) {
        ThreadProperty.set(envVar, value);
    }

    /**
     * Wait seconds.
     *
     * @param seconds
     * @throws InterruptedException
     */
    @When("^I wait '(\\d+)' seconds?$")
    public void idleWait(Integer seconds) throws InterruptedException {
        Thread.sleep(seconds * DEFAULT_TIMEOUT);
    }

    /**
     * Sort elements in envVar by a criteria and order.
     *
     * @param envVar   Environment variable to be sorted
     * @param criteria alphabetical,...
     * @param order    ascending or descending
     */
    @When("^I sort elements in '(.+?)' by '(.+?)' criteria in '(.+?)' order$")
    public void sortElements(String envVar, String criteria, String order) {

        String value = ThreadProperty.get(envVar);
        JsonArray jsonArr = JsonValue.readHjson(value).asArray();

        List<JsonValue> jsonValues = new ArrayList<JsonValue>();
        for (int i = 0; i < jsonArr.size(); i++) {
            jsonValues.add(jsonArr.get(i));
        }

        Comparator<JsonValue> comparator;
        switch (criteria) {
            case "alphabetical":
                commonspec.getLogger().debug("Alphabetical criteria selected.");
                comparator = new Comparator<JsonValue>() {
                    public int compare(JsonValue json1, JsonValue json2) {
                        int res = String.CASE_INSENSITIVE_ORDER.compare(json1.toString(), json2.toString());
                        if (res == 0) {
                            res = json1.toString().compareTo(json2.toString());
                        }
                        return res;
                    }
                };
                break;
            default:
                commonspec.getLogger().debug("No criteria selected.");
                comparator = null;
        }

        if ("ascending".equals(order)) {
            Collections.sort(jsonValues, comparator);
        } else {
            Collections.sort(jsonValues, comparator.reversed());
        }

        ThreadProperty.set(envVar, jsonValues.toString());
    }

    /**
     * Checks if an exception has been thrown.
     *
     * @param exception    : "IS NOT" | "IS"
     * @param clazz
     * @param exceptionMsg
     */
    @Then("^an exception '(.+?)' thrown( with class '(.+?)')?( and message like '(.+?)')?")
    public void assertExceptionNotThrown(String exception, String clazz, String exceptionMsg)
            throws ClassNotFoundException {
        List<Exception> exceptions = commonspec.getExceptions();
        if ("IS NOT".equals(exception)) {
            assertThat(exceptions).as("Captured exception list is not empty").isEmpty();
        } else {
            assertThat(exceptions).as("Captured exception list is empty").isNotEmpty();
            Exception ex = exceptions.get(exceptions.size() - 1);
            if (clazz != null && exceptionMsg != null) {
                assertThat(ex.toString()).as("Unexpected last exception class").contains(clazz);
                assertThat(ex.toString()).as("Unexpected last exception message").contains(exceptionMsg);
            } else if (clazz != null) {
                assertThat(exceptions.get(exceptions.size() - 1).getClass().getSimpleName()).as("Unexpected last exception class").isEqualTo(clazz);
            }

            commonspec.getExceptions().clear();
        }
    }

    /**
     * Check if expression defined by JSOPath (http://goessner.net/articles/JsonPath/index.html)
     * match in JSON stored in a environment variable.
     *
     * @param envVar environment variable where JSON is stored
     * @param table  data table in which each row stores one expression
     */
    @Then("^'(.+?)' matches the following cases:$")
    public void matchWithExpresion(String envVar, DataTable table) throws Exception {
        String jsonString = ThreadProperty.get(envVar);

        for (List<String> row : table.cells()) {
            String expression = row.get(0);
            String condition = row.get(1);
            String result = row.get(2);

            String value = commonspec.getJSONPathString(jsonString, expression, null);
            commonspec.evaluateJSONElementOperation(value, condition, result);
        }
    }

    /*
     * Check value stored in environment variable "is|matches|is higher than|is lower than|contains||does not contain|is different from" to value provided
     *
     * @param envVar
     * @param value
     *
     */
    @Then("^'(.+)' (is|matches|is higher than|is lower than|contains|does not contain|is different from) '(.+)'$")
    public void checkValue(String envVar, String operation, String value) throws Exception {
        switch (operation.toLowerCase()) {
            case "is":
                Assertions.assertThat(envVar).isEqualTo(value);
                break;
            case "matches":
                Assertions.assertThat(envVar).matches(value);
                break;
            case "is higher than":
                if (envVar.matches("^-?\\d+$") && value.matches("^-?\\d+$")) {
                    Assertions.assertThat(Integer.parseInt(envVar)).isGreaterThan(Integer.parseInt(value));
                } else {
                    Fail.fail("A number should be provided in order to perform a valid comparison.");
                }
                break;
            case "is lower than":
                if (envVar.matches("^-?\\d+$") && value.matches("^-?\\d+$")) {
                    Assertions.assertThat(Integer.parseInt(envVar)).isLessThan(Integer.parseInt(value));
                } else {
                    Fail.fail("A number should be provided in order to perform a valid comparison.");
                }
                break;
            case "contains":
                Assertions.assertThat(envVar).contains(value);
                break;
            case "does not contain":
                Assertions.assertThat(envVar).doesNotContain(value);
                break;
            case "is different from":
                Assertions.assertThat(envVar).isNotEqualTo(value);
                break;
            default:
                Fail.fail("Not a valid comparison. Valid ones are: is | matches | is higher than | is lower than | contains | does not contain | is different from");
        }
    }
}
