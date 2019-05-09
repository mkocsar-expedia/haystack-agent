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
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

public class AgentManager {

    private static final long CONFIG_REFRESHING_RATE_IN_SECONDS = 5;

    private static final Logger logger = LoggerFactory.getLogger(AgentManager.class);

    private ConfigReader configReader;
    private Map<String, String> configReaderArgs;
    private boolean configRefreshing;

    private Config currentApplicationConfig;

    private Map<String, Agent> availableAgents = new HashMap<>();
    private Map<Agent, Config> runningAgentsWithConfig = new HashMap<>();

    public AgentManager(ConfigReader configReader, Map<String, String> configReaderArgs, boolean configRefreshing) {
        this.configReader = configReader;
        this.configReaderArgs = configReaderArgs;
        this.configRefreshing = configRefreshing;

        loadAgents();
    }

    public void start() {
        Config initialApplicationConfig = loadInitialApplicationConfig();
        applyApplicationConfig(initialApplicationConfig);

        if (configRefreshing) {
            scheduleConfigRefresher();
        }

        registerAgentCloserShutdownHook();
    }

    private void loadAgents() {
        List<String> agentClassesAndNames = new LinkedList<>();
        ServiceLoader<Agent> agentLoader = ServiceLoader.load(Agent.class);
        agentLoader.forEach(agent -> {
            availableAgents.put(agent.getName(), agent);
            agentClassesAndNames.add(agent.getClass().getName() + " (" + agent.getName() + ")");
        });

        logger.info("Loaded Agents: {}", String.join(", ", agentClassesAndNames));
    }

    private Config loadInitialApplicationConfig() {
        Config initialApplicationConfig;
        try {
            initialApplicationConfig = configReader.read(configReaderArgs);
        } catch (Exception exception) {
            logger.error("An exception occurred while loading initial configuration. Shutting down.", exception);
            throw new RuntimeException(exception);
        }
        logger.info("Successfully loaded initial configuration.");
        return initialApplicationConfig;
    }

    private void applyApplicationConfig(Config newApplicationConfig) {
        logger.info("Applying application configuration:\n{}",
                MessageUtils.configToFormattedString(newApplicationConfig));

        Map<Agent, Config> newAgentConfigs = extractAgentConfigs(newApplicationConfig);

        Set<Agent> runningAgents = new HashSet<>(runningAgentsWithConfig.keySet());
        runningAgents.forEach(agent -> {
            if (! newAgentConfigs.containsKey(agent)) {
                logger.info("Currently running {} is not declared in the new configuration. Stopping it.",
                        agent.getClass().getName());
                stopAgent(agent);
            }
        });

        newAgentConfigs.forEach(this::applyAgentConfig);

        currentApplicationConfig = newApplicationConfig;
    }

    private Map<Agent, Config> extractAgentConfigs(Config applicationConfig) {
        Map<Agent, Config> agentConfigs = new HashMap<>();
        Map<String, Config> agentNamesAndConfigs = ConfigurationHelpers.readAgentConfigs(applicationConfig);
        agentNamesAndConfigs.forEach((agentName, agentConfig) -> {
            if (availableAgents.containsKey(agentName)) {
                agentConfigs.put(availableAgents.get(agentName), agentConfig);
            } else {
                logger.error("No Agent implementation is available for name {}. Ignoring corresponding agent" +
                        " declaration.", agentName);
            }
        });
        return agentConfigs;
    }

    private void applyAgentConfig(Agent agent, Config newAgentConfig) {
        boolean disabled = newAgentConfig.hasPath("enabled") && ! newAgentConfig.getBoolean("enabled");
        boolean currentlyRunning = runningAgentsWithConfig.containsKey(agent);

        if (disabled) {
            if (currentlyRunning) {
                logger.info("Currently running {} is disabled in the new configuration. Stopping it.",
                        agent.getClass().getName());
                stopAgent(agent);
            } else {
                logger.info("Ignoring disabled {}.", agent.getClass().getName());
            }
        } else {
            if (currentlyRunning) {
                boolean configChanged = ! runningAgentsWithConfig.get(agent).equals(newAgentConfig);
                if (configChanged) {
                    logger.info("The configuration of currently running {} has changed. Reconfiguring it. New configuration:\n{}",
                            agent.getClass().getName(), MessageUtils.configToFormattedString(newAgentConfig));
                    reconfigureAgent(agent, newAgentConfig);
                } else {
                    logger.info("The configuration of currently running {} hasn't changed. Not altering it.",
                            agent.getClass().getName());
                }
            } else {
                logger.info("Starting {} with config:\n{}", agent.getClass().getName(),
                        MessageUtils.configToFormattedString(newAgentConfig));
                startAgent(agent, newAgentConfig);
            }
        }
    }

    private void startAgent(Agent agent, Config agentConfig) {
        Thread agentThread = new Thread(() -> {
            try {
                agent.initialize(agentConfig);
            } catch (Exception exception) {
                logger.error("An exception occurred wile starting {}.", agent.getClass().getName(), exception);
            }
        });
        agentThread.start();
        runningAgentsWithConfig.put(agent, agentConfig);
    }

    private void reconfigureAgent(Agent agent, Config newConfig) {
        try {
            agent.reconfigure(newConfig);
            logger.info("Successfully reconfigured {}.", agent.getClass().getName());
        } catch (Exception exception) {
            logger.error("An exception occurred wile reconfiguring {}.", agent.getClass().getName(), exception);
        }
        runningAgentsWithConfig.put(agent, newConfig);
    }

    private void stopAgent(Agent agent) {
        try {
            agent.close();
            logger.info("Successfully stopped {}.", agent.getClass().getName());
        } catch (Exception exception) {
            logger.error("An exception occurred while stopping {}.", agent.getClass().getName(), exception);
        }
        runningAgentsWithConfig.remove(agent);
    }

    private void scheduleConfigRefresher() {
        Runnable configRefresher = () -> {
            try {
                Config loadedApplicationConfig = configReader.read(configReaderArgs);
                if (! loadedApplicationConfig.equals(currentApplicationConfig)) {
                    logger.info("Configuration change detected.");
                    applyApplicationConfig(loadedApplicationConfig);
                }
            } catch (Exception exception) {
                logger.error("An exception occurred while refreshing configuration. Continuing to operate" +
                        " according to the current configuration.", exception);
            }
        };

        ScheduledExecutorService scheduledExecutor = Executors.newSingleThreadScheduledExecutor();
        scheduledExecutor.scheduleAtFixedRate(configRefresher,0,CONFIG_REFRESHING_RATE_IN_SECONDS, TimeUnit.SECONDS);
    }

    private void registerAgentCloserShutdownHook() {
        Thread agentCloserHook = new Thread(() -> {
            Set<Agent> runningAgents = new HashSet<>(runningAgentsWithConfig.keySet());
            runningAgents.forEach(this::stopAgent);
        });
        Runtime.getRuntime().addShutdownHook(agentCloserHook);
    }

}
