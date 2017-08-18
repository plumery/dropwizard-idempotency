package io.plumery.dropwizard.idempotency;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.ws.rs.core.Response;
import java.io.IOException;

import static org.apache.commons.lang3.StringUtils.isEmpty;

public class IdempotentProviderService {
    private static final Logger log = LoggerFactory.getLogger(IdempotentProviderService.class);
    private final Cache cache;

    public IdempotentProviderService(Cache cache) {
        this.cache = cache;
    }

    public Response idempotent(String idempotencyKey, IdempotentRequestProcessor request) {
        if (isEmpty(idempotencyKey)) {
            return request.process();
        }
        Response response = getResponse(idempotencyKey);
        if (response == null) {
            response = request.process();
            putResponse(idempotencyKey, response);
        }
        return response;
    }

    private void putResponse(String idempotencyKey, Response response) {
        // -- successful response (2xx)
        if (firstNum(response.getStatus()) != 2) {
            return;
        }
        ObjectMapper mapper = new ObjectMapper();
        String responseJson;
        try {
            responseJson = mapper.writeValueAsString(new CachedResponse(response.getEntity(), response.getStatus()));
        } catch (JsonProcessingException e) {
            log.warn("Cannot serialize Response to put into cache: ", e.getMessage());
            return;
        }
        cache.put(idempotencyKey, responseJson);
    }

    private Response getResponse(String idempotencyKey) {
        ObjectMapper mapper = new ObjectMapper();
        String obj = cache.get(idempotencyKey);
        if (obj == null) {
            return null;
        }
        try {
            CachedResponse cachedResponse = mapper.readValue(obj, CachedResponse.class);
            return Response.status(cachedResponse.getStatus()).entity(cachedResponse.getEntity()).build();
        } catch (IOException e) {
            log.warn("Cannot deserialize Response from cache: ", e.getMessage());
        }
        return null;
    }

    private int firstNum(int x) {
        return Integer.parseInt(Integer.toString(x).substring(0, 1));
    }
}
