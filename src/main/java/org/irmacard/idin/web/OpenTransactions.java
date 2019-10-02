package org.irmacard.idin.web;

import net.bankid.merchant.library.Communicator;
import net.bankid.merchant.library.StatusRequest;
import net.bankid.merchant.library.StatusResponse;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

public class OpenTransactions {

    private static org.slf4j.Logger logger = LoggerFactory.getLogger(OpenTransactions.class);
    private static Set<IdinTransaction> openOrPendingTransactions = new HashSet<>();

    public OpenTransactions() {}

    public static String getOpenTransactions(){
        StringBuilder open = new StringBuilder();
        for (IdinTransaction it: openOrPendingTransactions){
            open.append(it.getTransactionId()).append(", ");
        }
        return open.toString();
    }

    public static synchronized void addTransaction (IdinTransaction it){
        openOrPendingTransactions.add(it);
    }

    public static synchronized int getHowMany(){
        return openOrPendingTransactions.size();
    }

    public static synchronized void requestStates(){
        StringBuilder closed = new StringBuilder();
        int startSize = openOrPendingTransactions.size();
        logger.info("Starting status requests for open and pending statusses, initially: {}", startSize);
        for (IdinTransaction it: openOrPendingTransactions){
            if (it.isFinished()) {
                openOrPendingTransactions.remove(it);
                continue;
            } else if (!it.isOneDayOld()) {
                // According to the IDIN specs we are only allowed to check the transaction status once a day
                continue;
            }
            StatusRequest sr = new StatusRequest(it.getTransactionId());
            StatusResponse response = new Communicator().getResponse(sr);
            it.handled();
            switch (response.getStatus()) {
                case StatusResponse.Success:
                case StatusResponse.Cancelled:
                case StatusResponse.Expired:
                case StatusResponse.Failure:
                    closed.append(it.getTransactionId()).append(", ");
                    openOrPendingTransactions.remove(it);
                    break;
                case StatusResponse.Open:
                case StatusResponse.Pending:
                default:
                    break;
            }
        }
        logger.info("Finished with status Requests, ended with: {}", openOrPendingTransactions.size());
        logger.info("Transactions now closed: {}",closed.toString());
        logger.info("Transactions still open: {}",getOpenTransactions());
    }

    public static synchronized IdinTransaction findTransaction(String trxid) {
        for (IdinTransaction it : openOrPendingTransactions) {
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
