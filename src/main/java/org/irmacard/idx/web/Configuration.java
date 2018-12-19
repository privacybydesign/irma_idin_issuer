package org.irmacard.idx.web;

import io.jsonwebtoken.SignatureAlgorithm;
import org.irmacard.api.common.util.GsonUtil;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.URL;
import java.net.URLConnection;
import java.security.*;
import java.security.spec.InvalidKeySpecException;
import java.security.spec.PKCS8EncodedKeySpec;
import java.security.spec.X509EncodedKeySpec;

/**
 * Helper class for common methods and attributes between iDIN en iDEAL configurations
 */

public abstract class Configuration {
    protected String server_name = "IRMA_bank_test";
    protected String jwt_privatekey = "sk.der";
    protected String jwt_publickey = "pk.der";
    protected transient PrivateKey jwtPrivateKey;
    protected transient PublicKey jwtPublicKey;
    private String human_readable_name = "";

    private String return_url = "";

    protected static PublicKey parsePublicKey(byte[] bytes) throws KeyManagementException {
        try {
            if (bytes == null || bytes.length == 0)
                throw new KeyManagementException("Could not read public key");

            X509EncodedKeySpec spec = new X509EncodedKeySpec(bytes);

            return KeyFactory.getInstance("RSA").generatePublic(spec);
        } catch (NoSuchAlgorithmException |InvalidKeySpecException e) {
            throw new KeyManagementException(e);
        }
    }

    public static PrivateKey parsePrivateKey(byte[] bytes) throws KeyManagementException {
        try {
            if (bytes == null || bytes.length == 0)
                throw new KeyManagementException("Could not read private key");

            PKCS8EncodedKeySpec spec = new PKCS8EncodedKeySpec(bytes);

            return KeyFactory.getInstance("RSA").generatePrivate(spec);
        } catch (NoSuchAlgorithmException|InvalidKeySpecException e) {
            throw new KeyManagementException(e);
        }
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


    public String getReturnUrl() {
        return return_url;
    }

    public boolean isHttpsEnabled() {
        return return_url.startsWith("https://");
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

    public PublicKey getJwtPublicKey() {
        if (jwtPublicKey == null) {
            try {
                jwtPublicKey = parsePublicKey(getResource(jwt_publickey));
            } catch (KeyManagementException|IOException e) {
                throw new RuntimeException(e);
            }
        }

        return jwtPublicKey;
    }

    public PrivateKey getJwtPrivateKey() {
        if (jwtPrivateKey == null) {
            try {
                jwtPrivateKey = parsePrivateKey(getResource(jwt_privatekey));
            } catch (KeyManagementException|IOException e) {
                throw new RuntimeException(e);
            }
        }

        return jwtPrivateKey;
    }

    public SignatureAlgorithm getJwtAlgorithm() {
        return SignatureAlgorithm.RS256;
    }


    @Override
    public String toString() {
        return GsonUtil.getGson().toJson(this);
    }
}
