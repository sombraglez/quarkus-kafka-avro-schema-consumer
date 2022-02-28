package org.acme.kafka.config;

import io.quarkus.test.common.QuarkusTestResourceLifecycleManager;
import io.smallrye.reactive.messaging.providers.connectors.InMemoryConnector;

import java.util.HashMap;
import java.util.Map;

public class KafkaTestResourceLifecycleManager implements QuarkusTestResourceLifecycleManager {

    @Override
    public Map<String, String> start() {
        return new HashMap<>(InMemoryConnector.switchIncomingChannelsToInMemory("movies-from-kafka"));
    }

    @Override
    public void stop() {
        InMemoryConnector.clear();
    }
}

