package org.irmacard.idin.web;

import java.util.Set;

import redis.clients.jedis.DefaultJedisClientConfig;
import redis.clients.jedis.HostAndPort;
import redis.clients.jedis.JedisClientConfig;
import redis.clients.jedis.JedisPooled;
import redis.clients.jedis.JedisSentineled;
import redis.clients.jedis.UnifiedJedis;

/**
 * Builds a {@link UnifiedJedis} client from the {@link IdinConfiguration}. Supports both a plain
 * Redis server (host/port/password) and a Redis Sentinel setup, mirroring the configuration shape
 * used by the other privacybydesign issuers (e.g. yivi-iban-issuer).
 */
public final class RedisClient {

    private RedisClient() {
    }

    /**
     * Creates and connectivity-checks a Redis client based on the given configuration. The caller is
     * responsible for falling back to another store if this throws.
     *
     * @throws RuntimeException if a connection to Redis could not be established.
     */
    public static UnifiedJedis create(final IdinConfiguration config) {
        final UnifiedJedis jedis;

        if (config.isRedisSentinelEnabled()) {
            final Set<HostAndPort> sentinels =
                    Set.of(new HostAndPort(config.getRedisSentinelHost(), config.getRedisSentinelPort()));
            final JedisClientConfig masterConfig = DefaultJedisClientConfig.builder()
                    .password(emptyToNull(config.getRedisPassword()))
                    .build();
            final JedisClientConfig sentinelConfig = DefaultJedisClientConfig.builder()
                    .user(emptyToNull(config.getRedisSentinelUsername()))
                    .password(emptyToNull(config.getRedisPassword()))
                    .build();
            jedis = new JedisSentineled(config.getRedisMasterName(), masterConfig, sentinels, sentinelConfig);
        } else {
            final HostAndPort address = new HostAndPort(config.getRedisHost(), config.getRedisPort());
            final JedisClientConfig clientConfig = DefaultJedisClientConfig.builder()
                    .password(emptyToNull(config.getRedisPassword()))
                    .build();
            jedis = new JedisPooled(address, clientConfig);
        }

        // Fail fast if Redis is unreachable so the caller can fall back to the in-memory store.
        jedis.ping();
        return jedis;
    }

    private static String emptyToNull(final String value) {
        return (value == null || value.isBlank()) ? null : value;
    }
}
