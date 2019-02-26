package org.irmacard.idx.web;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import javax.ws.rs.ext.ExceptionMapper;

public class IdinExceptionMapper implements ExceptionMapper<Throwable> {
    private static Logger logger = LoggerFactory.getLogger(IdinApplication.class);

    /**
     * Convert an exception to a response for the client of the server
     */
     @Override
     public Response toResponse(Throwable ex) {
         logger.error("caught exception", ex);

         return Response.status(Response.Status.INTERNAL_SERVER_ERROR)
                 .entity("error:uncaught")
                 .build();
     }
}
