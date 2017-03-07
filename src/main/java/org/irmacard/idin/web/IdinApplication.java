package org.irmacard.idin.web;

import org.glassfish.jersey.server.ResourceConfig;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.ws.rs.ApplicationPath;

@ApplicationPath("/")
public class IdinApplication extends ResourceConfig {
	private static Logger logger = LoggerFactory.getLogger(IdinApplication.class);

	public IdinApplication() {
		register(IdinResource.class);
	}
}