package org.irmacard.idin.web;

import org.irmacard.api.common.util.GsonUtil;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.Collection;
import java.util.Set;

import redis.clients.jedis.UnifiedJedis;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

public class RedisTransactionStoreTest {

    private static final String TRX_1 = "TRX-1";
    private static final String EC = "ec";
    private static final long TTL = 3600L;
    private static final String KEY_1 = "idin:tx:" + TRX_1;
    private static final String INDEX = "idin:tx:index";

    private UnifiedJedis jedis;
    private RedisTransactionStore store;

    @BeforeEach
    public void setup() {
        jedis = mock(UnifiedJedis.class);
        store = new RedisTransactionStore(jedis, TTL);
    }

    @Test
    public void putStoresJsonWithTtlAndIndexes() {
        final IdinTransaction t = new IdinTransaction(TRX_1, EC);
        store.put(t);

        verify(jedis).setex(eq(KEY_1), eq(TTL), anyString());
        verify(jedis).sadd(INDEX, TRX_1);
    }

    @Test
    public void getDeserializesStoredTransaction() {
        final IdinTransaction t = new IdinTransaction(TRX_1, EC);
        when(jedis.get(KEY_1)).thenReturn(GsonUtil.getGson().toJson(t));

        final IdinTransaction result = store.get(TRX_1);
        assertNotNull(result);
        assertEquals(TRX_1, result.getTransactionId());
        assertEquals(EC, result.getEntranceCode());
    }

    @Test
    public void getMissingReturnsNullAndPrunesIndex() {
        when(jedis.get(KEY_1)).thenReturn(null);

        assertNull(store.get(TRX_1));
        verify(jedis).srem(INDEX, TRX_1);
    }

    @Test
    public void removeDeletesKeyAndIndexEntry() {
        store.remove(TRX_1);
        verify(jedis).del(KEY_1);
        verify(jedis).srem(INDEX, TRX_1);
    }

    @Test
    public void getAllSkipsAndPrunesExpiredEntries() {
        final IdinTransaction t = new IdinTransaction(TRX_1, EC);
        when(jedis.smembers(INDEX)).thenReturn(Set.of(TRX_1, "EXPIRED"));
        when(jedis.get(KEY_1)).thenReturn(GsonUtil.getGson().toJson(t));
        when(jedis.get("idin:tx:EXPIRED")).thenReturn(null);

        final Collection<IdinTransaction> all = store.getAll();
        assertEquals(1, all.size());
        assertEquals(TRX_1, all.iterator().next().getTransactionId());
        verify(jedis).srem(INDEX, "EXPIRED");
    }

    @Test
    public void sizeCountsLiveEntries() {
        final IdinTransaction t = new IdinTransaction(TRX_1, EC);
        when(jedis.smembers(INDEX)).thenReturn(Set.of(TRX_1));
        when(jedis.get(KEY_1)).thenReturn(GsonUtil.getGson().toJson(t));

        assertEquals(1, store.size());
    }

    @Test
    public void roundTripPreservesFinishedState() {
        final IdinTransaction t = new IdinTransaction(TRX_1, EC);
        t.finished();

        // capture what put serializes, then feed it back to get
        final String[] stored = new String[1];
        doAnswer(invocation -> {
            stored[0] = invocation.getArgument(2);
            return null;
        }).when(jedis).setex(eq(KEY_1), eq(TTL), anyString());

        store.put(t);
        when(jedis.get(KEY_1)).thenReturn(stored[0]);

        final IdinTransaction restored = store.get(TRX_1);
        assertTrue(restored.isFinished());
    }
}
