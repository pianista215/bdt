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

import cucumber.api.java.en.Given;
import cucumber.api.java.en.Then;
import cucumber.api.java.en.When;
import org.apache.zookeeper.KeeperException;
import org.assertj.core.api.Assertions;

import java.net.UnknownHostException;

/**
 * Generic Kafka Specs.
 */
public class KafkaSpec extends BaseGSpec {

    /**
     * Generic constructor.
     *
     * @param spec object
     */
    public KafkaSpec(CommonG spec) {
        this.commonspec = spec;

    }

    /**
     * Connect to Kafka.
     *
     * @param zkHost ZK host
     * @param zkPath ZK port
     * @throws UnknownHostException exception
     */
    @Given("^I connect to kafka at '(.+)' using path '(.+)'$")
    public void connectKafka(String zkHost, String zkPath) throws UnknownHostException {
        String zkPort = zkHost.split(":")[1];
        zkHost = zkHost.split(":")[0];
        commonspec.getKafkaUtils().setZkHost(zkHost, zkPort, zkPath);
        commonspec.getKafkaUtils().connect();
    }

    /**
     * Create a Kafka topic.
     *
     * @param topic_name topic name
     */
    @When("^I create a Kafka topic named '(.+?)'")
    public void createKafkaTopic(String topic_name) throws Exception {
        commonspec.getKafkaUtils().createTopic(topic_name);
    }

    /**
     * Delete a Kafka topic.
     *
     * @param topic_name topic name
     */
    @When("^I delete a Kafka topic named '(.+?)'")
    public void deleteKafkaTopic(String topic_name) throws Exception {
        commonspec.getKafkaUtils().deleteTopic(topic_name);
    }

    /**
     * Copy Kafka Topic content to file
     *
     * @param topic_name
     * @param filename
     * @param header
     * @throws Exception
     */
    @When("^I copy the kafka topic '(.*?)' to file '(.*?)' with headers '(.*?)'$")
    public void topicToFile(String topic_name, String filename, String header) throws Exception {
        commonspec.getKafkaUtils().resultsToFile(topic_name, filename, header);
    }

    /**
     * Modify partitions in a Kafka topic.
     *
     * @param topic_name    topic name
     * @param numPartitions number of partitions
     */
    @When("^I increase '(\\d+)' partitions in a Kafka topic named '(.+?)'")
    public void modifyPartitions(int numPartitions, String topic_name) throws Exception {
        commonspec.getKafkaUtils().modifyTopicPartitioning(topic_name, numPartitions);
    }


    /**
     * Sending a message in a Kafka topic.
     *
     * @param topic_name topic name
     * @param message    string that you send to topic
     */
    @When("^I send a message '(.+?)' to the kafka topic named '(.+?)'")
    public void sendAMessage(String message, String topic_name) throws Exception {
        commonspec.getKafkaUtils().sendMessage(message, topic_name);
    }

    /**
     * Check that a kafka topic exist
     *
     * @param topic_name name of topic
     */
    @Then("^A kafka topic named '(.+?)' exists")
    public void kafkaTopicExist(String topic_name) throws KeeperException, InterruptedException {
        assert commonspec.getKafkaUtils().getZkUtils().pathExists("/" + topic_name) : "There is no topic with that name";
    }

    /**
     * Check that a kafka topic not exist
     *
     * @param topic_name name of topic
     */
    @Then("^A kafka topic named '(.+?)' does not exist")
    public void kafkaTopicNotExist(String topic_name) throws KeeperException, InterruptedException {
        assert !commonspec.getKafkaUtils().getZkUtils().pathExists("/" + topic_name) : "There is a topic with that name";
    }

    /**
     * Check that the number of partitions is like expected.
     *
     * @param topic_name      Name of kafka topic
     * @param numOfPartitions Number of partitions
     * @throws Exception
     */
    @Then("^The number of partitions in topic '(.+?)' should be '(\\d+)'$")
    public void checkNumberOfPartitions(String topic_name, int numOfPartitions) throws Exception {
        Assertions.assertThat(commonspec.getKafkaUtils().getPartitions(topic_name)).isEqualTo(numOfPartitions);
    }

    @Then("^The kafka topic '(.*?)' has a message containing '(.*?)'$")
    public void checkMessages(String topic, String content) {
        assert commonspec.getKafkaUtils().readTopicFromBeginning(topic).contains(content) : "Topic does not exist or the content does not match";
    }

    @Then("^The kafka topic '(.*?)' has a message that contains '(.*?)'$")
    public void checkMessagesContent(String topic, String content) {
        assert commonspec.getKafkaUtils().checkMessageContent(topic, content) : "Topic does not exist or the content does not match";
    }

    @Then("^The kafka topic '(.*?)' has '(\\d+)' messages$")
    public void checkMessageOfTopicLentgh(String topic, int numberOfMessages) {
        Assertions.assertThat(commonspec.getKafkaUtils().checkTopicMessagesLenght(topic)).isEqualTo(numberOfMessages);
    }
}
