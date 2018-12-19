package org.irmacard.idx.web;

import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import javax.ws.rs.ext.ExceptionMapper;

public class IdinExceptionMapper implements ExceptionMapper<Throwable> {
    /**
     * Convert an exception to a response for the client of the server
     */
     @Override
     public Response toResponse(Throwable ex) {
         IdinErrorMessage message = new IdinErrorMessage(ex);

         return Response.status(message.getStatus())
                 .entity(message)
                 .type(MediaType.APPLICATION_JSON)
                 .build();
     }
}
