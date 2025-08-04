package org.irmacard.idin.web;

import org.glassfish.jersey.server.ResourceConfig;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import jakarta.ws.rs.ApplicationPath;

@ApplicationPath("/")
public class IdinApplication extends ResourceConfig {
	private static final Logger LOGGER = LoggerFactory.getLogger(IdinApplication.class);

	public IdinApplication() {
		register(GsonJerseyProvider.class);
		register(IdinResource.class);
		register(IdinExceptionMapper.class);

		LOGGER.info("Starting IRMA iDIN server");

	}



}