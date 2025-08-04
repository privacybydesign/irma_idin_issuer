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

    public OpenTransactions() {}

    public static String getOpenTransactions(){
        final StringBuilder open = new StringBuilder();
        for (final IdinTransaction it: OPEN_OR_PENDING_TRANSACTIONS){
            open.append(it.getTransactionId()).append(", ");
        }
        return open.toString();
    }

    public static synchronized void addTransaction (final IdinTransaction it){
        OPEN_OR_PENDING_TRANSACTIONS.add(it);
    }

    public static synchronized int getHowMany(){
        return OPEN_OR_PENDING_TRANSACTIONS.size();
    }

    public static synchronized void requestStates(){
        final StringBuilder closed = new StringBuilder();
        final int startSize = OPEN_OR_PENDING_TRANSACTIONS.size();
        LOGGER.info("Starting status requests for open and pending statusses, initially: {}", startSize);
        for (final IdinTransaction it: OPEN_OR_PENDING_TRANSACTIONS){
            if (it.isFinished()) {
                OPEN_OR_PENDING_TRANSACTIONS.remove(it);
                continue;
            } else if (!it.isOneDayOld()) {
                // According to the IDIN specs we are only allowed to check the transaction status once a day
                continue;
            }
            final StatusRequest sr = new StatusRequest(it.getTransactionId());
            final StatusResponse response = new Communicator().getResponse(sr);
            it.handled();
            switch (response.getStatus()) {
                case StatusResponse.Success:
                case StatusResponse.Cancelled:
                case StatusResponse.Expired:
                case StatusResponse.Failure:
                    closed.append(it.getTransactionId()).append(", ");
                    OPEN_OR_PENDING_TRANSACTIONS.remove(it);
                    break;
                case StatusResponse.Open:
                case StatusResponse.Pending:
                default:
                    break;
            }
        }
        LOGGER.info("Finished with status Requests, ended with: {}", OPEN_OR_PENDING_TRANSACTIONS.size());
        LOGGER.info("Transactions now closed: {}", closed);
        LOGGER.info("Transactions still open: {}",getOpenTransactions());
    }

    public static synchronized IdinTransaction findTransaction(final String trxid) {
        for (final IdinTransaction it : OPEN_OR_PENDING_TRANSACTIONS) {
            if (it.getTransactionId().equals(trxid)) {
                return it;
            }
        }
        return null;
    }

    //Prevent cloning of this class
    public Object clone() throws CloneNotSupportedException {
        throw new CloneNotSupportedException();
    }
}
