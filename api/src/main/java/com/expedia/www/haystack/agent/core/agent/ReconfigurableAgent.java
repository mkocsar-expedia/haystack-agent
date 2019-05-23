package com.expedia.www.haystack.agent.core.agent;

import com.typesafe.config.Config;

public interface ReconfigurableAgent extends Agent {

    void reconfigure(Config newConfig);

}
