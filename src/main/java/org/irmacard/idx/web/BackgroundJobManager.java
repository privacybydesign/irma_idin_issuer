package org.irmacard.idx.web;

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
	private static Logger logger = LoggerFactory.getLogger(BackgroundJobManager.class);
	private ScheduledExecutorService scheduler;

	static {
		// This has to be loaded before the first time the cronjob is run,
		// as the call to new Communicator(); needs the iDIN configuration.
		// same goes for the iDEAL Connector
		IdinConfiguration.loadIdinConfiguration();
	}

	@Override
	public void contextInitialized(ServletContextEvent event) {
		logger.info("Setting up issuer fetching cron task");
		scheduler = Executors.newSingleThreadScheduledExecutor();

		scheduler.scheduleAtFixedRate(new Runnable() {
			@Override public void run() {
				//attempting to close old sessions with status Open or Pending
				IdinOpenTransactions.getIdinOpenTransactions().requestStates();
				//Swapping to a new list for this day, so that we only request a state once per day.
				IdinOpenTransactions.getIdinOpenTransactions().newDay();
				//refresh Issuerlists
				refreshIdinIssuerList();
			}
		}, 0, 1, TimeUnit.DAYS);
	}

	private void refreshIdinIssuerList() {
		try {
            IdinIssuers idinIssuers = IdinConfiguration.getInstance().getIdinIssuers();
            if (idinIssuers != null && !idinIssuers.shouldUpdate()) {
                logger.info("iDIN issuer list is still up to date, not refreshing");
                return;
            }

            logger.info("Updating iDIN issuer list");
            DirectoryResponse response = new Communicator().getDirectory();

            if (response.getIsError()) {
                logger.error("Retrieving iDIN issuer list failed!");
                logger.error(response.getErrorResponse().getErrorMessage());
                logger.error(response.getErrorResponse().getErrorDetails());
                return;
            }

            idinIssuers = new IdinIssuers(response.getIssuersByCountry());
            Files.write(
                    IdinConfiguration.getInstance().getIdinIssuersPath(),
                    GsonUtil.getGson().toJson(idinIssuers).getBytes()
            );
        } catch (Exception e) {
            logger.error("Failed to run idin issuer fetching cron task", e);
            e.printStackTrace();
        }
	}

	@Override
	public void contextDestroyed(ServletContextEvent event) {
		scheduler.shutdownNow();
	}
}
