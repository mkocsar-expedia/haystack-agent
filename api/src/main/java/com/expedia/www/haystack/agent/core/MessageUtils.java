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

import com.typesafe.config.Config;
import com.typesafe.config.ConfigRenderOptions;

public class MessageUtils {

    private static final ConfigRenderOptions renderingOptions;

    static {
        renderingOptions = ConfigRenderOptions.concise()
                .setFormatted(true)
                .setJson(false);
    }

    // silencing PDM UseUtilityClass violation
    private MessageUtils() {}

    public static String configToFormattedString(Config config) {
        return config.root().render(renderingOptions);
    }

}
