package org.irmacard.idx.web;

import net.bankid.merchant.library.StatusRequest;
import net.bankid.merchant.library.Communicator;
import net.bankid.merchant.library.StatusResponse;

public class IdinOpenTransactions extends OpenTransactions {
    private static IdinOpenTransactions instance;

    public static IdinOpenTransactions getIdinOpenTransactions() {
        if (instance == null) {
            instance = new IdinOpenTransactions();
        }
        return instance;
    }

    public synchronized void requestStates(){
        StringBuilder closed = new StringBuilder();
        int startSize = openOrPendingTransactions.size();
        logger.info("Starting status requests for open and pending statusses, initially: {}", startSize);
        for (String trxID: openOrPendingTransactions){
            StatusRequest sr = new StatusRequest(trxID);
            StatusResponse response = new Communicator().getResponse(sr);
            switch (response.getStatus()) {
                case StatusResponse.Success:
                case StatusResponse.Cancelled:
                case StatusResponse.Expired:
                case StatusResponse.Failure:
                    closed.append(trxID).append(", ");
                    openOrPendingTransactions.remove(trxID);
                    break;
                case StatusResponse.Open:
                case StatusResponse.Pending:
                    break;
                default:
                    break;
            }
        }
        logger.info("Finished with status Requests, ended with: {}", openOrPendingTransactions.size());
        logger.info("Transactions now closed: {}",closed.toString());
        logger.info("Transactions still open: {}",getOpenTransactions());
    }
}
