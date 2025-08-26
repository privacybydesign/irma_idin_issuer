package org.irmacard.idin.web;

import net.bankid.merchant.library.Communicator;
import net.bankid.merchant.library.StatusRequest;
import net.bankid.merchant.library.StatusResponse;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.MockedConstruction;

import java.lang.reflect.Field;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

public class OpenTransactionsTest {


    private static final String TRANSACTION_ID_1 = "TRX-BBB";
    private static final String TRANSACTION_ID_2 = "TRX-AAA";
    private static final String ENTRANCE_CODE = "ENT-123";
    public static final IdinTransaction IDIN_TRANSACTION_1 = new IdinTransaction(TRANSACTION_ID_1, ENTRANCE_CODE);
    public static final IdinTransaction IDIN_TRANSACTION_2 = new IdinTransaction(TRANSACTION_ID_2, ENTRANCE_CODE);

    @BeforeEach
    @SuppressWarnings("unchecked")
    public void resetOpenSet() throws Exception {
        final Field openOrPendingTransactions = OpenTransactions.class.getDeclaredField("OPEN_OR_PENDING_TRANSACTIONS");
        openOrPendingTransactions.setAccessible(true);
        final Set<IdinTransaction> idinTransactionSet = (Set<IdinTransaction>) openOrPendingTransactions.get(null);
        idinTransactionSet.clear();
    }

    @Test
    public void getOpenTransactions_returnsBothIds_inSomeOrder_withTrailingCommaSpace() {
        setTwoIdinTransactions();

        final String result = OpenTransactions.getOpenTransactions();

        final String expectedA = TRANSACTION_ID_1 + ", " + TRANSACTION_ID_2 + ", ";
        final String expectedB = TRANSACTION_ID_2 + ", " + TRANSACTION_ID_1 + ", ";

        assertTrue(result.equals(expectedA) || result.equals(expectedB),
                () -> "Unexpected ordering/format. Got: '" + result + "'");
    }

    @Test
    public void getHowMany_returns2_not1() {
        setTwoIdinTransactions();

        assertEquals(2, OpenTransactions.getHowMany());
        assertNotEquals(1, OpenTransactions.getHowMany());
    }

    @Test
    public void requestStates_finishedTransaction_isRemoved_withoutCallingCommunicator() {
        final IdinTransaction finished = mock(IdinTransaction.class);
        when(finished.isFinished()).thenReturn(true);
        when(finished.getTransactionId()).thenReturn(TRANSACTION_ID_2);
        OpenTransactions.addTransaction(finished);

        try (final MockedConstruction<Communicator> mocked = mockConstruction(Communicator.class)) {
            OpenTransactions.requestStates();

            assertEquals(0, OpenTransactions.getHowMany());
            assertEquals(1, mocked.constructed().size());
        }
        verify(finished, never()).handled();
    }

    @Test
    public void requestStates_notOneDayOld_isKept_andNoStatusCheck() {
        final IdinTransaction young = mock(IdinTransaction.class);
        when(young.isFinished()).thenReturn(false);
        when(young.isOneDayOld()).thenReturn(false);
        when(young.getTransactionId()).thenReturn(TRANSACTION_ID_2);
        OpenTransactions.addTransaction(young);

        try (final MockedConstruction<Communicator> mocked = mockConstruction(Communicator.class)) {
            OpenTransactions.requestStates();

            assertEquals(1, OpenTransactions.getHowMany());
            assertEquals(1, mocked.constructed().size());
        }
        verify(young, never()).handled();
    }

    @Test
    public void requestStates_successResponse_removesTransaction_andCallsHandled() {
        final IdinTransaction idinTransaction = mock(IdinTransaction.class);
        when(idinTransaction.isFinished()).thenReturn(false);
        when(idinTransaction.isOneDayOld()).thenReturn(true);
        when(idinTransaction.getTransactionId()).thenReturn(TRANSACTION_ID_2);
        OpenTransactions.addTransaction(idinTransaction);

        final StatusResponse resp = mock(StatusResponse.class);
        when(resp.getStatus()).thenReturn(StatusResponse.Success);

        try (final MockedConstruction<Communicator> mocked =
                     mockConstruction(Communicator.class, (mock, ctx) ->
                             when(mock.getResponse(any(StatusRequest.class))).thenReturn(resp))) {
            OpenTransactions.requestStates();

            assertEquals(0, OpenTransactions.getHowMany());
            assertEquals(1, mocked.constructed().size());
            verify(mocked.constructed().getFirst(), times(1)).getResponse(any(StatusRequest.class));
        }
        verify(idinTransaction, times(1)).handled();
    }

    @Test
    public void requestStates_openResponse_keepsTransaction_andCallsHandled() {
        final IdinTransaction idinTransaction = mock(IdinTransaction.class);
        when(idinTransaction.isFinished()).thenReturn(false);
        when(idinTransaction.isOneDayOld()).thenReturn(true);
        when(idinTransaction.getTransactionId()).thenReturn(TRANSACTION_ID_2);
        OpenTransactions.addTransaction(idinTransaction);

        final StatusResponse resp = mock(StatusResponse.class);
        when(resp.getStatus()).thenReturn(StatusResponse.Open);

        try (final MockedConstruction<Communicator> mocked =
                     mockConstruction(Communicator.class, (mock, ctx) ->
                             when(mock.getResponse(any(StatusRequest.class))).thenReturn(resp))) {
            OpenTransactions.requestStates();

            assertEquals(1, OpenTransactions.getHowMany());
            assertEquals(1, mocked.constructed().size());
            verify(mocked.constructed().getFirst(), times(1)).getResponse(any(StatusRequest.class));
        }
        verify(idinTransaction, times(1)).handled();
    }

    @Test
    public void requestStates_communicatorThrows_keepsTransaction_andDoesNotCallHandled() {
        final IdinTransaction idinTransaction = mock(IdinTransaction.class);
        when(idinTransaction.isFinished()).thenReturn(false);
        when(idinTransaction.isOneDayOld()).thenReturn(true);
        when(idinTransaction.getTransactionId()).thenReturn(TRANSACTION_ID_2);
        OpenTransactions.addTransaction(idinTransaction);

        try (final MockedConstruction<Communicator> mocked =
                     mockConstruction(Communicator.class, (mock, ctx) ->
                             when(mock.getResponse(any(StatusRequest.class))).thenThrow(new RuntimeException()))) {
            OpenTransactions.requestStates();

            assertEquals(1, OpenTransactions.getHowMany());
            assertEquals(1, mocked.constructed().size());
            verify(mocked.constructed().getFirst(), times(1)).getResponse(any(StatusRequest.class));
        }
        verify(idinTransaction, never()).handled();
    }

    @Test
    public void findTransaction_findsOne() {
        setTwoIdinTransactions();
        assertEquals(IDIN_TRANSACTION_2, OpenTransactions.findTransaction(TRANSACTION_ID_2));
    }

    @Test
    public void findTransaction_findsNull() {
        final String nonExistentTransactionId = "test";
        assertNull(OpenTransactions.findTransaction(nonExistentTransactionId));
    }

    @Test
    public void clone_throwsCloneNotSupportedException() {
        final OpenTransactions openTransactions = new OpenTransactions();
        assertThrows(CloneNotSupportedException.class, openTransactions::clone);
    }

    private static void setTwoIdinTransactions() {
        OpenTransactions.addTransaction(IDIN_TRANSACTION_2);
        OpenTransactions.addTransaction(IDIN_TRANSACTION_1);
    }
}