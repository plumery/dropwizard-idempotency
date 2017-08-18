package io.plumery.dropwizard.idempotency.dropwizard;

import com.amazonaws.services.dynamodbv2.AmazonDynamoDB;
import com.amazonaws.services.dynamodbv2.AmazonDynamoDBClient;
import io.plumery.dropwizard.idempotency.Cache;
import io.plumery.dropwizard.idempotency.IdempotentProviderService;
import io.plumery.dropwizard.idempotency.dynamodb.DDBCache;
import io.plumery.dropwizard.idempotency.hazelcast.HazelcastCache;
import com.fasterxml.jackson.annotation.JsonIgnore;
import com.hazelcast.config.Config;
import com.hazelcast.core.Hazelcast;
import com.hazelcast.core.HazelcastInstance;

public class IdempotencyFactory {
    private IdempotencyFactory() { }

    @JsonIgnore
    public static IdempotentProviderService build(IdempotencyConfiguration configuration) {
        Cache responsesCache = null;

        if (configuration.getStorageImplementation().equals(IdempotencyConfiguration.DYNAMODB)) {
            responsesCache = buildDynamoDBCache(configuration);
        } else if (configuration.getStorageImplementation().equals(IdempotencyConfiguration.HAZELCAST)) {
            responsesCache = buildHazelcastCache(configuration);
        } else {
            throw new IllegalArgumentException("Unsupported [storageImplementation] specified '" + configuration.getStorageImplementation() + "'");
        }

        return new IdempotentProviderService(responsesCache);
    }

    private static HazelcastCache buildHazelcastCache(IdempotencyConfiguration configuration) {
        Config cfg = new Config();
        HazelcastInstance instance = Hazelcast.newHazelcastInstance(cfg);
        return new HazelcastCache(instance, configuration.getCacheName());
    }

    private static Cache buildDynamoDBCache(IdempotencyConfiguration configuration) {
        AmazonDynamoDB amazonDynamoDB = new AmazonDynamoDBClient();
        amazonDynamoDB.setEndpoint(configuration.getDynamodbEndpointURL());
        return new DDBCache(amazonDynamoDB, configuration.getCacheName());
    }
}
