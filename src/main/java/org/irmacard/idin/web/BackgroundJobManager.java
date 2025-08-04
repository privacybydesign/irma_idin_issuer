package org.irmacard.idin.web;

import net.bankid.merchant.library.Communicator;
import net.bankid.merchant.library.DirectoryResponse;
import org.irmacard.api.common.util.GsonUtil;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.servlet.ServletContextEvent;
import javax.servlet.ServletContextListener;
import javax.servlet.annotation.WebListener;
import java.nio.file.Files;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

@WebListener
public class BackgroundJobManager implements ServletContextListener {
	private static final Logger LOGGER = LoggerFactory.getLogger(BackgroundJobManager.class);
	private ScheduledExecutorService scheduler;

	static {
		// This has to be loaded before the first time the cronjob is run,
		// as the call to new Communicator(); needs the iDIN configuration.
		IdinConfiguration.loadIdinConfiguration();
	}

	@Override
	public void contextInitialized(final ServletContextEvent event) {
		LOGGER.info("Setting up issuer fetching cron task");
		scheduler = Executors.newSingleThreadScheduledExecutor();

		scheduler.scheduleAtFixedRate(() -> {
            //attempting to close old sessions with status Open or Pending
            OpenTransactions.requestStates();

            try {
                IdinIssuers issuers = IdinConfiguration.getInstance().getIdinIssuers();
                if (issuers != null && !issuers.shouldUpdate()) {
                    LOGGER.info("iDIN issuer list is still up to date, not refreshing");
                    return;
                }

                LOGGER.info("Updating iDIN issuer list");
                final DirectoryResponse response = new Communicator().getDirectory();

                if (response.getIsError()) {
                    LOGGER.error("Retrieving iDIN issuer list failed!");
                    LOGGER.error(response.getErrorResponse().getErrorMessage());
                    LOGGER.error(response.getErrorResponse().getErrorDetails());
                    return;
                }

                issuers = new IdinIssuers(response.getIssuersByCountry());
                Files.write(
                        IdinConfiguration.getInstance().getIdinIssuersPath(),
                        GsonUtil.getGson().toJson(issuers).getBytes()
                );
            } catch (final Exception e) {
                LOGGER.error("Failed to run issuer fetching cron task", e);
            }
        }, 0, 1, TimeUnit.DAYS);
	}

	@Override
	public void contextDestroyed(final ServletContextEvent event) {
		scheduler.shutdownNow();
	}
}
