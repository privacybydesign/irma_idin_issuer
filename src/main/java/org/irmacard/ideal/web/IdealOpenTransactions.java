package org.irmacard.ideal.web;

import com.ing.ideal.connector.IdealConnector;
import com.ing.ideal.connector.IdealException;
import com.ing.ideal.connector.Transaction;

public class IdealOpenTransactions extends OpenTransactions {
    private static IdealOpenTransactions instance;

    public static IdealOpenTransactions getIdealOpenTransactions() {
        if (instance == null) {
            instance = new IdealOpenTransactions();
        }
        return instance;
    }

    public synchronized void requestStates(){
        StringBuilder closed = new StringBuilder();
        int startSize = openOrPendingTransactions.size();
        logger.info("Starting status requests for open and pending statusses, initially: {}", startSize);
        for (String trxID : openOrPendingTransactions) {
            try {
                IdealConnector connector = new IdealConnector();
                Transaction response = connector.requestTransactionStatus(trxID);
                switch (response.getStatus()) {
                    case "Success":
                    case "Cancelled":
                    case "Expired":
                    case "Failure":
                        closed.append(trxID).append(", ");
                        openOrPendingTransactions.remove(trxID);
                        break;
                    case "Open":
                    case "Pending":
                        break;
                    default:
                        break;
                }
            } catch (IdealException e) {
                logger.error("Cannot get transaction status:", e.getMessage());
            }
        }
        logger.info("Finished with status Requests, ended with: {}", openOrPendingTransactions.size());
        logger.info("Transactions now closed: {}",closed.toString());
        logger.info("Transactions still open: {}",getOpenTransactions());
    }
}
