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

package com.expedia.www.haystack.agent.core.agent;

import com.expedia.www.haystack.agent.core.Agent;
import com.expedia.www.haystack.agent.core.MessageUtils;
import com.expedia.www.haystack.agent.core.config.ConfigReader;
import com.expedia.www.haystack.agent.core.config.ConfigurationHelpers;
import com.typesafe.config.Config;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.*;

public class AgentManager {

    private static final Logger logger = LoggerFactory.getLogger(AgentManager.class);

    private ConfigReader configReader;
    private Map<String, String> configReaderArgs;
    private boolean configReloading;

    private Map<String, Agent> agents = new HashMap<>();
    private Map<Agent, Config> runningAgentsWithConfig = new HashMap<>();

    public AgentManager(ConfigReader configReader, Map<String, String> configReaderArgs, boolean configReloading) {
        this.configReader = configReader;
        this.configReaderArgs = configReaderArgs;
        this.configReloading = configReloading;

        loadAgents();
    }

    public void start() {
        Config loadedEntireConfig;
        try {
            logger.info("Loading config...");
            loadedEntireConfig = configReader.read(configReaderArgs);
            logger.info("Loaded config:\n{}", MessageUtils.configToFormattedString(loadedEntireConfig));
        } catch (Exception exception) {
            throw new RuntimeException("Failed to load config.", exception);
        }

        Map<String, Config> loadedAgentConfigs = ConfigurationHelpers.readAgentConfigs(loadedEntireConfig);
        loadedAgentConfigs.forEach((agentName, agentConfig) -> {
            try {
                applyAgentConfig(agentName, agentConfig);
            } catch (Exception exception) {
                logger.error("An exception occurred while applying config to Agent \"{}\". config:\n{}", agentName,
                        MessageUtils.configToFormattedString(agentConfig), exception);
            }
        });

        if (configReloading) {
            // TODO: implement config reloading; probably via ScheduledThreadPoolExecutor
        }

        Thread agentCloserHook = new Thread(() -> {
            Set<Agent> agents = new HashSet<>(runningAgentsWithConfig.keySet());
            agents.forEach(this::stopAgent);
        });
        Runtime.getRuntime().addShutdownHook(agentCloserHook);
    }

    private void loadAgents() {
        logger.info("Loading Agents...");

        List<String> agentClassNames = new LinkedList<>();
        ServiceLoader<Agent> agentLoader = ServiceLoader.load(Agent.class);
        agentLoader.forEach(agent -> {
            agents.put(agent.getName(), agent);
            agentClassNames.add(agent.getClass().getName());
        });

        logger.info("Loaded Agents: {}", String.join(", ", agentClassNames));
    }

    private void applyAgentConfig(String agentName, Config agentConfig) {
        Agent agent = findAgent(agentName);

        boolean disabled = agentConfig.hasPath("enabled") && ! agentConfig.getBoolean("enabled");
        boolean runningBefore = runningAgentsWithConfig.containsKey(agent);

        if (disabled) {
            if (runningBefore) {
                stopAgent(agent);
            } else {
                logger.info("Ignoring disabled {}. config:\n{}", agent.getClass().getName(),
                        MessageUtils.configToFormattedString(agentConfig));
            }
        } else {
            if (runningBefore) {
                boolean configChanged = ! runningAgentsWithConfig.get(agent).equals(agentConfig);
                if (configChanged) {
                    reconfigureAgent(agent, agentConfig);
                }
            } else {
                startAgent(agent, agentConfig);
            }
        }

    }

    private Agent findAgent(String agentName) {
        if (! agents.containsKey(agentName)) {
            throw new RuntimeException("No Agent service-provider was found for name \"" + agentName + "\".");
        }
        return agents.get(agentName);
    }


    private void startAgent(Agent agent, Config agentConfig) {
        logger.info("Starting {} with config:\n{}", agent.getClass().getName(),
                MessageUtils.configToFormattedString(agentConfig));

        new Thread(() -> {
            try {
                agent.initialize(agentConfig);
            } catch (Exception exception) {
                logger.error("An exception occurred wile starting {} with config:\n{}", agent.getClass().getName(),
                        MessageUtils.configToFormattedString(agentConfig), exception);
            }
        }).start();

        runningAgentsWithConfig.put(agent, agentConfig);
    }

    private void reconfigureAgent(Agent agent, Config newConfig) {
        logger.info("Reconfiguring {} with new config:\n{}", agent.getClass().getName(),
                MessageUtils.configToFormattedString(newConfig));

        try {
            agent.reconfigure(newConfig);
        } catch (Exception exception) {
            logger.error("An exception occurred wile reconfiguring {} with new config:\n{}", agent.getClass().getName(),
                    MessageUtils.configToFormattedString(newConfig), exception);
        }

        runningAgentsWithConfig.put(agent, newConfig);
    }

    private void stopAgent(Agent agent) {
        logger.info("Stopping {}", agent.getClass().getName());

        try {
            agent.close();
        } catch (Exception exception) {
            logger.error("An exception occurred while stopping {}", agent.getClass().getName(), exception);
        }

        runningAgentsWithConfig.remove(agent);
    }

}
