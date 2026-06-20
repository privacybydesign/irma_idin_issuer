package org.irmacard.idin.web;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.URL;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.security.KeyFactory;
import java.security.KeyManagementException;
import java.security.NoSuchAlgorithmException;
import java.security.PrivateKey;
import java.security.PublicKey;
import java.security.spec.InvalidKeySpecException;
import java.security.spec.PKCS8EncodedKeySpec;
import java.security.spec.X509EncodedKeySpec;

import org.irmacard.api.common.util.GsonUtil;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.gson.JsonSyntaxException;

import foundation.privacybydesign.common.BaseConfiguration;
import io.jsonwebtoken.SignatureAlgorithm;
import net.bankid.merchant.library.Configuration;

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

    // Optional Redis configuration for persistent transaction storage. When no Redis host (and no
    // Redis Sentinel) is configured, the issuer falls back to the historic in-memory store.
    private String redis_host = "";
    private int redis_port = 6379;
    private String redis_password = "";
    // Safety-net TTL (seconds) on stored transactions; defaults to 7 days. Transactions are normally
    // removed explicitly once resolved, so this only guards against abandoned ones piling up.
    private long redis_transaction_ttl_seconds = 60L * 60L * 24L * 7L;

    private boolean redis_sentinel_enabled = false;
    private String redis_sentinel_host = "";
    private int redis_sentinel_port = 26379;
    private String redis_master_name = "mymaster";
    private String redis_sentinel_username = "";

    private String jwt_privatekey = "sk.der";
    private String jwt_publickey = "pk.der";
    private transient PrivateKey jwtPrivateKey;
    private transient PublicKey jwtPublicKey;

    private static final String IDIN_LIB_CONFIG_LOCATION = "config.xml";

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
            final URL config = getConfigurationDirectory().resolve(IDIN_LIB_CONFIG_LOCATION).toURL();
            if (config == null) {
                throw new Exception("Could not load iDin configfile: " + IDIN_LIB_CONFIG_LOCATION);
            }
            final InputStream configS = config.openStream();
            if (configS == null) {
                throw new Exception("Could not open iDin configfile: " + IDIN_LIB_CONFIG_LOCATION);
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

    /**
     * @return whether a persistent Redis-backed transaction store should be used. True when either a
     * plain Redis host or a Redis Sentinel setup is configured.
     */
    public boolean isRedisEnabled() {
        return isRedisSentinelEnabled() || (redis_host != null && !redis_host.isBlank());
    }

    /**
     * @return whether Redis Sentinel should be used (takes precedence over a plain Redis host).
     */
    public boolean isRedisSentinelEnabled() {
        return redis_sentinel_enabled && redis_sentinel_host != null && !redis_sentinel_host.isBlank();
    }

    public String getRedisHost() {
        return redis_host;
    }

    public int getRedisPort() {
        return redis_port;
    }

    public String getRedisPassword() {
        return redis_password;
    }

    public long getRedisTransactionTtlSeconds() {
        return redis_transaction_ttl_seconds;
    }

    public String getRedisSentinelHost() {
        return redis_sentinel_host;
    }

    public int getRedisSentinelPort() {
        return redis_sentinel_port;
    }

    public String getRedisMasterName() {
        return redis_master_name;
    }

    public String getRedisSentinelUsername() {
        return redis_sentinel_username;
    }
}
