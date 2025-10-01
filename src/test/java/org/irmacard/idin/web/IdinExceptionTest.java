package org.irmacard.idin.web;

import net.bankid.merchant.library.ErrorResponse;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

public final class IdinExceptionTest {

    public static final String ERROR_CODE = "E001";
    public static final String ERROR_MESSAGE = "Some error occurred";

    @Test
    public void constructor() {
        final ErrorResponse mockError = mock(ErrorResponse.class);
        when(mockError.toString()).thenReturn(ERROR_CODE + ": " + ERROR_MESSAGE);

        final IdinException ex = new IdinException(mockError);

        assertSame(mockError, ex.getError());

        assertNotNull(ex.getError());
    }

    @Test
    public void getError() {
        final ErrorResponse mockError = mock(ErrorResponse.class);

        final IdinException ex = new IdinException(mockError);

        assertEquals(mockError, ex.getError());
    }
}
