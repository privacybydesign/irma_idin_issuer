package org.irmacard.idin.web;

import net.bankid.merchant.library.ErrorResponse;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

public final class IdinErrorMessageTest {

    public static final int STATUS_UPSTREAM = 504;
    public static final int STATUS_SERVER = 500;
    public static final String CONSUMER_MESSAGE = "Please try again later";
    public static final String ERROR_MESSAGE = "Upstream gateway timeout";
    public static final String DEFAULT_DESCRIPTION = "Something unexpected went wrong";
    public static final String RUNTIME_EXCEPTION_TEXT = "boom";

    @Test
    public void forIdinException_uses504_andConsumerAndErrorMessages() {
        final ErrorResponse errorResponse = mock(ErrorResponse.class);
        when(errorResponse.getConsumerMessage()).thenReturn(CONSUMER_MESSAGE);
        when(errorResponse.getErrorMessage()).thenReturn(ERROR_MESSAGE);

        final IdinException idinException = new IdinException(errorResponse);

        final IdinErrorMessage idinErrorMessage = new IdinErrorMessage(idinException);

        assertEquals(STATUS_UPSTREAM, idinErrorMessage.getStatus());
        assertEquals(CONSUMER_MESSAGE, idinErrorMessage.getDescription());
        assertEquals(ERROR_MESSAGE, idinErrorMessage.getMessage());

        final String stacktrace = idinErrorMessage.getStacktrace();
        assertNotNull(stacktrace);
        assertFalse(stacktrace.isEmpty());
        assertTrue(stacktrace.contains(IdinException.class.getName()));
    }

    @Test
    public void forGenericThrowable_uses500_defaultDescription_andToStringAsMessage() {
        final RuntimeException runtimeException = new RuntimeException(RUNTIME_EXCEPTION_TEXT);

        final IdinErrorMessage idinErrorMessage = new IdinErrorMessage(runtimeException);

        assertEquals(STATUS_SERVER, idinErrorMessage.getStatus());
        assertEquals(DEFAULT_DESCRIPTION, idinErrorMessage.getDescription());
        assertEquals(runtimeException.toString(), idinErrorMessage.getMessage());

        final String stacktrace = idinErrorMessage.getStacktrace();
        assertNotNull(stacktrace);
        assertFalse(stacktrace.isEmpty());
        assertTrue(stacktrace.contains(RuntimeException.class.getName()));
        assertTrue(stacktrace.contains(RUNTIME_EXCEPTION_TEXT));
    }

    @Test
    public void static_getExceptionStacktrace_returnsNonEmptyString() {
        final String badArg = "bad arg";
        final IllegalArgumentException illegalArgumentException = new IllegalArgumentException(badArg);
        final String stacktrace = IdinErrorMessage.getExceptionStacktrace(illegalArgumentException);

        assertNotNull(stacktrace);
        assertFalse(stacktrace.isEmpty());
        assertTrue(stacktrace.contains(IllegalArgumentException.class.getName()));
    }
}
