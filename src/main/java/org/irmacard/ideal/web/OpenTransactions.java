package org.irmacard.ideal.web;

import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

public class OpenTransactions {

    protected org.slf4j.Logger logger = LoggerFactory.getLogger(OpenTransactions.class);
    protected Set<String> openOrPendingTransactions = new HashSet<>();
    private Set<String> openOrPendingNew = new HashSet<>();

    public OpenTransactions() {}

    public synchronized List<String> getOpenTransactionsCopy(){
        ArrayList<String> list = new ArrayList<>(openOrPendingTransactions);
        list.addAll(openOrPendingNew);
        return list;
    }

    public String getOpenTransactions(){
        StringBuilder open = new StringBuilder();
        for (String trxID: openOrPendingTransactions){
            open.append(trxID).append(", ");
        }
        for (String trxID: openOrPendingNew){
            open.append(trxID).append(", ");
        }
        return open.toString();
    }

    public synchronized void addTransaction (String trxId){
        openOrPendingNew.add(trxId);
    }

    public synchronized void removeTransaction (String trxId){
        openOrPendingNew.remove(trxId);
    }

    public synchronized int getHowMany(){
        return openOrPendingTransactions.size() + openOrPendingNew.size();
    }

    public synchronized void newDay(){
        openOrPendingTransactions.addAll(openOrPendingNew);
        openOrPendingNew = new HashSet<>();
    }

    //Prevent cloning of this class
    public Object clone() throws CloneNotSupportedException {
        throw new CloneNotSupportedException();
    }
}
