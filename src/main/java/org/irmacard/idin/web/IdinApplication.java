package org.irmacard.idin.web;

import net.bankid.merchant.library.Configuration;
import org.glassfish.jersey.server.ResourceConfig;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.xml.sax.SAXException;

import javax.ws.rs.ApplicationPath;
import javax.xml.parsers.ParserConfigurationException;
import java.io.IOException;
import java.net.URL;

@ApplicationPath("/")
public class IdinApplication extends ResourceConfig {
	private static Logger logger = LoggerFactory.getLogger(IdinApplication.class);

	public IdinApplication() throws IOException, ParserConfigurationException, SAXException {
		URL config = IdinApplication.class.getClassLoader().getResource("config.xml");
		if (config == null)
			throw new RuntimeException("Could not load config.xml");
		Configuration.defaultInstance().Load(config.openStream());

		register(IdinResource.class);

		logger.info("Starting IRMA iDIN server");
	}
}