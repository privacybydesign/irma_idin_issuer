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
    private static Set<String> openOrPendingTransactions = new HashSet<>();
    private static Set<String> openOrPendingNew = new HashSet<>();

    public OpenTransactions() {}

    public static synchronized List<String> getOpenTransactionsCopy(){
        ArrayList<String> list = new ArrayList<>(openOrPendingTransactions);
        list.addAll(openOrPendingNew);
        return list;
    }

    public static String getOpenTransactions(){
        StringBuilder open = new StringBuilder();
        for (String trxID: openOrPendingTransactions){
            open.append(trxID).append(", ");
        }
        for (String trxID: openOrPendingNew){
            open.append(trxID).append(", ");
        }
        return open.toString();
    }

    public static synchronized void addTransaction (String trxId){
        openOrPendingNew.add(trxId);
    }

    public static synchronized int getHowMany(){
        return openOrPendingTransactions.size() + openOrPendingNew.size();
    }

    public static synchronized void requestStates(){
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

    public static synchronized void newDay(){
        openOrPendingTransactions.addAll(openOrPendingNew);
        openOrPendingNew = new HashSet<>();
    }

    //Prevent cloning of this class
    public Object clone() throws CloneNotSupportedException {
        throw new CloneNotSupportedException();
    }
}
