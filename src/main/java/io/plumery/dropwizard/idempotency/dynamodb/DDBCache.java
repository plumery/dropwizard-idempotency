package io.plumery.dropwizard.idempotency.dynamodb;

import com.amazonaws.services.dynamodbv2.AmazonDynamoDB;
import com.amazonaws.services.dynamodbv2.document.AttributeUpdate;
import com.amazonaws.services.dynamodbv2.document.DynamoDB;
import com.amazonaws.services.dynamodbv2.document.Item;
import com.amazonaws.services.dynamodbv2.document.Table;
import com.amazonaws.services.dynamodbv2.model.*;
import com.amazonaws.services.dynamodbv2.util.TableUtils;
import io.plumery.dropwizard.idempotency.Cache;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Simple cache implementation where data is stored in Dynamo DB.
 * Class can be configured to treat data as expired based on creation time or last access time properties.
 * Cached data is never removed from Dynamo DB. It is just filtered out if it's considered to be too old.
 *
 * <p>Time-to-live expires data based on its creation time. It let's item to live for a certain amount of milliseconds
 * since its creation. Every {@link #put(String, String)} refreshes item's creation time.</p>
 *
 * <p>Time-to-idle expires data based on its last access time. It let's item to live for a certain amount of milliseconds
 * since its last access time. Item's access time is updated any time {@link #put(String, String)} or
 * {@link #get(String)} is called.</p>
 *
 * <p>If both time-to-live and time-to-idle are used, time-to-idle should be lower than time-to-live. Otherwise,
 * time-to-live will naturally take precedence.</p>
 */
public class DDBCache implements Cache {

    private static final Logger LOG = LoggerFactory.getLogger(DDBCache.class);

    private static final String KEY_ATTRIBUTE = "key";
    private static final String VALUE_ATTRIBUTE = "value";
    private static final String CREATION_TIME = "creationTime";
    private static final String EXPIRY_TIME = "expiryTime";

    private final Table table;
    private final String cacheName;
    private final long timeToLive;
    private final long timeToIdle;

    public DDBCache(AmazonDynamoDB dynamoDB, String cacheName, long timeToLive, long timeToIdle) {
        ensureTableExists(dynamoDB, cacheName);

        this.table = new DynamoDB(dynamoDB).getTable(cacheName);
        this.cacheName = cacheName;
        this.timeToLive = timeToLive;
        this.timeToIdle = timeToIdle;
    }

    public DDBCache(AmazonDynamoDB dynamoDB, String cacheName) {
        this(dynamoDB, cacheName, 0, 0);
    }

    public void put(String key, String value) {
        table.putItem(createItem(key, value));
    }

    public String get(String key) {
        Item item = table.getItem(KEY_ATTRIBUTE, key);
        if (item == null || isItemExpired(item)) {
            return null;
        }

        extendExpiryTime(key);

        return item.getString(VALUE_ATTRIBUTE);
    }

    public void remove(String key) {
        table.deleteItem(KEY_ATTRIBUTE, key);
    }

    private Item createItem(String key, String value) {
        long currentTimeMillis = System.currentTimeMillis();
        Item item = new Item()
                .withPrimaryKey(KEY_ATTRIBUTE, key)
                .withString(VALUE_ATTRIBUTE, value)
                .withLong(CREATION_TIME, currentTimeMillis);

        if (timeToIdle > 0 || timeToLive > 0) {
            item = item.withLong(EXPIRY_TIME, expiryTime(currentTimeMillis));
        }

        return item;
    }

    private long expiryTime(long currentTimeMillis) {
        long expiryTime;
        if (timeToIdle > 0 && timeToIdle < timeToLive) {
            expiryTime = currentTimeMillis + timeToIdle;
        } else {
            expiryTime = currentTimeMillis + timeToLive;
        }
        return expiryTime;
    }

    private void extendExpiryTime(String key) {
        if (timeToIdle <= 0) {
            return;
        }

        try {
            long expiryTime = System.currentTimeMillis() + timeToIdle;
            table.updateItem(KEY_ATTRIBUTE, key, new AttributeUpdate(EXPIRY_TIME).put(expiryTime));
        } catch (Exception e) {
            LOG.info("Failed to update last access time for item with key {} in table {}", key, cacheName);
        }
    }

    private boolean isItemExpired(Item item) {
        return item.hasAttribute(EXPIRY_TIME) && System.currentTimeMillis() > item.getLong(EXPIRY_TIME);
    }

    private void ensureTableExists(AmazonDynamoDB dynamoDB, String cacheName) {
        TableUtils.createTableIfNotExists(dynamoDB,
                new CreateTableRequest()
                        .withTableName(cacheName)
                        .withKeySchema(new KeySchemaElement().withKeyType(KeyType.HASH).withAttributeName(KEY_ATTRIBUTE))
                        .withAttributeDefinitions(new AttributeDefinition(KEY_ATTRIBUTE, ScalarAttributeType.S))
                        .withProvisionedThroughput(
                                new ProvisionedThroughput()
                                        .withReadCapacityUnits(10L)
                                        .withWriteCapacityUnits(5L)));
    }
}