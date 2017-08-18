package io.plumery.dropwizard.idempotency.hazelcast;

import io.plumery.dropwizard.idempotency.Cache;
import io.plumery.dropwizard.idempotency.IdempotentProviderService;
import com.hazelcast.core.HazelcastInstance;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class HazelcastCache implements Cache {
    private static final Logger log = LoggerFactory.getLogger(IdempotentProviderService.class);
    private final HazelcastInstance client;
    private final String mapName;

    public HazelcastCache(HazelcastInstance client, String mapName) {
        this.client = client;
        this.mapName = mapName;
    }

    public void put(String requestId, String responseJson) {
        client.getMap(mapName).put(requestId, responseJson);
    }
    public String get(String s) {
        return (String) client.getMap(mapName).get(s);
    }
    public void remove(String s) {
        client.getMap(mapName).remove(s);
    }
}
