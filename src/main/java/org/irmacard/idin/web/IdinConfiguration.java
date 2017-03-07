package org.irmacard.idin.web;

import com.google.gson.JsonSyntaxException;
import net.bankid.merchant.library.Configuration;
import org.irmacard.api.common.util.GsonUtil;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.URL;
import java.net.URLConnection;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

@SuppressWarnings({"MismatchedQueryAndUpdateOfCollection", "FieldCanBeLocal", "unused"})
public class IdinConfiguration {
	private static Logger logger = LoggerFactory.getLogger(IdinConfiguration.class);
	private static final String filename = "config.json";
	private static IdinConfiguration instance;

	private String idin_issuers_path = "banks.json";
	private transient IdinIssuers issuers;

	/** Path to the file from which the iDIN issier list is saved and loaded */
	public Path getIdinIssuersPath() {
		return Paths.get(idin_issuers_path);
	}

	/**
	 * Get the list of iDIN issuers (kept up to date by the {@link BackgroundJobManager})
	 * @return List of issuers, or null if the serialized issuer file
	 *         (see {@link #getIdinIssuersPath()}) could not be read
	 */
	public IdinIssuers getIdinIssuers() {
		if (issuers == null) {
			try {
				byte[] bytes = Files.readAllBytes(IdinConfiguration.getInstance().getIdinIssuersPath());
				issuers = GsonUtil.getGson().fromJson(new String(bytes), IdinIssuers.class);
			} catch (Exception e) {
				return null;
			}
		}

		return issuers;
	}

	public static void loadIdinConfiguration() {
		try {
			URL config = IdinConfiguration.class.getClassLoader().getResource("config.xml");
			if (config == null)
				throw new Exception("Could not load config.xml");
			Configuration.defaultInstance().Load(config.openStream());
		} catch (Exception e) {
			logger.error("Could not load iDIN configuration");
			logger.error(e.getMessage());
			throw new RuntimeException(e);
		}
	}

	/**
	 * Reloads the configuration from disk so that {@link #getInstance()} returns the updated version
	 */
	public static void load() {
		try {
			String json = new String(getResource(filename));
			instance = GsonUtil.getGson().fromJson(json, IdinConfiguration.class);
		} catch (IOException|JsonSyntaxException e) {
			logger.warn("Could not load configuration file, using default values!");
			instance = new IdinConfiguration();
		}
	}

	public IdinConfiguration() {}

	public static IdinConfiguration getInstance() {
		if (instance == null)
			load();

		return instance;
	}

	public static byte[] getResource(String filename) throws IOException {
		URL url = IdinConfiguration.class.getClassLoader().getResource(filename);
		if (url == null)
			throw new IOException("Could not load file " + filename);

		URLConnection urlCon = url.openConnection();
		urlCon.setUseCaches(false);
		return convertSteamToByteArray(urlCon.getInputStream(), 2048);
	}

	public static byte[] convertSteamToByteArray(InputStream stream, int size) throws IOException {
		byte[] buffer = new byte[size];
		ByteArrayOutputStream os = new ByteArrayOutputStream();

		int line;
		while ((line = stream.read(buffer)) != -1) {
			os.write(buffer, 0, line);
		}
		stream.close();

		os.flush();
		os.close();
		return os.toByteArray();
	}

	@Override
	public String toString() {
		return GsonUtil.getGson().toJson(this);
	}
}
