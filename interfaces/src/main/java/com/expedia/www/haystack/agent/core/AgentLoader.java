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

import com.expedia.www.haystack.agent.core.config.AgentConfig;
import com.expedia.www.haystack.agent.core.config.Config;
import com.expedia.www.haystack.agent.core.config.ConfigReader;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Optional;
import java.util.ServiceConfigurationError;
import java.util.ServiceLoader;

public class AgentLoader {

    private static Logger LOGGER = LoggerFactory.getLogger(AgentLoader.class);

    private AgentLoader() { }

    private void run(String configProviderName) throws Exception {
        final Config config = loadConfig(configProviderName);
        final ServiceLoader<Agent> agentLoader = ServiceLoader.load(Agent.class);
        for (final Agent agent : agentLoader) {
            final Optional<AgentConfig> mayBeAgent = config.getAgentConfigs()
                    .stream()
                    .filter((c) -> c.getName().equalsIgnoreCase(agent.getName())).findFirst();
            if(mayBeAgent.isPresent()) {
                LOGGER.info("Initializing agent={} with config={}", agent.getName(), mayBeAgent.get());
                agent.initialize(mayBeAgent.get());
            }
        }
    }

    private Config loadConfig(final String configProviderName) throws Exception {
        final ServiceLoader<ConfigReader> configLoader = ServiceLoader.load(ConfigReader.class);
        for (final ConfigReader reader : configLoader) {
            if (reader.getName().equalsIgnoreCase(configProviderName)) {
                return reader.read();
            }
        }
        throw new ServiceConfigurationError("Fail to load the config provider for type = " + configProviderName);
    }

    public static void main(String[] args) throws Exception {
        final String configProviderName = (args != null && args.length > 0) ? args[0] : "file";
        new AgentLoader().run(configProviderName);
    }
}