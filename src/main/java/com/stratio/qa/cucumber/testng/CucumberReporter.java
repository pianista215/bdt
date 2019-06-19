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

package com.stratio.qa.cucumber.testng;

import com.stratio.qa.utils.StepException;
import com.stratio.qa.utils.ThreadProperty;
import cucumber.api.PickleStepTestStep;
import cucumber.api.Result;
import cucumber.api.TestCase;
import cucumber.api.event.*;
import cucumber.api.event.EventListener;
import cucumber.api.formatter.StrictAware;
import cucumber.runtime.CucumberException;
import cucumber.runtime.Utils;
import cucumber.runtime.io.URLOutputStream;
import cucumber.runtime.io.UTF8OutputStreamWriter;
import gherkin.pickles.*;
import org.apache.commons.io.FileUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.w3c.dom.*;
import org.w3c.dom.Node;

import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;
import javax.xml.transform.OutputKeys;
import javax.xml.transform.Transformer;
import javax.xml.transform.TransformerException;
import javax.xml.transform.TransformerFactory;
import javax.xml.transform.dom.DOMSource;
import javax.xml.transform.stream.StreamResult;
import java.io.*;
import java.math.BigDecimal;
import java.text.DecimalFormat;
import java.text.NumberFormat;
import java.text.SimpleDateFormat;
import java.util.*;
import java.util.concurrent.ExecutionException;

public class CucumberReporter implements EventListener, StrictAware {

    public static final int DURATION_STRING = 1000000;

    public static final int DEFAULT_MAX_LENGTH = 140;

    private static final SimpleDateFormat DATE_FORMAT = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss'Z'");

    private static final String STATUS = "status";

    private final Document document;

    private final Document jUnitDocument;

    private final Element results;

    private final Element jUnitResults;

    private final Element suite;

    private final Element jUnitSuite;

    private final Element test;

    private Writer writer;

    private Writer  writerJunit;

    private Element clazz;

    private Element root;

    private Element jUnitRoot;

    private TestMethod testMethod;

    private static String callerClass;

    private final Logger logger = LoggerFactory.getLogger(this.getClass()
            .getCanonicalName());

    private EventHandler<TestCaseStarted> caseStartedHandler = new EventHandler<TestCaseStarted>() {
        @Override
        public void receive(TestCaseStarted event) {
            handleTestCaseStarted(event);
        }
    };

    private EventHandler<TestStepFinished> stepFinishedHandler = new EventHandler<TestStepFinished>() {
        @Override
        public void receive(TestStepFinished event) {
            handleTestStepFinished(event);
        }
    };

    private EventHandler<TestCaseFinished> caseFinishedHandler = new EventHandler<TestCaseFinished>() {
        @Override
        public void receive(TestCaseFinished event) {
            handleTestCaseFinished(event);
        }
    };

    private EventHandler<TestRunFinished> runFinishedHandler = new EventHandler<TestRunFinished>() {
        @Override
        public void receive(TestRunFinished event) {
            finishReport();
        }
    };

    /**
     * Constructor of cucumberReporter.
     *
     * @param url url
     * @param cClass class
     * @throws IOException exception
     */
    public CucumberReporter(String url, String cClass, String additional) throws IOException {
        URLOutputStream urlOS = null;
        try {
            urlOS = new URLOutputStream(Utils.toURL(url + cClass + additional + "TESTNG.xml"));
            this.writer = new UTF8OutputStreamWriter(urlOS);
        } catch (Exception e) {
            logger.error("error writing TESTNG.xml file", e);
        }
        try {
            urlOS = new URLOutputStream(Utils.toURL(url + cClass + additional + "JUNIT.xml"));
            this.writerJunit = new UTF8OutputStreamWriter(urlOS);
        } catch (Exception e) {
            logger.error("error writing JUNIT.xml file", e);
        }
        TestMethod.currentFeatureFile = null;
        TestMethod.treatConditionallySkippedAsFailure = false;
        TestMethod.previousTestCaseName = "";
        TestMethod.exampleNumber = 1;
        callerClass = cClass;

        try {
            document = DocumentBuilderFactory.newInstance().newDocumentBuilder().newDocument();
            results = document.createElement("testng-results");
            suite = document.createElement("suite");
            test = document.createElement("test");
            suite.appendChild(test);
            results.appendChild(suite);
            document.appendChild(results);
            jUnitDocument = DocumentBuilderFactory.newInstance().newDocumentBuilder().newDocument();
            jUnitResults = jUnitDocument.createElement("testsuites");
            jUnitSuite = jUnitDocument.createElement("testsuite");
            jUnitDocument.appendChild(jUnitResults);
            jUnitResults.appendChild(jUnitSuite);
        } catch (ParserConfigurationException e) {
            throw new CucumberException("Error initializing DocumentBuilder.", e);
        }
    }

    @Override
    public void setEventPublisher(EventPublisher publisher) {
        publisher.registerHandlerFor(TestCaseStarted.class, caseStartedHandler);
        publisher.registerHandlerFor(TestCaseFinished.class, caseFinishedHandler);
        publisher.registerHandlerFor(TestStepFinished.class, stepFinishedHandler);
        publisher.registerHandlerFor(TestRunFinished.class, runFinishedHandler);
    }

    @Override
    public void setStrict(boolean strict) {
    }

    private void handleTestCaseStarted(TestCaseStarted event) {
        if (TestMethod.currentFeatureFile == null || !TestMethod.currentFeatureFile.equals(event.testCase.getUri())) {
            TestMethod.currentFeatureFile = event.testCase.getUri();
            TestMethod.previousTestCaseName = "";
            TestMethod.exampleNumber = 1;
            clazz = document.createElement("class");
            clazz.setAttribute("name", callerClass);
            test.appendChild(clazz);
        }
        testMethod = new TestMethod(event.testCase);
        String scenarioName = testMethod.calculateElementName(event.testCase);
        //TestNG
        root = document.createElement("test-method");
        clazz.appendChild(root);
        root.setAttribute("name", scenarioName);
        root.setAttribute("started-at", DATE_FORMAT.format(new Date()));
        //JUnit
        jUnitRoot = testMethod.createElement(jUnitDocument);
        jUnitRoot.setAttribute("classname", callerClass);
        jUnitRoot.setAttribute("name", scenarioName);
        jUnitSuite.appendChild(jUnitRoot);
        increaseAttributeValue(jUnitSuite, "tests");
    }

    private void handleTestStepFinished(TestStepFinished event) {
        if (event.testStep instanceof PickleStepTestStep) {
            testMethod.steps.add((PickleStepTestStep) event.testStep);
            testMethod.results.add(event.result);
        } else {
            testMethod.hooks.add(event.result);
        }
        if (event.result.getStatus() == Result.Type.FAILED) {
            StringBuilder stepFailedText = new StringBuilder();
            stepFailedText.append("STEP FAILED!!!");
            if (StepException.INSTANCE.getException() != null) {
                stepFailedText.append(" - ").append(StepException.INSTANCE.getException().getClass().getCanonicalName());
                if (StepException.INSTANCE.getException().getMessage() != null) {
                    stepFailedText.append(": ").append(StepException.INSTANCE.getException().getMessage());
                }
                try {
                    StackTraceElement[] elements = StepException.INSTANCE.getException().getStackTrace();
                    stepFailedText.append(" | ").append(elements[0]);
                } catch (Exception ignore) {
                }
            }
            logger.error(stepFailedText.toString());
        }
    }

    private void handleTestCaseFinished(TestCaseFinished event) {
        testMethod.finish(document, root, jUnitDocument, jUnitRoot, event.result);
    }

    private void finishReport() {
        try {
            // TestNG
            results.setAttribute("total", String.valueOf(getElementsCountByAttribute(suite, "status", ".*")));
            results.setAttribute("passed", String.valueOf(getElementsCountByAttribute(suite, "status", "PASS")));
            results.setAttribute("failed", String.valueOf(getElementsCountByAttribute(suite, "status", "FAIL")));
            results.setAttribute("skipped", String.valueOf(getElementsCountByAttribute(suite, "status", "SKIP")));
            suite.setAttribute("name", CucumberReporter.class.getName());
            suite.setAttribute("duration-ms", String.valueOf(getTotalDuration(suite.getElementsByTagName("test-method"))));
            test.setAttribute("name", CucumberReporter.class.getName());
            test.setAttribute("duration-ms", String.valueOf(getTotalDuration(suite.getElementsByTagName("test-method"))));

            Transformer transformer = TransformerFactory.newInstance().newTransformer();
            transformer.setOutputProperty(OutputKeys.INDENT, "yes");
            StreamResult streamResult = new StreamResult(writer);
            DOMSource domSource = new DOMSource(document);
            transformer.transform(domSource, streamResult);
            closeQuietly(writer);

            // JUnit
            // set up a transformer
            jUnitSuite.setAttribute("name", callerClass + "." + TestSourcesModelUtil.INSTANCE.getTestSourcesModel().getFeatureName(TestMethod.currentFeatureFile));
            jUnitSuite.setAttribute("failures", String.valueOf(jUnitSuite.getElementsByTagName("failure").getLength()));
            jUnitSuite.setAttribute("skipped", String.valueOf(jUnitSuite.getElementsByTagName("skipped").getLength()));
            jUnitSuite.setAttribute("time", sumTimes(jUnitSuite.getElementsByTagName("testcase")));
            jUnitSuite.setAttribute("tests", String.valueOf(getElementsCountByAttribute(suite, STATUS, ".*")));
            jUnitSuite.setAttribute("errors", String.valueOf(getElementsCountByAttribute(suite, STATUS, "FAIL")));
            jUnitSuite.setAttribute("timestamp", new java.util.Date().toString());
            jUnitSuite.setAttribute("time",
                    String.valueOf(BigDecimal.valueOf(getTotalDurationMs(suite.getElementsByTagName("test-method"))).setScale(3, BigDecimal.ROUND_HALF_UP).floatValue()));

            if (jUnitSuite.getElementsByTagName("testcase").getLength() == 0) {
                addDummyTestCase(); // to avoid failed Jenkins jobs
            }
            TransformerFactory transfac = TransformerFactory.newInstance();
            Transformer trans = transfac.newTransformer();
            trans.setOutputProperty(OutputKeys.INDENT, "yes");
            StreamResult result = new StreamResult(writerJunit);
            DOMSource source = new DOMSource(jUnitDocument);
            trans.transform(source, result);
            closeQuietly(writerJunit);
        } catch (TransformerException e) {
            throw new CucumberException("Error transforming report.", e);
        }
    }

    private int getElementsCountByAttribute(Node node, String attributeName, String attributeValue) {
        int count = 0;

        for (int i = 0; i < node.getChildNodes().getLength(); i++) {
            count += getElementsCountByAttribute(node.getChildNodes().item(i), attributeName, attributeValue);
        }

        NamedNodeMap attributes = node.getAttributes();
        if (attributes != null) {
            Node namedItem = attributes.getNamedItem(attributeName);
            if (namedItem != null && namedItem.getNodeValue().matches(attributeValue)) {
                count++;
            }
        }
        return count;
    }

    private double getTotalDuration(NodeList testCaseNodes) {
        double totalDuration = 0;
        for (int i = 0; i < testCaseNodes.getLength(); i++) {
            try {
                String duration = "0";
                Node durationms = testCaseNodes.item(i).getAttributes().getNamedItem("duration-ms");
                if (durationms != null) {
                    duration = durationms.getNodeValue();
                }
                totalDuration += Double.parseDouble(duration);
            } catch (NumberFormatException e) {
                throw new CucumberException(e);
            }
        }
        return totalDuration;
    }

    private double getTotalDurationMs(NodeList testCaseNodes) {
        return getTotalDuration(testCaseNodes) / 1000;
    }

    private void increaseAttributeValue(Element element, String attribute) {
        int value = 0;
        if (element.hasAttribute(attribute)) {
            value = Integer.parseInt(element.getAttribute(attribute));
        }
        element.setAttribute(attribute, String.valueOf(++value));
    }

    private void addDummyTestCase() {
        Element dummy = jUnitDocument.createElement("testcase");
        dummy.setAttribute("classname", "dummy");
        dummy.setAttribute("name", "dummy");
        jUnitSuite.appendChild(dummy);
        Element skipped = jUnitDocument.createElement("skipped");
        skipped.setAttribute("message", "No features found");
        dummy.appendChild(skipped);
    }

    private String sumTimes(NodeList testCaseNodes) {
        double totalDurationSecondsForAllTimes = 0.0d;
        for (int i = 0; i < testCaseNodes.getLength(); i++) {
            try {
                double testCaseTime =
                        Double.parseDouble(testCaseNodes.item(i).getAttributes().getNamedItem("time").getNodeValue());
                totalDurationSecondsForAllTimes += testCaseTime;
            } catch (NumberFormatException e) {
                throw new CucumberException(e);
            } catch (NullPointerException e) {
                throw new CucumberException(e);
            }
        }
        DecimalFormat nfmt = (DecimalFormat) NumberFormat.getNumberInstance(Locale.US);
        nfmt.applyPattern("0.######");
        return nfmt.format(totalDurationSecondsForAllTimes);
    }

    private static void closeQuietly(Closeable out) {
        try {
            out.close();
        } catch (IOException ignored) {
            // go gentle into that good night
        }
    }

    public static class TestMethod {

        static String currentFeatureFile;

        static String previousTestCaseName;

        static int exampleNumber;

        static boolean treatConditionallySkippedAsFailure = false;

        private final List<Result> results = new ArrayList<Result>();

        private TestCase scenario = null;

        private List<PickleStepTestStep> steps = new ArrayList<PickleStepTestStep>();

        private List<Result> hooks = new ArrayList<Result>();

        private static final DecimalFormat NUMBER_FORMAT = (DecimalFormat) NumberFormat.getNumberInstance(Locale.US);

        static {
            NUMBER_FORMAT.applyPattern("0.######");
        }

        public TestMethod(TestCase scenario) {
            this.scenario = scenario;
        }

        /**
         * Builds a test result xml document
         *
         * @param doc report document
         * @param element scenario execution result
         * @param docJunit docJunit report document
         * @param Junit Junit scenario execution result
         */
        public void finish(Document doc, Element element, Document docJunit, Element Junit, Result eventResult) {
            if (steps.isEmpty()) {
                createExceptionJunit(docJunit, "The scenario has no steps", "");
            }
            //JUnit
            Junit.setAttribute("time", calculateTotalDurationString(eventResult));
            //TestNG
            element.setAttribute("duration-ms", String.valueOf(calculateTotalDurationString()));
            element.setAttribute("finished-at", DATE_FORMAT.format(new Date()));

            StringBuilder stringBuilder = new StringBuilder();
            addStepAndResultListing(stringBuilder);
            Result skipped = null;
            Result failed = null;
            for (Result result : results) {
                if (result.is(Result.Type.FAILED) || result.is(Result.Type.AMBIGUOUS)) {
                    failed = result;
                }
                if (result.is(Result.Type.UNDEFINED) || result.is(Result.Type.PENDING)) {
                    skipped = result;
                }
            }
            for (Result result : hooks) {
                if (failed == null && result.is(Result.Type.FAILED)) {
                    failed = result;
                }
            }
            if (failed != null) {
                element.setAttribute("status", "FAIL");
                StringWriter stringWriter = new StringWriter();
                if (failed.getErrorMessage().contains("An important scenario has failed!")) {
                    String message = "This scenario was skipped because an important scenario has failed.";
                    element.setAttribute(STATUS, "SKIP");
                    Element exception = createException(doc, "NonRealException", message, " ");
                    element.appendChild(exception);
                    Element skippedElementJunit = docJunit.createElement("skipped");
                    Junit.appendChild(skippedElementJunit);
                    Element systemOut = systemOutPrintJunit(docJunit, message);
                    Junit.appendChild(systemOut);
                } else if (failed.getErrorMessage().contains("NonReplaceableException")) {
                    Element exception = createException(doc, "The scenario has unreplaced variables.",
                            "The scenario has unreplaced variables.", " ");
                    element.appendChild(exception);
                    Element exceptionJunit = createExceptionJunit(docJunit, "The scenario has unreplaced variables.", " ");
                    Junit.appendChild(exceptionJunit);
                    Element systemOut = systemOutPrintJunit(docJunit, stringBuilder.toString());
                    Junit.appendChild(systemOut);
                } else {
                    failed.getError().printStackTrace(new PrintWriter(stringWriter));
                    Element exception = createException(doc, failed.getError().getClass().getName(), stringBuilder.toString(), stringWriter.toString());
                    element.appendChild(exception);
                    Element exceptionJunit = createExceptionJunit(docJunit, stringBuilder.toString(), stringWriter.toString());
                    Junit.appendChild(exceptionJunit);
                }
            } else if (skipped != null) {
                element.setAttribute("status", "SKIP");
                Element exception = createException(doc, "NonRealException", stringBuilder.toString(), " ");
                element.appendChild(exception);
                Element skippedElementJunit = docJunit.createElement("skipped");
                Junit.appendChild(skippedElementJunit);
                Element systemOut = systemOutPrintJunit(docJunit, stringBuilder.toString());
                Junit.appendChild(systemOut);
            } else {
                element.setAttribute("status", "PASS");
                Element exception = createException(doc, "NonRealException", stringBuilder.toString(), " ");
                element.appendChild(exception);
                Element systemOut = systemOutPrintJunit(docJunit, stringBuilder.toString());
                Junit.appendChild(systemOut);
            }
        }

        private double calculateTotalDurationString() {
            double totalDurationNanos = 0;
            for (Result r : results) {
                totalDurationNanos += r.getDuration() == null ? 0 : r.getDuration();
            }
            for (Result r : hooks) {
                totalDurationNanos += r.getDuration() == null ? 0 : r.getDuration();
            }
            return totalDurationNanos / DURATION_STRING;
        }

        private String calculateTotalDurationString(Result result) {
            return NUMBER_FORMAT.format(((double) result.getDuration()) / 1000000000);
        }

        public void addStepAndResultListing(StringBuilder sb) {
            for (int i = 0; i < steps.size(); i++) {
                int length = sb.length();
                String resultStatus = "not executed";
                if (i < results.size()) {
                    resultStatus = results.get(i).getStatus().lowerCaseName();
                }
                PickleStep replacedStep = TestSourcesModelUtil.INSTANCE.getTestSourcesModel().getReplacedStep(currentFeatureFile, steps.get(i).getStepLine());
                sb.append(TestSourcesModelUtil.INSTANCE.getTestSourcesModel().getKeywordFromSource(currentFeatureFile, steps.get(i).getStepLine()));
                sb.append(replacedStep != null ? replacedStep.getText() : steps.get(i).getStepText());
                int len = sb.length() - length;
                if (!steps.get(i).getStepArgument().isEmpty()) {
                    Argument argument = replacedStep != null ? replacedStep.getArgument().get(0) : steps.get(i).getStepArgument().get(0);
                    if (argument instanceof PickleTable) {
                        for (PickleRow row : ((PickleTable) argument).getRows()) {
                            StringBuilder strrowBuilder = new StringBuilder();
                            strrowBuilder.append("| ");
                            for (PickleCell cell : row.getCells()) {
                                strrowBuilder.append(cell.getValue()).append(" | ");
                            }
                            String strrow = strrowBuilder.toString();
                            len = strrow.length() + 11;
                            sb.append("\n           ");
                            sb.append(strrow);
                        }
                    }
                }
                do {
                    sb.append(".");
                    len++;
                } while (len < DEFAULT_MAX_LENGTH);
                sb.append(resultStatus);
                sb.append("\n");
            }
            String cap;
            if (!("".equals(cap = hasCapture(scenario.getName())))) {
                sb.append("evidence @ " + System.getProperty("BUILD_URL", "") + "/artifact/testsAT/" + cap.replaceAll("", ""));
            }
        }

        private String hasCapture(String scen) {
            String testSuffix = System.getProperty("TESTSUFFIX");
            File dir;
            if (testSuffix != null) {
                dir = new File("./target/executions/" + testSuffix + "/");
            } else {
                dir = new File("./target/executions/");
            }
            final String[] imgext = {"png"};
            Collection<File> files = FileUtils.listFiles(dir, imgext, true);

            for (File file : files) {
                if (file.getPath().contains(scen.replaceAll(" ", "_")) &&
                        file.getName().contains("assert")) {
                    return file.toString();
                }
            }
            return "";
        }

        private Element createException(Document doc, String clazz, String message, String stacktrace) {
            Element exceptionElement = doc.createElement("exception");
            exceptionElement.setAttribute("class", clazz);

            if (message != null) {
                Element messageElement = doc.createElement("message");
                messageElement.appendChild(doc.createCDATASection("\r\n<pre>\r\n" + message + "\r\n</pre>\r\n"));
                exceptionElement.appendChild(messageElement);
            }

            Element stacktraceElement = doc.createElement("full-stacktrace");
            stacktraceElement.appendChild(doc.createCDATASection(stacktrace));
            exceptionElement.appendChild(stacktraceElement);

            return exceptionElement;
        }

        private Element createElement(Document doc) {
            return doc.createElement("testcase");
        }

        public String calculateElementName(cucumber.api.TestCase testCase) {
            String testCaseName = testCase.getName();
            if (testCaseName.equals(previousTestCaseName)) {
                exampleNumber++;
                ThreadProperty.set("dataSet", String.valueOf(exampleNumber));
                return Utils.getUniqueTestNameForScenarioExample(testCaseName, exampleNumber);
            } else {
                ThreadProperty.set("dataSet", "");
                previousTestCaseName = testCase.getName();
                exampleNumber = 1;
                return testCaseName;
            }
        }

        private Element createExceptionJunit(Document doc, String message, String stacktrace) {
            Element exceptionElement = doc.createElement("failure");
            if (message != null) {
                exceptionElement.setAttribute("message", "\r\n" + message + "\r\n");
            }
            exceptionElement.appendChild(doc.createCDATASection(stacktrace));
            return exceptionElement;
        }

        private Element systemOutPrintJunit(Document doc, String message) {
            Element systemOut = doc.createElement("system-out");
            systemOut.appendChild(doc.createCDATASection("\r\n" + message + "\r\n"));
            return systemOut;
        }
    }
}
