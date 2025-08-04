package org.irmacard.idin.web;

public class IdinTransaction {
    private final String transactionId;
    private final String entranceCode;
    private long lastHandled;
    private boolean finished = false;

    private static final long DAY_IN_MILLIS = 1000*60*60*24;

    public IdinTransaction(final String trxid, final String ec) {
        this.transactionId = trxid;
        this.entranceCode = ec;
        this.handled();
    }

    public String getTransactionId() {
        return transactionId;
    }

    public String getEntranceCode() {
        return entranceCode;
    }

    public void handled() {
        lastHandled = System.currentTimeMillis();
    }

    public boolean isOneDayOld() {
        return this.lastHandled + DAY_IN_MILLIS <= System.currentTimeMillis();
    }

    public boolean isFinished() {
        return finished;
    }

    public void finished() {
        finished = true;
    }
}
