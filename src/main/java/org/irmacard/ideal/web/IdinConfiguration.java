package org.irmacard.ideal.web;

import com.google.gson.JsonSyntaxException;
import org.irmacard.api.common.util.GsonUtil;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.net.URL;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.security.KeyManagementException;
import java.security.PublicKey;


public class IdinConfiguration extends Configuration{
    private static final String filename = "iDIN-config.json";
    private static Logger logger = LoggerFactory.getLogger(IdinConfiguration.class);
    private static IdinConfiguration instance;

    private String scheme_manager = "";
    private String idin_issuer = "";
    private String idin_credential = "";
    private String age_limits_credential = "";
    private String api_server_public_key = "";

    private String initials_attribute = "";
    private String lastname_attribute = "";
    private String address_attribute = "";
    private String city_attribute = "";
    private String postalcode_attribute = "";
    private String birthdate_attribute = "";
    private String gender_attribute = "";
    private String country_attribute = "";
    private String ideal_bic_attribute = "";
    private String ideal_iban_attribute = "";
    private String static_salt = "";

    private String idin_issuers_path = "idin-banks.json";
    private transient IdinIssuers iDinIssuers;
    private String hmac_key = "";
    private transient PublicKey apiServerPublickKey;

    public static IdinConfiguration getInstance() {
        if (instance == null) {
            load();
        }
        return instance;
    }


    /**
     * Reloads the configuration from disk so that {@link IdinConfiguration#getInstance()} returns the updated version
     */
    public static void load() {
        try {
            String json = new String(getResource(filename));
            instance = GsonUtil.getGson().fromJson(json, IdinConfiguration.class);
        } catch (IOException |JsonSyntaxException e) {
            logger.warn("Could not load configuration file, using default values!");
            e.printStackTrace();
            instance = new IdinConfiguration();
        }

        System.out.println("Configuration:");
        System.out.println(instance.toString());
    }

    public static void loadIdinConfiguration() {
        try {
            URL config = IdinConfiguration.class.getClassLoader().getResource("config.xml");
            if (config == null)
                throw new Exception("Could not load config.xml");
            net.bankid.merchant.library.Configuration.defaultInstance().Load(config.openStream());
        } catch (Exception e) {
            logger.error("Could not load iDIN configuration");
            logger.error(e.getMessage());
            throw new RuntimeException(e);
        }
    }


    /** Path to the file from which the iDIN issuer list is saved and loaded */
    public Path getIdinIssuersPath() {
        return Paths.get(idin_issuers_path);
    }

    /**
     * Get the list of iDIN issuers (kept up to date by the {@link BackgroundJobManager})
     * @return List of iDIN issuers, or null if the serialized issuer file
     *         (see {@link #getIdinIssuersPath()}) could not be read
     */
    public IdinIssuers getIdinIssuers() {
        if (iDinIssuers == null) {
            try {
                byte[] bytes = Files.readAllBytes(IdinConfiguration.getInstance().getIdinIssuersPath());
                iDinIssuers = GsonUtil.getGson().fromJson(new String(bytes), IdinIssuers.class);
            } catch (Exception e) {
                return null;
            }
        }

        return iDinIssuers;
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

    public String getCountryAttribute() {
        return country_attribute;
    }


    public String getAgeLimitsIssuer() {
        //currently the same issuer
        return idin_issuer;
    }

    public String getAgeLimitsCredential() {
        return age_limits_credential;
    }

    public String getStaticSalt() {
        return static_salt;
    }

    public String getHMACKey() {
        return hmac_key;
    }

    public PublicKey getApiServerPublicKey() {
        if (apiServerPublickKey == null) {
            try {
                apiServerPublickKey = parsePublicKey(getResource(api_server_public_key));
            } catch (KeyManagementException | IOException e) {
                throw new RuntimeException(e);
            }
        }

        return apiServerPublickKey;
    }

    public String getIdealBICAttribute() {
        return ideal_bic_attribute;
    }

    public String getIdealIBANAttribute() {
        return ideal_iban_attribute;
    }
}
