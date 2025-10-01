/*
 * GsonJerseyProvider.java
 *
 * Copyright (c) 2015, Sietse Ringers, Radboud University
 * All rights reserved.
 *
 * Redistribution and use in source and binary forms, with or without
 * modification, are permitted provided that the following conditions are met:
 *
 * 1. Redistributions of source code must retain the above copyright notice,
 * this list of conditions and the following disclaimer.
 *
 * 2. Redistributions in binary form must reproduce the above copyright notice,
 * this list of conditions and the following disclaimer in the documentation
 * and/or other materials provided with the distribution.
 *
 * 3. Neither the name of the IRMA project nor the names of its
 * contributors may be used to endorse or promote products derived from this
 * software without specific prior written permission.
 *
 * THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS "AS IS"
 * AND ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE
 * IMPLIED WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE
 * ARE DISCLAIMED. IN NO EVENT SHALL THE COPYRIGHT HOLDER OR CONTRIBUTORS BE
 * LIABLE FOR ANY DIRECT, INDIRECT, INCIDENTAL, SPECIAL, EXEMPLARY, OR
 * CONSEQUENTIAL DAMAGES (INCLUDING, BUT NOT LIMITED TO, PROCUREMENT OF
 * SUBSTITUTE GOODS OR SERVICES; LOSS OF USE, DATA, OR PROFITS; OR BUSINESS
 * INTERRUPTION) HOWEVER CAUSED AND ON ANY THEORY OF LIABILITY, WHETHER IN
 * CONTRACT, STRICT LIABILITY, OR TORT (INCLUDING NEGLIGENCE OR OTHERWISE)
 * ARISING IN ANY WAY OUT OF THE USE OF THIS SOFTWARE, EVEN IF ADVISED OF THE
 * POSSIBILITY OF SUCH DAMAGE.
 */

package org.irmacard.idin.web;

import org.irmacard.api.common.util.GsonUtil;

import jakarta.ws.rs.Consumes;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.WebApplicationException;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.MultivaluedMap;
import jakarta.ws.rs.ext.MessageBodyReader;
import jakarta.ws.rs.ext.MessageBodyWriter;
import jakarta.ws.rs.ext.Provider;
import java.io.*;
import java.lang.annotation.Annotation;
import java.lang.reflect.Type;
import java.nio.charset.StandardCharsets;

@Provider
@Produces(MediaType.APPLICATION_JSON)
@Consumes(MediaType.APPLICATION_JSON)
public class GsonJerseyProvider implements MessageBodyWriter<Object>, MessageBodyReader<Object> {

	@Override
	public boolean isReadable(final Class<?> type, final Type genericType,
                              final Annotation[] annotations, final MediaType mediaType) {
		return true;
	}

	@Override
	public Object readFrom(final Class<Object> type, final Type genericType,
                           final Annotation[] annotations, final MediaType mediaType,
                           final MultivaluedMap<String, String> httpHeaders, final InputStream entityStream)
			throws IOException {
		try (final InputStreamReader streamReader = new InputStreamReader(entityStream, StandardCharsets.UTF_8)) {
			return GsonUtil.getGson().fromJson(streamReader, genericType);
		}
	}

	@Override
	public boolean isWriteable(final Class<?> type, final Type genericType,
                               final Annotation[] annotations, final MediaType mediaType) {
		return true;
	}

	@Override
	public long getSize(final Object object, final Class<?> type, final Type genericType,
                        final Annotation[] annotations, final MediaType mediaType) {
		return -1;
	}

	@Override
	public void writeTo(final Object object, final Class<?> type, final Type genericType,
                        final Annotation[] annotations, final MediaType mediaType,
                        final MultivaluedMap<String, Object> httpHeaders,
                        final OutputStream entityStream) throws IOException,
			WebApplicationException {
		try (final OutputStreamWriter writer = new OutputStreamWriter(entityStream, StandardCharsets.UTF_8)) {
			GsonUtil.getGson().toJson(object, genericType, writer);
		}
	}
}
