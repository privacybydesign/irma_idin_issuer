package org.irmacard.idin.web;

import net.bankid.merchant.library.Communicator;
import net.bankid.merchant.library.StatusRequest;
import net.bankid.merchant.library.StatusResponse;
import org.slf4j.LoggerFactory;

import java.util.HashSet;
import java.util.Set;

public class OpenTransactions {

    private static final org.slf4j.Logger LOGGER = LoggerFactory.getLogger(OpenTransactions.class);
    private static final Set<IdinTransaction> OPEN_OR_PENDING_TRANSACTIONS = new HashSet<>();

    public OpenTransactions() {
    }

    public static String getOpenTransactions() {
        final StringBuilder open = new StringBuilder();
        for (final IdinTransaction it : OPEN_OR_PENDING_TRANSACTIONS) {
            open.append(it.getTransactionId()).append(", ");
        }
        return open.toString();
    }

    public static synchronized void addTransaction(final IdinTransaction it) {
        OPEN_OR_PENDING_TRANSACTIONS.add(it);
    }

    public static synchronized int getHowMany() {
        return OPEN_OR_PENDING_TRANSACTIONS.size();
    }

    public static synchronized void requestStates() {
        final int startSize = OPEN_OR_PENDING_TRANSACTIONS.size();
        LOGGER.info("Starting status requests for open and pending statuses, initially: {}", startSize);

        final StringBuilder closed = new StringBuilder();
        Communicator communicator = null;

        for (final java.util.Iterator<IdinTransaction> it = OPEN_OR_PENDING_TRANSACTIONS.iterator(); it.hasNext(); ) {
            final IdinTransaction idinTransaction = it.next();

            if (idinTransaction.isFinished()) {
                it.remove();
                continue;
            }
            if (!idinTransaction.isOneDayOld()) {
                continue;
            }

            try {
                if (communicator == null) {
                    communicator = new Communicator();
                }
                final StatusRequest statusRequest = new StatusRequest(idinTransaction.getTransactionId());
                final StatusResponse response = communicator.getResponse(statusRequest);
                idinTransaction.handled();

                switch (response.getStatus()) {
                    case StatusResponse.Success:
                    case StatusResponse.Cancelled:
                    case StatusResponse.Expired:
                    case StatusResponse.Failure:
                        closed.append(idinTransaction.getTransactionId()).append(", ");
                        it.remove();
                        break;
                    case StatusResponse.Open:
                    case StatusResponse.Pending:
                    default:
                        break;
                }
            } catch (final Exception exception) {
                LOGGER.warn("Status check failed for {}: {}", idinTransaction.getTransactionId(), exception.toString());
            }
        }

        LOGGER.info("Finished with status requests, ended with: {}", OPEN_OR_PENDING_TRANSACTIONS.size());
        LOGGER.info("Transactions now closed: {}", closed);
        LOGGER.info("Transactions still open: {}", getOpenTransactions());
    }

    public static synchronized IdinTransaction findTransaction(final String transactionId) {
        for (final IdinTransaction idinTransaction : OPEN_OR_PENDING_TRANSACTIONS) {
            if (idinTransaction.getTransactionId().equals(transactionId)) {
                return idinTransaction;
            }
        }
        return null;
    }

    //Prevent cloning of this class
    public Object clone() throws CloneNotSupportedException {
        throw new CloneNotSupportedException();
    }
}
