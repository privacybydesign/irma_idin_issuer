package org.irmacard.idin.web;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

public class IdinTransactionTest {

    public static final String TRANSACTION_ID = "transactionId";
    public static final String ENTRANCE_CODE = "entranceCode";

    @Test
    public void getters() {
        final IdinTransaction idinTransaction = new IdinTransaction(TRANSACTION_ID, ENTRANCE_CODE);

        assertEquals(TRANSACTION_ID, idinTransaction.getTransactionId());
        assertEquals(ENTRANCE_CODE, idinTransaction.getEntranceCode());
    }

    @Test
    public void isNotOneDayOld() {
        final IdinTransaction idinTransaction = new IdinTransaction(TRANSACTION_ID, ENTRANCE_CODE);

        assertFalse(idinTransaction.isOneDayOld());
    }

    @Test
    public void isFinished() {
        final IdinTransaction idinTransaction = new IdinTransaction(TRANSACTION_ID, ENTRANCE_CODE);

        assertFalse(idinTransaction.isFinished());

        idinTransaction.finished();

        assertTrue(idinTransaction.isFinished());
    }
}