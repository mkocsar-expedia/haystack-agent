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

import java.util.HashMap;
import java.util.Map;

class LaunchParameters {

    private static final String DEFAULT_CONFIG_READER_NAME = "file";
    private static final Map<String, String> DEFAULT_CONFIG_READER_ARGS = new HashMap<>();
    private static final Boolean DEFAULT_CONFIG_REFRESHING = false;

    private String configReaderName;
    private Map<String, String> configReaderArgs;
    private Boolean configRefreshing;

    public LaunchParameters(String configReaderName, Map<String, String> configReaderArgs, Boolean configRefreshing) {
        this.configReaderName = configReaderName;
        this.configReaderArgs = new HashMap<>(configReaderArgs);
        this.configRefreshing = configRefreshing;
    }

    public static LaunchParameters defaultParameters() {
        return new LaunchParameters(DEFAULT_CONFIG_READER_NAME, DEFAULT_CONFIG_READER_ARGS, DEFAULT_CONFIG_REFRESHING);
    }

    public void addConfigReaderArg(String key, String value) {
        configReaderArgs.put(key, value);
    }

    public String getConfigReaderName() {
        return configReaderName;
    }

    public void setConfigReaderName(String configReaderName) {
        this.configReaderName = configReaderName;
    }

    public Map<String, String> getConfigReaderArgs() {
        return new HashMap<>(configReaderArgs);
    }

    public void setConfigReaderArgs(Map<String, String> configReaderArgs) {
        this.configReaderArgs = new HashMap<>(configReaderArgs);
    }

    public Boolean getConfigRefreshing() {
        return configRefreshing;
    }

    public void setConfigRefreshing(Boolean configRefreshing) {
        this.configRefreshing = configRefreshing;
    }

}
