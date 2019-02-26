package org.irmacard.idx.web;

import io.sentry.Sentry;
import org.glassfish.jersey.server.ResourceConfig;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.xml.sax.SAXException;
import org.javalite.activejdbc.Base;

import javax.ws.rs.ApplicationPath;
import javax.xml.parsers.ParserConfigurationException;
import java.io.IOException;

@ApplicationPath("/")
public class IdinApplication extends ResourceConfig {
	private static Logger logger = LoggerFactory.getLogger(IdinApplication.class);

	public IdinApplication() throws IOException, ParserConfigurationException, SAXException {
		register(GsonJerseyProvider.class);
		register(IdinResource.class);
		register(IdinExceptionMapper.class);

		String sentry_dsn = IdinConfiguration.getInstance().getSentryDSN();
		if (sentry_dsn.length() != 0) {
			Sentry.init(sentry_dsn);
		}

		logger.info("Starting IRMA iDin server");

		openDatabase();
		closeDatabase();
	}

	public static void openDatabase() {
		if(!Base.hasConnection()) {
			// TODO: make configurable
			Base.open("jdbc/irma_ideal");
		}
	}

	public static void closeDatabase() {
		Base.close();
	}
}