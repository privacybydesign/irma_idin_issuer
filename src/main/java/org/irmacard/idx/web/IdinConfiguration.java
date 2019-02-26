package org.irmacard.idx.web;

import foundation.privacybydesign.common.BaseConfiguration;
import io.jsonwebtoken.SignatureAlgorithm;
import org.irmacard.api.common.util.GsonUtil;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.net.URL;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.security.KeyManagementException;
import java.security.PrivateKey;
import java.security.PublicKey;


@SuppressWarnings({"FieldCanBeLocal"})
public class IdinConfiguration extends BaseConfiguration<IdinConfiguration> {
    private static Logger logger = LoggerFactory.getLogger(IdinConfiguration.class);

    static {
        BaseConfiguration.clazz = IdinConfiguration.class;
        BaseConfiguration.environmentVarPrefix = "IRMA_IDIN_CONF_";
        BaseConfiguration.confDirEnvironmentVarName = "IRMA_IDIN_CONF";
        BaseConfiguration.logger = IdinConfiguration.logger;
        BaseConfiguration.filename = "iDIN-config.json";
        BaseConfiguration.confDirName = "irma_idin_server";
        BaseConfiguration.printOnLoad = true;
    }

    private static final String CONFIG_FILENAME = "iDIN-config.json";

    private String server_name = "";
    private String human_readable_name = "";
    private String jwt_privatekey = "";
    private String api_server_public_key = "";
    private String idin_issuers_path = "idin-banks.json";
    private String sentry_dsn = "";

    private String token_static_salt = "";
    private String token_hmac_key = "";

    private String scheme_manager = "";
    private String idin_issuer = "";
    private String idin_credential = "";
    private String age_limits_credential = "";

    private String ideal_bic_attribute = "";
    private String ideal_iban_attribute = "";

    private String initials_attribute = "";
    private String lastname_attribute = "";
    private String address_attribute = "";
    private String city_attribute = "";
    private String postalcode_attribute = "";
    private String birthdate_attribute = "";
    private String gender_attribute = "";
    private String country_attribute = "";

    private transient PrivateKey jwtPrivateKey;
    private transient PublicKey apiServerPublickKey;
    private transient IdinIssuers iDinIssuers;

    public static IdinConfiguration getInstance() {
        return (IdinConfiguration) BaseConfiguration.getInstance();
    }

    public String getSentryDSN() {
        return sentry_dsn;
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

    public String getServerName() {
        return server_name;
    }

    public String getHumanReadableName() {
        if (human_readable_name == null || human_readable_name.length() == 0)
            return server_name;
        else
            return human_readable_name;
    }


    public PrivateKey getJwtPrivateKey() throws KeyManagementException {
        if (jwtPrivateKey == null) {
            jwtPrivateKey = BaseConfiguration.getPrivateKey(jwt_privatekey);
        }

        return jwtPrivateKey;
    }

    public SignatureAlgorithm getJwtAlgorithm() {
        return SignatureAlgorithm.RS256;
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

    public String getTokenStaticSalt() {
        return token_static_salt;
    }

    public String getTokenHMACKey() {
        return token_hmac_key;
    }

    public PublicKey getApiServerPublicKey() throws KeyManagementException {
        if (apiServerPublickKey == null) {
            apiServerPublickKey = BaseConfiguration.getPublicKey(api_server_public_key);
        }

        return apiServerPublickKey;
    }

    public String getIdealBICAttribute() {
        return ideal_bic_attribute;
    }

    public String getIdealIBANAttribute() {
        return ideal_iban_attribute;
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
}
