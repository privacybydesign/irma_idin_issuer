package org.irmacard.idin.web;

import com.google.gson.JsonSyntaxException;
import foundation.privacybydesign.common.BaseConfiguration;
import io.jsonwebtoken.SignatureAlgorithm;
import net.bankid.merchant.library.Configuration;
import org.irmacard.api.common.util.GsonUtil;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.URL;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.security.*;
import java.security.spec.InvalidKeySpecException;
import java.security.spec.PKCS8EncodedKeySpec;
import java.security.spec.X509EncodedKeySpec;

@SuppressWarnings({"MismatchedQueryAndUpdateOfCollection", "FieldCanBeLocal", "unused"})
public class IdinConfiguration extends BaseConfiguration {
    private static final Logger LOGGER = LoggerFactory.getLogger(IdinConfiguration.class);
    private static final String FILENAME = "config.json";
    private static IdinConfiguration instance;

    static {
        BaseConfiguration.confDirName = "irma_idin_issuer";
    }

    private String enroll_url;
    private String server_name = "IRMAiDIN_test";
    private String human_readable_name;

    private String return_url = "";

    private String scheme_manager = "";
    private String idin_issuer = "";
    private String idin_credential = "";
    private boolean age_limits_credential_enabled = true;
    private String age_limits_credential = "";


    private String initials_attribute = "";
    private String lastname_attribute = "";
    private String address_attribute = "";
    private String city_attribute = "";
    private String postalcode_attribute = "";
    private String birthdate_attribute = "";
    private String gender_attribute = "";
    private String email_attribute = "";
    private String tel_attribute = "";
    private String country_attribute = "";

    private String idin_issuers_path = "banks.json";
    private transient IdinIssuers issuers;

    private String jwt_privatekey = "sk.der";
    private String jwt_publickey = "pk.der";
    private transient PrivateKey jwtPrivateKey;
    private transient PublicKey jwtPublicKey;

    private static final String iDinLibConfigLocation = "config.xml";

    /**
     * Path to the file from which the iDIN issier list is saved and loaded
     */
    public Path getIdinIssuersPath() {
        return Paths.get(idin_issuers_path);
    }

    /**
     * Get the list of iDIN issuers (kept up to date by the {@link BackgroundJobManager})
     *
     * @return List of issuers, or null if the serialized issuer file
     * (see {@link #getIdinIssuersPath()}) could not be read
     */
    public IdinIssuers getIdinIssuers() {
        if (issuers == null) {
            try {
                final byte[] bytes = Files.readAllBytes(IdinConfiguration.getInstance().getIdinIssuersPath());
                issuers = GsonUtil.getGson().fromJson(new String(bytes), IdinIssuers.class);
            } catch (final Exception e) {
                return null;
            }
        }

        return issuers;
    }

    public static void loadIdinConfiguration() {
        getInstance();
        try {
            final URL config = getConfigurationDirectory().resolve(iDinLibConfigLocation).toURL();
            if (config == null) {
                throw new Exception("Could not load iDin configfile: " + iDinLibConfigLocation);
            }
            final InputStream configS = config.openStream();
            if (configS == null) {
                throw new Exception("Could not open iDin configfile: " + iDinLibConfigLocation);
            }
            Configuration.defaultInstance().Load(configS);
        } catch (final Exception e) {
            LOGGER.error("Could not load iDIN configuration");
            LOGGER.error(e.getMessage());
            throw new RuntimeException(e);
        }
    }

    /**
     * Reloads the configuration from disk so that {@link #getInstance()} returns the updated version
     */
    public static void load() {
        try {
            final String json = new String(getResource(FILENAME));
            instance = GsonUtil.getGson().fromJson(json, IdinConfiguration.class);
        } catch (final IOException | JsonSyntaxException e) {
            LOGGER.warn("Could not load configuration file, using default values!", e);
            instance = new IdinConfiguration();
        }

        System.out.println("Configuration:");
        System.out.println(instance.toString());
    }

    public IdinConfiguration() {
    }

    public static IdinConfiguration getInstance() {
        if (instance == null)
            load();

        return instance;
    }

    private static PublicKey parsePublicKey(final byte[] bytes) throws KeyManagementException {
        try {
            if (bytes == null || bytes.length == 0)
                throw new KeyManagementException("Could not read public key");

            final X509EncodedKeySpec spec = new X509EncodedKeySpec(bytes);

            return KeyFactory.getInstance("RSA").generatePublic(spec);
        } catch (final NoSuchAlgorithmException | InvalidKeySpecException e) {
            throw new KeyManagementException(e);
        }
    }

    public static PrivateKey parsePrivateKey(final byte[] bytes) throws KeyManagementException {
        try {
            if (bytes == null || bytes.length == 0)
                throw new KeyManagementException("Could not read private key");

            final PKCS8EncodedKeySpec spec = new PKCS8EncodedKeySpec(bytes);

            return KeyFactory.getInstance("RSA").generatePrivate(spec);
        } catch (final NoSuchAlgorithmException | InvalidKeySpecException e) {
            throw new KeyManagementException(e);
        }
    }

    public static byte[] convertStreamToByteArray(final InputStream stream, final int size) throws IOException {
        final byte[] buffer = new byte[size];
        final ByteArrayOutputStream os = new ByteArrayOutputStream();

        int line;
        while ((line = stream.read(buffer)) != -1) {
            os.write(buffer, 0, line);
        }
        stream.close();

        os.flush();
        os.close();
        return os.toByteArray();
    }

    public String getReturnUrl() {
        return return_url;
    }

    public boolean isHttpsEnabled() {
        return return_url.startsWith("https://");
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

    public String getEnrollUrl() {
        return enroll_url;
    }

    public String getEmailAttribute() {
        return email_attribute;
    }

    public String getTelephoneAttribute() {
        return tel_attribute;
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


    public String getServerName() {
        return server_name;
    }

    public String getHumanReadableName() {
        if (human_readable_name == null || human_readable_name.isEmpty())
            return server_name;
        else
            return human_readable_name;
    }

    public PublicKey getJwtPublicKey() {
        if (jwtPublicKey == null) {
            try {
                jwtPublicKey = parsePublicKey(getResource(jwt_publickey));
            } catch (final KeyManagementException | IOException e) {
                throw new RuntimeException(e);
            }
        }

        return jwtPublicKey;
    }

    public PrivateKey getJwtPrivateKey() {
        if (jwtPrivateKey == null) {
            try {
                jwtPrivateKey = parsePrivateKey(getResource(jwt_privatekey));
            } catch (final KeyManagementException | IOException e) {
                throw new RuntimeException(e);
            }
        }

        return jwtPrivateKey;
    }

    public SignatureAlgorithm getJwtAlgorithm() {
        return SignatureAlgorithm.RS256;
    }

    public String getAgeLimitsIssuer() {
        //currently the same issuer
        return idin_issuer;
    }

    public String getAgeLimitsCredential() {
        return age_limits_credential;
    }

    public boolean isAgeLimitsCredentialEnabled() {
        return age_limits_credential_enabled;
    }
}
