package org.irmacard.idin.web;

import org.glassfish.jersey.server.ResourceConfig;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.xml.sax.SAXException;

import javax.ws.rs.ApplicationPath;
import javax.xml.parsers.ParserConfigurationException;
import java.io.IOException;

@ApplicationPath("/")
public class IdinApplication extends ResourceConfig {
	private static Logger logger = LoggerFactory.getLogger(IdinApplication.class);

	public IdinApplication() throws IOException, ParserConfigurationException, SAXException {
		// TODO find out why without this, connections to the acquirer fail
		System.setProperty("https.protocols", "TLSv1.2");

		register(GsonJerseyProvider.class);
		register(IdinResource.class);

		logger.info("Starting IRMA iDIN server");
	}
}