package org.irmacard.idin.web;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;

import java.io.ByteArrayInputStream;
import java.io.InputStream;
import java.lang.reflect.Field;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.nio.file.Paths;
import java.security.*;

import static org.junit.jupiter.api.Assertions.*;

public class IdinConfigurationTest {

    public static final String SERVER_NAME = "MY_SERVER";
    public static final String RETURN_URL = "https://example.org/return";
    public static final String HUMAN_READABLE_SERVER_NAME = "My Server";
    public static final String SCHEME_MANAGER = "MySchemeManager";
    public static final String ISSUER = "MyIssuer";
    public static final String IDIN_CREDENTIAL = "MyCredential";
    public static final String AGE_LIMITS_CREDENTIAL = "MyAgeLimitsCredential";
    public static final String INITIALS = "initials";
    public static final String LASTNAME = "lastname";
    public static final String ADDRESS = "address";
    public static final String CITY = "city";
    public static final String POSTALCODE = "postalcode";
    public static final String BIRTHDATE = "birthdate";
    public static final String ENROLL_URL = "https://example.org/enroll";
    public static final String EMAIL = "email";
    public static final String TEL = "tel";
    public static final String COUNTRY = "country";
    public static final String GENDER = "male";

    @AfterEach
    public void clearInstance() throws Exception {
        final Field instanceField = IdinConfiguration.class.getDeclaredField("instance");
        instanceField.setAccessible(true);
        instanceField.set(null, null);
    }

    @Test
    public void testGettersReturnConfiguredValues() throws Exception {
        final IdinConfiguration idinConfiguration = new IdinConfiguration();
        setField(idinConfiguration, "enroll_url", ENROLL_URL);
        setField(idinConfiguration, "server_name", SERVER_NAME);
        setField(idinConfiguration, "human_readable_name", HUMAN_READABLE_SERVER_NAME);
        setField(idinConfiguration, "return_url", RETURN_URL);
        setField(idinConfiguration, "scheme_manager", SCHEME_MANAGER);
        setField(idinConfiguration, "idin_issuer", ISSUER);
        setField(idinConfiguration, "idin_credential", IDIN_CREDENTIAL);
        setField(idinConfiguration, "age_limits_credential_enabled", true);
        setField(idinConfiguration, "age_limits_credential", AGE_LIMITS_CREDENTIAL);
        setField(idinConfiguration, "initials_attribute", INITIALS);
        setField(idinConfiguration, "lastname_attribute", LASTNAME);
        setField(idinConfiguration, "address_attribute", ADDRESS);
        setField(idinConfiguration, "city_attribute", CITY);
        setField(idinConfiguration, "postalcode_attribute", POSTALCODE);
        setField(idinConfiguration, "birthdate_attribute", BIRTHDATE);
        setField(idinConfiguration, "email_attribute", EMAIL);
        setField(idinConfiguration, "tel_attribute", TEL);
        setField(idinConfiguration, "country_attribute", COUNTRY);
        setField(idinConfiguration, "gender_attribute", GENDER);

        assertEquals(ENROLL_URL, idinConfiguration.getEnrollUrl());
        assertEquals(SERVER_NAME, idinConfiguration.getServerName());
        assertEquals(HUMAN_READABLE_SERVER_NAME, idinConfiguration.getHumanReadableName());
        assertEquals(RETURN_URL, idinConfiguration.getReturnUrl());
        assertEquals(SCHEME_MANAGER, idinConfiguration.getSchemeManager());
        assertEquals(ISSUER, idinConfiguration.getIdinIssuer());
        assertEquals(IDIN_CREDENTIAL, idinConfiguration.getIdinCredential());
        assertTrue(idinConfiguration.isAgeLimitsCredentialEnabled());
        assertEquals(AGE_LIMITS_CREDENTIAL, idinConfiguration.getAgeLimitsCredential());
        assertEquals(INITIALS, idinConfiguration.getInitialsAttribute());
        assertEquals(LASTNAME, idinConfiguration.getLastnameAttribute());
        assertEquals(ADDRESS, idinConfiguration.getAddressAttribute());
        assertEquals(CITY, idinConfiguration.getCityAttribute());
        assertEquals(POSTALCODE, idinConfiguration.getPostalcodeAttribute());
        assertEquals(BIRTHDATE, idinConfiguration.getBirthdateAttribute());
        assertEquals(EMAIL, idinConfiguration.getEmailAttribute());
        assertEquals(TEL, idinConfiguration.getTelephoneAttribute());
        assertEquals(COUNTRY, idinConfiguration.getCountryAttribute());
        assertEquals(GENDER, idinConfiguration.getGenderAttribute());
    }

    @Test
    public void getHumanReadableName_returnsServerName_whenEmpty() throws Exception {
        final IdinConfiguration idinConfiguration = new IdinConfiguration();
        setField(idinConfiguration, "server_name", SERVER_NAME);
        setField(idinConfiguration, "human_readable_name", "");
        setInstance(idinConfiguration);

        final IdinConfiguration inst = IdinConfiguration.getInstance();
        assertEquals(SERVER_NAME, inst.getHumanReadableName());
    }

    @Test
    public void isHttpsEnabled_detectsHttpsPrefix() throws Exception {
        final IdinConfiguration idinConfiguration = new IdinConfiguration();
        setField(idinConfiguration, "return_url", RETURN_URL);
        setInstance(idinConfiguration);

        assertTrue(IdinConfiguration.getInstance().isHttpsEnabled());

        setField(idinConfiguration, "return_url", "http://example.org/return");
        assertFalse(IdinConfiguration.getInstance().isHttpsEnabled());
    }

    @Test
    public void getIdinIssuersPath_resolvesConfiguredPath() throws Exception {
        final IdinConfiguration idinConfiguration = new IdinConfiguration();
        setField(idinConfiguration, "idin_issuers_path", "some/path/banks.json");
        setInstance(idinConfiguration);

        assertEquals(Paths.get("some/path/banks.json"), IdinConfiguration.getInstance().getIdinIssuersPath());
    }

    @Test
    public void getIdinIssuers_returnsNull_whenFileMissing() throws Exception {
        final IdinConfiguration idinConfiguration = new IdinConfiguration();
        // point to a non-existent file
        setField(idinConfiguration, "idin_issuers_path", "this-file-does-not-exist-hopefully.json");
        setInstance(idinConfiguration);

        assertNull(IdinConfiguration.getInstance().getIdinIssuers());
    }

    @Test
    public void convertStreamToByteArray_readsAllBytes_inChunks() throws Exception {
        final byte[] data = "abcdefghijklmnopqrstuvwxyz".getBytes();
        try (final InputStream in = new ByteArrayInputStream(data)) {
            final byte[] out = IdinConfiguration.convertStreamToByteArray(in, 3); // small buffer to force multiple reads
            assertArrayEquals(data, out);
        }
    }

    @Test
    public void parsePrivateKey_success_and_failure() throws Exception {
        // generate an RSA keypair and test parsePrivateKey with its PKCS#8 encoding
        final KeyPairGenerator keyPairGenerator = KeyPairGenerator.getInstance("RSA");
        keyPairGenerator.initialize(2048);
        final KeyPair keyPair = keyPairGenerator.generateKeyPair();
        final byte[] encoded = keyPair.getPrivate().getEncoded();

        final PrivateKey parsed = IdinConfiguration.parsePrivateKey(encoded);
        assertNotNull(parsed);
        assertEquals("RSA", parsed.getAlgorithm());

        // invalid input should throw KeyManagementException
        assertThrows(java.security.KeyManagementException.class, () -> IdinConfiguration.parsePrivateKey(new byte[0]));
    }

    @Test
    public void parsePublicKey_viaReflection_success_and_failure() throws Exception {
        // generate an RSA keypair and test the private parsePublicKey via reflection
        final KeyPairGenerator keyPairGenerator = KeyPairGenerator.getInstance("RSA");
        keyPairGenerator.initialize(2048);
        final KeyPair keyPair = keyPairGenerator.generateKeyPair();
        final byte[] x509 = keyPair.getPublic().getEncoded();

        final Method parsePublic = IdinConfiguration.class.getDeclaredMethod("parsePublicKey", byte[].class);
        parsePublic.setAccessible(true);

        final Object result = parsePublic.invoke(null, (Object) x509);
        assertInstanceOf(PublicKey.class, result);
        final PublicKey pub = (PublicKey) result;
        assertEquals("RSA", pub.getAlgorithm());

        // empty bytes should cause KeyManagementException wrapped in InvocationTargetException
        try {
            parsePublic.invoke(null, (Object) new byte[0]);
            fail("Expected KeyManagementException");
        } catch (final InvocationTargetException ite) {
            final Throwable cause = ite.getCause();
            assertNotNull(cause);
            assertInstanceOf(KeyManagementException.class, cause);
        }
    }

    @Test
    public void getJwtAlgorithm_isRS256() {
        final IdinConfiguration idinConfiguration = new IdinConfiguration();
        // no need to set instance, method is instance-level but deterministic
        assertEquals(io.jsonwebtoken.SignatureAlgorithm.RS256, idinConfiguration.getJwtAlgorithm());
    }

    private static void setInstance(final IdinConfiguration idinConfiguration) throws Exception {
        final Field instanceField = IdinConfiguration.class.getDeclaredField("instance");
        instanceField.setAccessible(true);
        instanceField.set(null, idinConfiguration);
    }

    private static void setField(final Object target, final String fieldName, final Object value) throws Exception {
        final Field field = target.getClass().getDeclaredField(fieldName);
        field.setAccessible(true);
        field.set(target, value);
    }
}