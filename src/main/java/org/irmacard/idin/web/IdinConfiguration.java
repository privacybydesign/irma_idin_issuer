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

	private String server_name = "IRMAiDIN_test";
	private String human_readable_name;

	private String server_url = "";

	private String scheme_manager = "";
	private String idin_issuer = "";
	private String idin_credential = "";


	private String initials_attribute = "";
	private String lastname_attribute = "";
	private String address_attribute = "";
	private String city_attribute = "";
	private String postalcode_attribute = "";
	private String birthdate_attribute = "";
	private String gender_attribute = "";
	private String email_attribute = "";
	private String tel_attribute = "";

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
			e.printStackTrace();
			instance = new IdinConfiguration();
		}

		System.out.println("Configuration:");
		System.out.println(instance.toString());
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
		return convertStreamToByteArray(urlCon.getInputStream(), 2048);
	}

	public static byte[] convertStreamToByteArray(InputStream stream, int size) throws IOException {
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

	public String getUrl() {
		if (server_url.endsWith("/")) return server_url + "irma_idin_server";
		else return server_url + "/irma_idin_server";
	}

	public boolean isHttpsEnabled() {
		return server_url.startsWith("https://");
	}

	public String getSchemeManager() {
		return scheme_manager;
	}

	public String getIdinIssuer() {
		return idin_issuer;
	}

	public String getIdinCredential() {
		return idin_credential;
	}

	public String getEmailAttribute() {
		return email_attribute;
	}

	public String getTelephoneAttribute() { return tel_attribute;}

	public String getInitialsAttribute() {
		return initials_attribute;
	}

	public String getLastnameAttribute() {
		return lastname_attribute;
	}

	public String getAddressAttribute() {
		return address_attribute;
	}

	public String getCityAttribute() {
		return city_attribute;
	}

	public String getPostalcodeAttribute() {
		return postalcode_attribute;
	}

	public String getBirthdateAttribute() {
		return birthdate_attribute;
	}

	public String getGenderAttribute() {
		return gender_attribute;
	}


	public String getServerName() {
		return server_name;
	}

	public String getHumanReadableName() {
		if (human_readable_name == null || human_readable_name.length() == 0)
			return server_name;
		else
			return human_readable_name;
	}

	@Override
	public String toString() {
		return GsonUtil.getGson().toJson(this);
	}


}
