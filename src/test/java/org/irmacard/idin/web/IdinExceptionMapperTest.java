package org.irmacard.idin.web;

import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

public class IdinExceptionMapperTest {

    private final IdinExceptionMapper mapper = new IdinExceptionMapper();

    @Test
    void toResponse_setsStatusEntityAndJsonType() {
        final Throwable throwable = new RuntimeException();

        final Response response = mapper.toResponse(throwable);

        assertNotNull(response);
        assertEquals(MediaType.APPLICATION_JSON, response.getMediaType().toString());

        final Object entity = response.getEntity();
        assertNotNull(entity);
        assertInstanceOf(IdinErrorMessage.class, entity);

        final IdinErrorMessage msg = (IdinErrorMessage) entity;
        assertEquals(msg.getStatus(), response.getStatus());
    }

    @Test
    void toResponse_worksForDifferentExceptionTypes() {
        final Throwable ex = new IllegalArgumentException();

        final Response response = mapper.toResponse(ex);

        assertNotNull(response);
        assertEquals(MediaType.APPLICATION_JSON, response.getMediaType().toString());

        final Object entity = response.getEntity();
        assertInstanceOf(IdinErrorMessage.class, entity);

        final IdinErrorMessage msg = (IdinErrorMessage) entity;
        assertEquals(msg.getStatus(), response.getStatus());
    }

}