package org.irmacard.idin.web;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.Map;

/**
 * In-memory {@link TransactionStore}. This is the historic (and default) behaviour: transactions are
 * kept in a {@link HashMap} and are therefore lost when the server restarts. Used as a fallback when
 * Redis is not configured (or unreachable).
 */
public class InMemoryTransactionStore implements TransactionStore {

    private final Map<String, IdinTransaction> transactions = new HashMap<>();

    @Override
    public synchronized void put(final IdinTransaction transaction) {
        transactions.put(transaction.getTransactionId(), transaction);
    }

    @Override
    public synchronized IdinTransaction get(final String transactionId) {
        return transactions.get(transactionId);
    }

    @Override
    public synchronized Collection<IdinTransaction> getAll() {
        return new ArrayList<>(transactions.values());
    }

    @Override
    public synchronized void remove(final String transactionId) {
        transactions.remove(transactionId);
    }

    @Override
    public synchronized int size() {
        return transactions.size();
    }
}
