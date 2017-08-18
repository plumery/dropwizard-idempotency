package io.plumery.dropwizard.idempotency.dropwizard;

import io.plumery.dropwizard.idempotency.IdempotentRequestProcessor;
import io.plumery.dropwizard.idempotency.IdempotentProviderService;
import org.junit.Before;
import org.junit.Test;

import javax.ws.rs.core.Response;
import java.util.UUID;

import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.core.IsEqual.equalTo;
import static org.junit.Assert.*;

public class IdempotencyFactoryTest {
    private IdempotentProviderService providerService;

    @Before
    public void setUp() {
        IdempotencyConfiguration configuration = new IdempotencyConfiguration();
        configuration.setStorageImplementation(IdempotencyConfiguration.DEFAULT_IMPLEMENTATION);
        configuration.setCacheName("testing");

        providerService = IdempotencyFactory.build(configuration);
    }

    @Test
    public void shouldCacheInitialResponse() throws Exception {
        String idempotencyKey = UUID.randomUUID().toString();

        Response response = doIdempotentCall(idempotencyKey, Response.Status.ACCEPTED);
        assertThat(response.getStatus(), is(equalTo(202)));

        // -- assert that the status returned by the provider should be the same as above given the same idempotencyKey
        response = doIdempotentCall(idempotencyKey, Response.Status.OK);
        assertThat(response.getStatus(), is(equalTo(202)));
    }

    private Response doIdempotentCall(String idempotencyKey, final Response.Status status) {
        return providerService.idempotent(idempotencyKey, new IdempotentRequestProcessor() {
            public Response process() {
                return Response.status(status).build();
            }
        });
    }
}