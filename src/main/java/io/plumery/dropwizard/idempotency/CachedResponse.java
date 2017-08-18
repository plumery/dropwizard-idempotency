package io.plumery.dropwizard.idempotency;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;

public class CachedResponse {
    private Object entity;
    private Integer status;

    @JsonCreator
    public CachedResponse(@JsonProperty("entity") Object entity, @JsonProperty("status") Integer status) {
        this.entity = entity;
        this.status = status;
    }

    public Object getEntity() {
        return entity;
    }
    public Integer getStatus() {
        return status;
    }
}
