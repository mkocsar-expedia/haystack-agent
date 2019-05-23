/*
 *  Copyright 2017 Expedia, Inc.
 *
 *     Licensed under the Apache License, Version 2.0 (the "License");
 *     you may not use this file except in compliance with the License.
 *     You may obtain a copy of the License at
 *
 *         http://www.apache.org/licenses/LICENSE-2.0
 *
 *     Unless required by applicable law or agreed to in writing, software
 *     distributed under the License is distributed on an "AS IS" BASIS,
 *     WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *     See the License for the specific language governing permissions and
 *     limitations under the License.
 *
 */

package com.expedia.www.haystack.agent.core.app;

import com.expedia.www.haystack.agent.core.agent.AgentManager;
import com.expedia.www.haystack.agent.core.config.ConfigReader;
import com.expedia.www.haystack.agent.core.metrics.SharedMetricRegistry;

public class Launcher {

    // to prevent instantiation
    private Launcher() {}

    public static void main(String[] args) {
        setupJmxMetricReporter();

        LaunchParameters launchParameters = ArgsOperations.extractLaunchParameters(args);

        ConfigReader configReader = ConfigReader.forName(launchParameters.getConfigReaderName());
        AgentManager agentManager = new AgentManager(configReader, launchParameters.getConfigReaderArgs(), launchParameters.getConfigRefreshing());
        agentManager.start();
    }

    private static void setupJmxMetricReporter() {
        SharedMetricRegistry.startJmxMetricReporter();

        Thread jmxMetricReporterCloser = new Thread(SharedMetricRegistry::closeJmxMetricReporter);
        Runtime.getRuntime().addShutdownHook(jmxMetricReporterCloser);
    }

}
