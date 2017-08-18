package io.plumery.dropwizard.idempotency;

public interface Cache {

    void put(String key, String value);
    String get(String key);
    void remove(String key);
}
