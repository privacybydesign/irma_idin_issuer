package org.irmacard.idin.web;

import net.bankid.merchant.library.ErrorResponse;

/**
 * Exception occuring during usage of the Idin Server.
 */
public class IdinException extends RuntimeException {

    private final ErrorResponse error;

    public IdinException (final ErrorResponse resp){
        error = resp;
    }

    public ErrorResponse getError() {
        return error;
    }
}
