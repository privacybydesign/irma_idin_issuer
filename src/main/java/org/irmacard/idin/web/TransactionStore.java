package org.irmacard.idin.web;

import java.util.Collection;

/**
 * Storage backend for open/pending iDIN transactions.
 *
 * <p>Two implementations exist: {@link InMemoryTransactionStore} (the default, which loses its
 * contents on restart) and {@link RedisTransactionStore} (persistent, and shareable between
 * multiple server instances). The backend in use is selected by {@link OpenTransactions} based on
 * the {@link IdinConfiguration}.</p>
 *
 * <p>Implementations must be safe to use concurrently.</p>
 */
public interface TransactionStore {

    /**
     * Stores a transaction, overwriting any existing transaction with the same id. This is used both
     * to add a new transaction and to persist mutations to an existing one (e.g. after
     * {@link IdinTransaction#handled()} or {@link IdinTransaction#finished()}).
     */
    void put(IdinTransaction transaction);

    /**
     * @return the transaction with the given id, or {@code null} if it is not (or no longer) stored.
     */
    IdinTransaction get(String transactionId);

    /**
     * @return a snapshot of all currently stored transactions. Mutating the returned collection does
     * not affect the store.
     */
    Collection<IdinTransaction> getAll();

    /**
     * Removes the transaction with the given id. A no-op if it is not stored.
     */
    void remove(String transactionId);

    /**
     * @return the number of currently stored transactions.
     */
    int size();
}
