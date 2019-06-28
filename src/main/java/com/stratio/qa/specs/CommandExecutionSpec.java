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

import com.stratio.qa.utils.RemoteSSHConnection;
import cucumber.api.java.en.Given;
import cucumber.api.java.en.Then;
import org.assertj.core.api.Assertions;

import java.io.File;

import static com.stratio.qa.assertions.Assertions.assertThat;

/**
 * Generic Command Execution Specs.
 */
public class CommandExecutionSpec extends BaseGSpec {

    /**
     * Generic constructor.
     *
     * @param spec object
     */
    public CommandExecutionSpec(CommonG spec) {
        this.commonspec = spec;

    }

    /**
     * Opens a ssh connection to remote host
     *
     * @param remoteHost remote host
     * @param user       remote user
     * @param password   (required if pemFile null)
     * @param pemFile    (required if password null)
     * @throws Exception exception
     */
    @Given("^I open a ssh connection to '(.+?)'( in port '(.+?)')? with user '(.+?)'( and password '(.+?)')?( using pem file '(.+?)')?$")
    public void openSSHConnection(String remoteHost, String remotePort, String user, String password, String pemFile) throws Exception {
        if ((pemFile == null) || (pemFile.equals("none"))) {
            if (password == null) {
                throw new Exception("You have to provide a password or a pem file to be used for connection");
            }
            commonspec.setRemoteSSHConnection(new RemoteSSHConnection(user, password, remoteHost, remotePort, null));
            commonspec.getLogger().debug("Opening ssh connection with password: { " + password + "}", commonspec.getRemoteSSHConnection());
        } else {
            File pem = new File(pemFile);
            if (!pem.exists()) {
                throw new Exception("Pem file: " + pemFile + " does not exist");
            }
            commonspec.setRemoteSSHConnection(new RemoteSSHConnection(user, null, remoteHost, remotePort, pemFile));
            commonspec.getLogger().debug("Opening ssh connection with pemFile: {}", commonspec.getRemoteSSHConnection());
        }
    }

    /*
     * Copies file/s from remote system into local system
     *
     * @param remotePath path where file is going to be copy
     * @param localPath path where file is located
     * @throws Exception exception
     *
     */
    @Given("^I inbound copy '(.+?)' through a ssh connection to '(.+?)'$")
    public void copyFromRemoteFile(String remotePath, String localPath) throws Exception {
        commonspec.getRemoteSSHConnection().copyFrom(remotePath, localPath);
    }


    /**
     * Copies file/s from local system to remote system
     *
     * @param remotePath path where file is going to be copied
     * @param localPath  path where file is located
     * @throws Exception exception
     */
    @Given("^I outbound copy '(.+?)' through a ssh connection to '(.+?)'$")
    public void copyToRemoteFile(String localPath, String remotePath) throws Exception {
        commonspec.getRemoteSSHConnection().copyTo(localPath, remotePath);
    }


    /**
     * Executes the command specified in local system
     *
     * @param command    command to be run locally
     * @param sExitStatus command exit status
     * @param envVar     environment variable name
     * @throws Exception exception
     **/
    @Given("^I run '(.+?)' locally( with exit status '(\\d+)')?( and save the value in environment variable '(.+?)')?$")
    public void executeLocalCommand(String command, String sExitStatus, String envVar) throws Exception {
        Integer exitStatus = sExitStatus != null ? Integer.valueOf(sExitStatus) : null;
        if (exitStatus == null) {
            exitStatus = 0;
        }

        commonspec.runLocalCommand(command);
        commonspec.runCommandLoggerAndEnvVar(exitStatus, envVar, Boolean.TRUE);

        Assertions.assertThat(commonspec.getCommandExitStatus()).isEqualTo(exitStatus);
    }

    /**
     * Executes the command specified in remote system
     *
     * @param command    command to be run locally
     * @param sExitStatus command exit status
     * @param envVar     environment variable name
     * @throws Exception exception
     **/
    @Given("^I run '(.+?)' in the ssh connection( with exit status '(.+?)')?( and save the value in environment variable '(.+?)')?$")
    public void executeCommand(String command, String sExitStatus, String envVar) throws Exception {
        Integer exitStatus = sExitStatus != null ? Integer.valueOf(sExitStatus) : null;
        if (exitStatus == null) {
            exitStatus = 0;
        }

        commonspec.executeCommand(command, exitStatus, envVar);
    }

    /**
     * Checks if {@code expectedCount} text is found, whithin a {@code timeout} in the output of local command {@code command}
     * and the exit status is the specified in {@code exitStatus}.
     * Each negative lookup is followed by a wait of {@code wait} seconds.
     *
     * @param timeout
     * @param wait
     * @param command
     * @param search
     * @param sExitStatus
     * @throws InterruptedException
     */
    @Then("^in less than '(\\d+)' seconds, checking each '(\\d+)' seconds, the local command output '(.+?)' contains '(.*?)'( with exit status '(\\d+)')?$")
    public void assertCommandExistsOnTimeOutLocal(Integer timeout, Integer wait, String command, String search, String sExitStatus) throws Exception {
        Integer exitStatus = sExitStatus != null ? Integer.valueOf(sExitStatus) : null;
        Boolean found = false;
        for (int i = 0; (i <= timeout); i += wait) {
            if (found) {
                break;
            }
            commonspec.getLogger().debug("Checking local output value");
            commonspec.runLocalCommand(command);
            try {
                if (exitStatus != null) {
                    assertThat(commonspec.getCommandExitStatus()).isEqualTo(exitStatus);
                }
                assertThat(commonspec.getCommandResult()).as("Contains " + search + ".").contains(search);
                found = true;
                timeout = i;
            } catch (AssertionError e) {
                commonspec.getLogger().info("Local command output don't found yet after " + i + " seconds");
                Thread.sleep(wait * 1000);
            }
        }

        if (!found) {
            throw new Exception("Local command output don't found yet after " + timeout + " seconds");
        }
        commonspec.getLogger().info("Local command output found after " + timeout + " seconds");
    }

    /**
     * Checks if {@code expectedCount} text is found, whithin a {@code timeout} in the output of command {@code command}
     * and the exit status is the specified in {@code exitStatus}.
     * Each negative lookup is followed by a wait of {@code wait} seconds.
     *
     * @param timeout
     * @param wait
     * @param command
     * @param search
     * @param sExitStatus
     * @throws InterruptedException
     */
    @Then("^in less than '(\\d+)' seconds, checking each '(\\d+)' seconds, the command output '(.+?)' contains '(.*?)'( with exit status '(\\d+)')?$")
    public void assertCommandExistsOnTimeOut(Integer timeout, Integer wait, String command, String search, String sExitStatus) throws Exception {
        Integer exitStatus = sExitStatus != null ? Integer.valueOf(sExitStatus) : null;
        Boolean found = false;
        command = "set -o pipefail && alias grep='grep --color=never' && " + command;
        for (int i = 0; (i <= timeout); i += wait) {
            if (found) {
                break;
            }
            commonspec.getLogger().debug("Checking output value");
            commonspec.getRemoteSSHConnection().runCommand(command);
            commonspec.setCommandResult(commonspec.getRemoteSSHConnection().getResult());
            try {
                if (exitStatus != null) {
                    assertThat(commonspec.getRemoteSSHConnection().getExitStatus()).isEqualTo(exitStatus);
                }
                assertThat(commonspec.getCommandResult()).as("Contains " + search + ".").contains(search);
                found = true;
                timeout = i;
            } catch (AssertionError e) {
                commonspec.getLogger().info("Command output don't found yet after " + i + " seconds");
                Thread.sleep(wait * 1000);
            }
        }

        if (!found) {
            throw new Exception("Command output don't found yet after " + timeout + " seconds");
        }
        commonspec.getLogger().info("Command output found after " + timeout + " seconds");
    }

    /**
     * Check the existence of a text at a command output
     *
     * @param search
     **/
    @Then("^the command output contains '(.+?)'$")
    public void findShellOutput(String search) throws Exception {
        assertThat(commonspec.getCommandResult()).as("Contains " + search + ".").contains(search);
    }

    /**
     * Check the command output is empty
     *
     **/
    @Then("^the command output is empty$")
    public void emptyShellOutput() {
        assertThat(commonspec.getCommandResult()).as("Command output should be empty").isEmpty();
    }

    /**
     * Check the non existence of a text at a command output
     *
     * @param search
     **/
    @Then("^the command output does not contain '(.+?)'$")
    public void notFindShellOutput(String search) throws Exception {
        assertThat(commonspec.getCommandResult()).as("NotContains " + search + ".").doesNotContain(search);
    }

    /**
     * Check the exitStatus of previous command execution matches the expected one
     *
     * @param expectedExitStatus
     * @deprecated Success exit status is directly checked in the "execute remote command" method, so this is not
     * needed anymore.
     **/
    @Deprecated
    @Then("the command exit status is '{int}'")
    public void checkShellExitStatus(Integer expectedExitStatus) throws Exception {
        assertThat(commonspec.getCommandExitStatus()).as("Is equal to " + expectedExitStatus + ".").isEqualTo(expectedExitStatus);
    }
}
