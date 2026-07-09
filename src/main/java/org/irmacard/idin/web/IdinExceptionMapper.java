package org.irmacard.idin.web;

import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;
import jakarta.ws.rs.ext.ExceptionMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class IdinExceptionMapper implements ExceptionMapper<Throwable> {
    private static final Logger LOGGER = LoggerFactory.getLogger(IdinExceptionMapper.class);

    /**
     * Convert an exception to a response for the client of the server.
     * The full stack trace is logged server-side only; it is never included in the response body.
     */
     @Override
     public Response toResponse(final Throwable ex) {
         final IdinErrorMessage message = new IdinErrorMessage(ex);

         // Log the full stack trace server-side; clients only receive status + sanitized description.
         LOGGER.error("Returning error response with status {}", message.getStatus(), ex);

         return Response.status(message.getStatus())
                 .entity(message)
                 .type(MediaType.APPLICATION_JSON)
                 .build();
     }
}
