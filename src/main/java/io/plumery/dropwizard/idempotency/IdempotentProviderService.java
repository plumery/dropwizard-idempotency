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
            // -- no idempotency is requested
            return request.process();
        }

        Response response = retrieve(idempotencyKey);
        if (response == null) {
            response = request.process();

            if (firstNum(response.getStatus()) == 2) {
                // -- successful response (2xx)
                store(idempotencyKey, response);
            }
        }
        return response;
    }

    private void store(String idempotencyKey, Response response) {
        ObjectMapper mapper = new ObjectMapper();
        String responseJson;
        try {
            responseJson = mapper.writeValueAsString(new CachedResponse(response.getEntity(), response.getStatus()));
            cache.put(idempotencyKey, responseJson);
        } catch (JsonProcessingException e) {
            log.warn("Cannot serialize response: ", e);
        }
    }

    private Response retrieve(String idempotencyKey) {
        Response answer = null;
        String obj = cache.get(idempotencyKey);

        if (obj != null) {
            ObjectMapper mapper = new ObjectMapper();
            try {
                CachedResponse cachedResponse = mapper.readValue(obj, CachedResponse.class);
                answer = Response
                        .status(cachedResponse.getStatus())
                        .entity(cachedResponse.getEntity())
                        .build();
            } catch (IOException e) {
                log.warn("Cannot deserialize response: ", e);
            }
        }

        return answer;
    }

    private int firstNum(int x) {
        return Integer.parseInt(Integer.toString(x).substring(0, 1));
    }
}
