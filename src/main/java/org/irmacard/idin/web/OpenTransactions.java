package org.irmacard.idin.web;

import net.bankid.merchant.library.Communicator;
import net.bankid.merchant.library.StatusRequest;
import net.bankid.merchant.library.StatusResponse;
import org.slf4j.LoggerFactory;

import java.util.Collection;

public class OpenTransactions {

    private static final org.slf4j.Logger LOGGER = LoggerFactory.getLogger(OpenTransactions.class);

    private static TransactionStore store;

    public OpenTransactions() {
    }

    /**
     * Returns the configured transaction store, creating it on first use. A Redis-backed store is
     * used when Redis is configured (and reachable); otherwise the historic in-memory store is used.
     */
    static synchronized TransactionStore getStore() {
        if (store == null) {
            store = createStore();
        }
        return store;
    }

    /**
     * Replaces the backing store. Intended for tests and for re-initialisation; production code
     * should rely on {@link #getStore()}.
     */
    static synchronized void setStore(final TransactionStore newStore) {
        store = newStore;
    }

    private static TransactionStore createStore() {
        final IdinConfiguration config = IdinConfiguration.getInstance();
        if (config.isRedisEnabled()) {
            try {
                final TransactionStore redisStore =
                        new RedisTransactionStore(RedisClient.create(config), config.getRedisTransactionTtlSeconds());
                LOGGER.info("Using Redis-backed transaction store");
                return redisStore;
            } catch (final Exception e) {
                LOGGER.error("Could not connect to Redis, falling back to in-memory transaction store", e);
            }
        }
        LOGGER.info("Using in-memory transaction store");
        return new InMemoryTransactionStore();
    }

    public static String getOpenTransactions() {
        final StringBuilder open = new StringBuilder();
        for (final IdinTransaction it : getStore().getAll()) {
            open.append(it.getTransactionId()).append(", ");
        }
        return open.toString();
    }

    public static void addTransaction(final IdinTransaction it) {
        getStore().put(it);
    }

    /**
     * Persists mutations made to an already-stored transaction (e.g. after calling
     * {@link IdinTransaction#handled()} or {@link IdinTransaction#finished()}). Required for the
     * Redis store, which holds serialized copies rather than live references; harmless for the
     * in-memory store.
     */
    public static void update(final IdinTransaction it) {
        getStore().put(it);
    }

    public static int getHowMany() {
        return getStore().size();
    }

    public static synchronized void requestStates() {
        final TransactionStore store = getStore();
        final Collection<IdinTransaction> transactions = store.getAll();
        final int startSize = transactions.size();
        LOGGER.info("Starting status requests for open and pending statuses, initially: {}", startSize);

        final StringBuilder closed = new StringBuilder();
        final Communicator communicator = new Communicator();

        for (final IdinTransaction idinTransaction : transactions) {
            if (idinTransaction.isFinished()) {
                store.remove(idinTransaction.getTransactionId());
                continue;
            }
            if (!idinTransaction.isOneDayOld()) {
                continue;
            }

            try {
                final StatusRequest statusRequest = new StatusRequest(idinTransaction.getTransactionId());
                final StatusResponse response = communicator.getResponse(statusRequest);
                idinTransaction.handled();

                switch (response.getStatus()) {
                    case StatusResponse.Success:
                    case StatusResponse.Cancelled:
                    case StatusResponse.Expired:
                    case StatusResponse.Failure:
                        closed.append(idinTransaction.getTransactionId()).append(", ");
                        store.remove(idinTransaction.getTransactionId());
                        break;
                    case StatusResponse.Open:
                    case StatusResponse.Pending:
                    default:
                        // Persist the updated lastHandled timestamp.
                        store.put(idinTransaction);
                        break;
                }
            } catch (final Exception exception) {
                LOGGER.warn("Status check failed for {}: {}", idinTransaction.getTransactionId(), exception.toString());
            }
        }

        LOGGER.info("Finished with status requests, ended with: {}", store.size());
        LOGGER.info("Transactions now closed: {}", closed);
        LOGGER.info("Transactions still open: {}", getOpenTransactions());
    }

    public static IdinTransaction findTransaction(final String transactionId) {
        return getStore().get(transactionId);
    }

    //Prevent cloning of this class
    public Object clone() throws CloneNotSupportedException {
        throw new CloneNotSupportedException();
    }
}
