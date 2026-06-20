package org.irmacard.idin.web;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.Collection;

import static org.junit.jupiter.api.Assertions.*;

public class InMemoryTransactionStoreTest {

    private static final String TRX_1 = "TRX-1";
    private static final String TRX_2 = "TRX-2";
    private static final String EC = "ec";

    private InMemoryTransactionStore store;

    @BeforeEach
    public void setup() {
        store = new InMemoryTransactionStore();
    }

    @Test
    public void putAndGet() {
        final IdinTransaction t = new IdinTransaction(TRX_1, EC);
        store.put(t);
        assertSame(t, store.get(TRX_1));
        assertEquals(1, store.size());
    }

    @Test
    public void getMissingReturnsNull() {
        assertNull(store.get("does-not-exist"));
    }

    @Test
    public void putOverwritesSameId() {
        store.put(new IdinTransaction(TRX_1, EC));
        store.put(new IdinTransaction(TRX_1, "other-ec"));
        assertEquals(1, store.size());
        assertEquals("other-ec", store.get(TRX_1).getEntranceCode());
    }

    @Test
    public void remove() {
        store.put(new IdinTransaction(TRX_1, EC));
        store.remove(TRX_1);
        assertNull(store.get(TRX_1));
        assertEquals(0, store.size());
        // removing again is a no-op
        store.remove(TRX_1);
        assertEquals(0, store.size());
    }

    @Test
    public void getAllReturnsSnapshot() {
        store.put(new IdinTransaction(TRX_1, EC));
        store.put(new IdinTransaction(TRX_2, EC));

        final Collection<IdinTransaction> all = store.getAll();
        assertEquals(2, all.size());

        // mutating the returned collection does not affect the store
        all.clear();
        assertEquals(2, store.size());
    }
}
