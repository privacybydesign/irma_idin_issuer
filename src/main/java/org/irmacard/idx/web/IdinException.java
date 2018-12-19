package org.irmacard.idx.web;

import net.bankid.merchant.library.ErrorResponse;

/**
 * Exception occuring during usage of the Idin Server.
 */
public class IdinException extends RuntimeException {

    private ErrorResponse error;

    public IdinException (ErrorResponse resp){
        error = resp;
    }

    public ErrorResponse getError() {
        return error;
    }
}
