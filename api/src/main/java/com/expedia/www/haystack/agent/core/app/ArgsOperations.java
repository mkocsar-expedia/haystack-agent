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

public class ArgsOperations {

    private static final String CONFIG_READER_NAME_ARG = "--config-provider";
    private static final String CONFIG_REFRESHING_ARG = "--config-refreshing";

    // to prevent instantiation
    private ArgsOperations() {}

    public static LaunchParameters extractLaunchParameters(String[] args) {
        LaunchParameters launchParameters = LaunchParameters.defaultParameters();

        for (int index = 0; index < args.length; ++index) {
            if (args[index].equals(CONFIG_READER_NAME_ARG)) {
                launchParameters.setConfigReaderName(args[index + 1]);
                ++index;
            } else if (args[index].equals(CONFIG_REFRESHING_ARG)) {
                launchParameters.setConfigRefreshing(true);
            } else {
                launchParameters.addConfigReaderArg(args[index], args[index + 1]);
                ++index;
            }
        }

        return launchParameters;
    }

}
