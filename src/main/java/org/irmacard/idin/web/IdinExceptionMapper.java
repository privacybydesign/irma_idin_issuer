package org.irmacard.idin.web;

import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;
import jakarta.ws.rs.ext.ExceptionMapper;

public class IdinExceptionMapper implements ExceptionMapper<Throwable> {
    /**
     * Convert an exception to a response for the client of the server
     */
     @Override
     public Response toResponse(final Throwable ex) {
         final IdinErrorMessage message = new IdinErrorMessage(ex);

         return Response.status(message.getStatus())
                 .entity(message)
                 .type(MediaType.APPLICATION_JSON)
                 .build();
     }
}
