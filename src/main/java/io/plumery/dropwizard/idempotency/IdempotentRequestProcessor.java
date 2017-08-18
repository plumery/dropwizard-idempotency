package io.plumery.dropwizard.idempotency;

import javax.ws.rs.core.Response;

public interface IdempotentRequestProcessor {
    Response process();
}
