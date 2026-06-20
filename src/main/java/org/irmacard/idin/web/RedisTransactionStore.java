package org.irmacard.idin.web;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Set;

import org.irmacard.api.common.util.GsonUtil;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import redis.clients.jedis.UnifiedJedis;

/**
 * Redis-backed {@link TransactionStore}. Transactions survive a server restart and can be shared
 * between multiple server instances, which is the motivation for this class (see issue #11).
 *
 * <p>Each transaction is stored as JSON under {@code idin:tx:&lt;transactionId&gt;} with a TTL acting
 * as a safety net so abandoned transactions cannot accumulate forever. A separate Redis set
 * ({@code idin:tx:index}) tracks the known transaction ids so they can be enumerated without a
 * (blocking) {@code KEYS} scan; entries whose value key has expired are pruned from the index lazily
 * on read.</p>
 */
public class RedisTransactionStore implements TransactionStore {

    private static final Logger LOGGER = LoggerFactory.getLogger(RedisTransactionStore.class);

    private static final String KEY_PREFIX = "idin:tx:";
    private static final String INDEX_KEY = "idin:tx:index";

    private final UnifiedJedis jedis;
    private final long ttlSeconds;

    public RedisTransactionStore(final UnifiedJedis jedis, final long ttlSeconds) {
        this.jedis = jedis;
        this.ttlSeconds = ttlSeconds;
    }

    private static String key(final String transactionId) {
        return KEY_PREFIX + transactionId;
    }

    @Override
    public void put(final IdinTransaction transaction) {
        final String json = GsonUtil.getGson().toJson(transaction);
        jedis.setex(key(transaction.getTransactionId()), ttlSeconds, json);
        jedis.sadd(INDEX_KEY, transaction.getTransactionId());
    }

    @Override
    public IdinTransaction get(final String transactionId) {
        final String json = jedis.get(key(transactionId));
        if (json == null) {
            // value expired or was removed; keep the index tidy
            jedis.srem(INDEX_KEY, transactionId);
            return null;
        }
        return GsonUtil.getGson().fromJson(json, IdinTransaction.class);
    }

    @Override
    public Collection<IdinTransaction> getAll() {
        final Set<String> ids = jedis.smembers(INDEX_KEY);
        final Collection<IdinTransaction> result = new ArrayList<>(ids.size());
        for (final String id : ids) {
            final String json = jedis.get(key(id));
            if (json == null) {
                jedis.srem(INDEX_KEY, id);
                continue;
            }
            try {
                result.add(GsonUtil.getGson().fromJson(json, IdinTransaction.class));
            } catch (final Exception e) {
                LOGGER.warn("Could not deserialize stored transaction {}: {}", id, e.toString());
            }
        }
        return result;
    }

    @Override
    public void remove(final String transactionId) {
        jedis.del(key(transactionId));
        jedis.srem(INDEX_KEY, transactionId);
    }

    @Override
    public int size() {
        return getAll().size();
    }
}
