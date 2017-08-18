# dropwizard-idempotency
The Idempotency module for dropwizard based on DynamoDB‎ and Hazelcast

Example usage with Hazelcast:
```
IdempotencyConfiguration configuration = new IdempotencyConfiguration();
configuration.setStorageImplementation(IdempotencyConfiguration.DEFAULT_IMPLEMENTATION);
configuration.setCacheName("testing");

IdempotentProviderService providerService = IdempotencyFactory.build(configuration);
        
Response response = providerService.idempotent(idempotencyKey, new IdempotentRequestProcessor() {
    public Response process() {
        return Response.status(Response.Status.ACCEPTED).build();
    }
});
```
 