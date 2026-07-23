package org.irmacard.idin.web;

import net.bankid.merchant.library.ErrorResponse;

import java.io.PrintWriter;
import java.io.StringWriter;

/**
 * An error message for clients of this server, meant for JSON (d)serialization of {@link IdinException}s
 * (although it can also hold other {@link Throwable}s).
 */
public class IdinErrorMessage {
    private final int status;
    private final String description;
    private final String message;


    /**
     * Construct a new error message.
     *
     * Note: the stack trace of {@code ex} is deliberately NOT retained on this object, so that it
     * can never be serialized into a response body sent to clients. Stack traces are logged
     * server-side only (see {@link IdinExceptionMapper}).
     *
     * @param ex cause of the problem
     */
    public IdinErrorMessage(final Throwable ex) {
        if (ex instanceof IdinException) {
            //IF we get an IdinException, then the blame lies upstream
            this.status = 504;
            final ErrorResponse error = ((IdinException) ex).getError();
            this.message = error.getErrorMessage();
            this.description = error.getConsumerMessage();
        } else {
            this.status = 500; // if we get another exception, then the blame lies in our server....
            this.message = ex.toString(); // Include exception classname
            this.description = "Something unexpected went wrong";
        }
    }

    /** The HTTP status. */
    public int getStatus() {
        return status;
    }

    /** Human-readable description of the problem */
    public String getDescription() {
        return description;
    }

    /** The causer or subject of the error; a suggested alternative value;
     *  or the message of an uncaught exception. */
    public String getMessage() {
        return message;
    }

    /**
     * Renders the stack trace of a throwable as a string, for server-side logging only.
     * This is intentionally not exposed as an instance field/getter so it is never serialized
     * into a client-facing error response.
     */
    public static String getExceptionStacktrace(final Throwable ex) {
        final StringWriter errorStackTrace = new StringWriter();
        ex.printStackTrace(new PrintWriter(errorStackTrace));
        return errorStackTrace.toString();
    }
}
