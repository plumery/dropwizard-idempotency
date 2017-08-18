package io.plumery.dropwizard.idempotency.dropwizard;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.google.common.base.Strings;
import io.dropwizard.Configuration;
import io.dropwizard.validation.ValidationMethod;
import org.hibernate.validator.constraints.NotEmpty;
import javax.validation.constraints.NotNull;


public class IdempotencyConfiguration extends Configuration{
    protected static final String DYNAMODB = "dynamodb";
    protected static final String HAZELCAST = "hazelcast";
    protected static final String DEFAULT_IMPLEMENTATION = HAZELCAST;

    @JsonProperty
    @NotEmpty
    private String storageImplementation = DEFAULT_IMPLEMENTATION;

    @JsonProperty
    @NotNull
    private String dynamodbEndpointURL;

    @JsonProperty
    @NotNull
    private String cacheName;

    public String getStorageImplementation() {
        return storageImplementation;
    }

    public void setStorageImplementation(String storageImplementation) {
        this.storageImplementation = storageImplementation;
    }

    public String getDynamodbEndpointURL() {
        return dynamodbEndpointURL;
    }

    public void setDynamodbEndpointURL(String dynamodbEndpointURL) {
        this.dynamodbEndpointURL = dynamodbEndpointURL;
    }

    public String getCacheName() {
        return cacheName;
    }

    public void setCacheName(String cacheName) {
        this.cacheName = cacheName;
    }

    @ValidationMethod
    @JsonIgnore
    public boolean isValidConfig() {
        return storageImplementation.equals(DYNAMODB) && Strings.emptyToNull(dynamodbEndpointURL) != null;
    }
}
