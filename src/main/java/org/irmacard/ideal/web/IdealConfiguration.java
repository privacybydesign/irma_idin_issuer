package org.irmacard.ideal.web;

import com.google.gson.JsonSyntaxException;
import org.irmacard.api.common.util.GsonUtil;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.security.KeyManagementException;
import java.security.PublicKey;

public class IdealConfiguration extends Configuration{
    private static final String filename = "iDEAL-config.json";
    private static Logger logger = LoggerFactory.getLogger(IdealConfiguration.class);
    private static IdealConfiguration instance;

    private String scheme_manager = "";
    private String ideal_issuer = "";
    private String iban_credential = "";
    private String api_server_public_key = "";

    private String ideal_issuers_path = "ideal-banks.json";
    private transient IdealIssuers iDealIssuers;
    private transient PublicKey apiServerPublickKey;

    public static IdealConfiguration getInstance() {
        if (instance == null) {
            load();
        }
        return instance;
    }

    /**
     * Reloads the configuration from disk so that {@link IdealConfiguration#getInstance()} returns the updated version
     */
    public static void load() {
        try {
            String json = new String(getResource(filename));
            instance = GsonUtil.getGson().fromJson(json, IdealConfiguration.class);
        } catch (IOException |JsonSyntaxException e) {
            logger.warn("Could not load configuration file, using default values!");
            e.printStackTrace();
            instance = new IdealConfiguration();
        }
    }


    /** Path to the file from which the iDIN issuer list is saved and loaded */
    public Path getIdealIssuersPath() {
        return Paths.get(ideal_issuers_path);
    }

    /**
     * Get the list of iDEAL issuers (kept up to date by the {@link BackgroundJobManager})
     * @return List of iDEAL issuers, or null if the serialized issuer file
     *         (see {@link #getIdealIssuersPath()}) could not be read
     */
    public IdealIssuers getIdealIssuers() {
        if (iDealIssuers == null) {
            try {
                byte[] bytes = Files.readAllBytes(IdealConfiguration.getInstance().getIdealIssuersPath());
                iDealIssuers = GsonUtil.getGson().fromJson(new String(bytes), IdealIssuers.class);
            } catch (Exception e) {
                return null;
            }
        }

        return iDealIssuers;
    }

    public String getSchemeManager() {
        return scheme_manager;
    }

    public String getIdealIssuer() {
        return ideal_issuer;
    }

    public String getIbanCredential() {
        return iban_credential;
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
}
