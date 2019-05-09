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

package com.expedia.www.haystack.agent.core;

import com.expedia.www.haystack.agent.core.agent.AgentManager;
import com.expedia.www.haystack.agent.core.config.ConfigReader;
import com.expedia.www.haystack.agent.core.metrics.SharedMetricRegistry;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.HashMap;
import java.util.Map;
import java.util.ServiceLoader;

public class Application {

    private static final String CONFIG_READER_NAME_ARG = "--config-provider";
    private static final String CONFIG_REFRESHING_ARG = "--config-refreshing";

    private static final Logger logger = LoggerFactory.getLogger(Application.class);

    private static String configReaderName = "file";
    private static Map<String, String> configReaderArgs = new HashMap<>();
    private static boolean configRefreshing = false;

    // silencing PDM UseUtilityClass violation
    private Application() {}

    public static void main(String[] args) {
        parseArgs(args);

        SharedMetricRegistry.startJmxMetricReporter();

        ConfigReader configReader = findConfigReader(configReaderName);
        logger.info("Using {} with args {}, config refreshing: {}.", configReader.getClass().getName(),
                configReaderArgs, configRefreshing);

        AgentManager agentManager = new AgentManager(configReader, configReaderArgs, configRefreshing);
        agentManager.start();

        Thread jmxMetricReporterCloser = new Thread(SharedMetricRegistry::closeJmxMetricReporter);
        Runtime.getRuntime().addShutdownHook(jmxMetricReporterCloser);
    }

    private static void parseArgs(String[] args) {
        for (int index = 0; index < args.length; ++index) {
            if (args[index].equals(CONFIG_READER_NAME_ARG)) {
                configReaderName = args[index + 1];
                ++index;
            } else if (args[index].equals(CONFIG_REFRESHING_ARG)) {
                configRefreshing = true;
            } else {
                configReaderArgs.put(args[index], args[index + 1]);
                ++index;
            }
        }
    }

    private static ConfigReader findConfigReader(String configReaderName) {
        ServiceLoader<ConfigReader> configReaderLoader = ServiceLoader.load(ConfigReader.class);

        for (ConfigReader configReader : configReaderLoader) {
            if (configReader.getName().equals(configReaderName)) {
                return configReader;
            }
        }

        throw new RuntimeException("No ConfigReader service-provider was found for name \"" + configReaderName + "\".");
    }

}
