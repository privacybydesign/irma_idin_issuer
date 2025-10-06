package org.irmacard.idin.web;

import io.jsonwebtoken.SignatureAlgorithm;
import jakarta.ws.rs.core.NewCookie;
import jakarta.ws.rs.core.Response;
import net.bankid.merchant.library.*;
import net.bankid.merchant.library.internal.DirectoryResponseBase;
import org.irmacard.api.common.ApiClient;
import org.irmacard.credentials.info.CredentialIdentifier;
import org.junit.jupiter.api.Test;
import org.mockito.MockedConstruction;
import org.mockito.MockedStatic;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.net.URI;
import java.security.PrivateKey;
import java.util.*;

import static jakarta.ws.rs.core.HttpHeaders.LOCATION;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

public final class IdinResourceTest {

    public static final String BANK_CODE_VALID = "BANK_VALID";
    public static final String BANK_CODE_INVALID = "BANK_INVALID";
    public static final String ENROLL_URL = "https://example.org/enroll";
    public static final String RETURN_URL = "https://example.org/return";
    public static final String ERROR_PAGE = RETURN_URL + "/error.html";
    public static final String DISCLOSURE_JWT = "disc.jwt.token";
    public static final String ISSUER_AUTHENTICATION_URL = "https://bank.example.org/auth";
    public static final String ISSUER_AUTHENTICATION_INVALID_URL = "bad.url.com";
    public static final String TRANSACTION_ID = "trx-123";
    public static final String ENTRANCE_CODE = "ec-xyz";

    @Test
    public void banks() {
        final IdinIssuers idinIssuers = mock(IdinIssuers.class);
        final Map<String, List<DirectoryResponseBase.Issuer>> issuersMap = new HashMap<>();
        when(idinIssuers.getIssuers()).thenReturn(issuersMap);

        final IdinConfiguration idinConfiguration = mock(IdinConfiguration.class);
        when(idinConfiguration.getIdinIssuers()).thenReturn(idinIssuers);

        try (final MockedStatic<IdinConfiguration> idinConfigurationMockedStatic = mockStatic(IdinConfiguration.class)) {
            idinConfigurationMockedStatic.when(IdinConfiguration::getInstance).thenReturn(idinConfiguration);
            final IdinResource idinResource = new IdinResource();
            final Map<String, List<DirectoryResponseBase.Issuer>> result = idinResource.banks();
            assertSame(issuersMap, result);
        }
    }

    @Test
    public void banks_handlesEmptyIssuersMap() {
        final IdinIssuers idinIssuers = mock(IdinIssuers.class);
        when(idinIssuers.getIssuers()).thenReturn(Collections.emptyMap());

        final IdinConfiguration idinConfiguration = mock(IdinConfiguration.class);
        when(idinConfiguration.getIdinIssuers()).thenReturn(idinIssuers);

        try (final MockedStatic<IdinConfiguration> idinConfigurationMockedStatic = mockStatic(IdinConfiguration.class)) {
            idinConfigurationMockedStatic.when(IdinConfiguration::getInstance).thenReturn(idinConfiguration);
            final IdinResource idinResource = new IdinResource();
            final Map<String, List<DirectoryResponseBase.Issuer>> result = idinResource.banks();
            assertTrue(result.isEmpty());
        }
    }

    @Test
    public void getVerificationJWT_returnsDisclosureJwt() {
        final IdinConfiguration idinConfiguration = mock(IdinConfiguration.class);
        when(idinConfiguration.getServerName()).thenReturn("server");
        when(idinConfiguration.getHumanReadableName()).thenReturn("Server");
        when(idinConfiguration.getSchemeManager()).thenReturn("scheme");
        when(idinConfiguration.getIdinIssuer()).thenReturn("issuer");
        when(idinConfiguration.getIdinCredential()).thenReturn("cred");
        when(idinConfiguration.getBirthdateAttribute()).thenReturn("bd");

        final SignatureAlgorithm signatureAlgorithm = SignatureAlgorithm.RS256;
        final PrivateKey privateKey = mock(PrivateKey.class);
        when(idinConfiguration.getJwtAlgorithm()).thenReturn(signatureAlgorithm);
        when(idinConfiguration.getJwtPrivateKey()).thenReturn(privateKey);

        try (final MockedStatic<IdinConfiguration> idinConfigurationMockedStatic = mockStatic(IdinConfiguration.class);
             final MockedStatic<ApiClient> apiClientMockedStatic = mockStatic(ApiClient.class)) {
            idinConfigurationMockedStatic.when(IdinConfiguration::getInstance).thenReturn(idinConfiguration);
            apiClientMockedStatic.when(() -> ApiClient.getDisclosureJWT(any(), anyString(), anyString(), eq(signatureAlgorithm), eq(privateKey)))
                    .thenReturn(DISCLOSURE_JWT);

            final IdinResource idinResource = new IdinResource();
            final String jwt = idinResource.getVerificationJWT();
            assertEquals(DISCLOSURE_JWT, jwt);
        }
    }

    @Test
    public void openTransactions_delegatesToStatic() {
        try (final MockedStatic<OpenTransactions> openTransactionsMockedStatic = mockStatic(OpenTransactions.class)) {
            openTransactionsMockedStatic.when(OpenTransactions::getOpenTransactions).thenReturn("a, b, ");
            final IdinResource idinResource = new IdinResource();
            final String openTransactions = idinResource.openTransactions();
            assertEquals("a, b, ", openTransactions);
        }
    }

    @Test
    public void start_returns400WhenBankCodeInvalid() {
        final IdinIssuers idinIssuers = mock(IdinIssuers.class);
        when(idinIssuers.containsBankCode(BANK_CODE_INVALID)).thenReturn(false);

        final IdinConfiguration idinConfiguration = mock(IdinConfiguration.class);
        when(idinConfiguration.getIdinIssuers()).thenReturn(idinIssuers);

        try (final MockedStatic<IdinConfiguration> idinConfigurationMockedStatic = mockStatic(IdinConfiguration.class)) {
            idinConfigurationMockedStatic.when(IdinConfiguration::getInstance).thenReturn(idinConfiguration);
            final IdinResource idinResource = new IdinResource();
            final Response response = idinResource.start(BANK_CODE_INVALID);

            assertEquals(400, response.getStatus());
            assertInstanceOf(Map.class, response.getEntity());
            @SuppressWarnings("unchecked") final Map<String, Object> body = (Map<String, Object>) response.getEntity();
            assertEquals(400, body.get("status"));
            assertEquals("Bad Request", body.get("message"));
        }
    }

    @Test
    public void start_returnsOkJsonAndAddsTransactionOnSuccess() {
        final IdinIssuers idinIssuers = mock(IdinIssuers.class);
        when(idinIssuers.containsBankCode(BANK_CODE_VALID)).thenReturn(true);

        final IdinConfiguration idinConfiguration = mock(IdinConfiguration.class);
        when(idinConfiguration.getEnrollUrl()).thenReturn(ENROLL_URL);
        when(idinConfiguration.getReturnUrl()).thenReturn(RETURN_URL);
        when(idinConfiguration.getIdinIssuers()).thenReturn(idinIssuers);

        final AuthenticationResponse authenticationResponse = mock(AuthenticationResponse.class);
        when(authenticationResponse.getIsError()).thenReturn(false);
        when(authenticationResponse.getTransactionID()).thenReturn(TRANSACTION_ID);
        when(authenticationResponse.getIssuerAuthenticationURL()).thenReturn(ISSUER_AUTHENTICATION_URL);

        try (final MockedStatic<IdinConfiguration> idinConfigurationMockedStatic = mockStatic(IdinConfiguration.class);
             final MockedStatic<OpenTransactions> openTransactionsMockedStatic = mockStatic(OpenTransactions.class);
             final MockedConstruction<Communicator> ignored =
                     mockConstruction(Communicator.class, (communicator, context) ->
                             when(communicator.newAuthenticationRequest(any(AuthenticationRequest.class))).thenReturn(authenticationResponse))) {

            idinConfigurationMockedStatic.when(IdinConfiguration::getInstance).thenReturn(idinConfiguration);

            final IdinResource idinResource = new IdinResource();
            final Response response = idinResource.start(BANK_CODE_VALID);

            assertEquals(200, response.getStatus());
            assertInstanceOf(Map.class, response.getEntity());
            @SuppressWarnings("unchecked") final Map<String, Object> body = (Map<String, Object>) response.getEntity();
            assertEquals(ISSUER_AUTHENTICATION_URL, body.get("redirectUrl"));
            assertEquals(TRANSACTION_ID, body.get("trxid"));
            openTransactionsMockedStatic.verify(() -> OpenTransactions.addTransaction(any(IdinTransaction.class)));
        }
    }

    @Test
    public void start_returns504JsonWhenAuthenticationResponseIsError() {
        final IdinIssuers idinIssuers = mock(IdinIssuers.class);
        when(idinIssuers.containsBankCode(BANK_CODE_VALID)).thenReturn(true);

        final IdinConfiguration idinConfiguration = mock(IdinConfiguration.class);
        when(idinConfiguration.getIdinIssuers()).thenReturn(idinIssuers);

        final AuthenticationResponse authenticationResponse = mock(AuthenticationResponse.class);
        when(authenticationResponse.getIsError()).thenReturn(true);
        final ErrorResponse errorResponse = mock(ErrorResponse.class);
        when(authenticationResponse.getErrorResponse()).thenReturn(errorResponse);

        try (final MockedStatic<IdinConfiguration> idinConfigurationMockedStatic = mockStatic(IdinConfiguration.class);
             final MockedConstruction<Communicator> ignored =
                     mockConstruction(Communicator.class, (communicator, context) ->
                             when(communicator.newAuthenticationRequest(any(AuthenticationRequest.class))).thenReturn(authenticationResponse))) {

            idinConfigurationMockedStatic.when(IdinConfiguration::getInstance).thenReturn(idinConfiguration);

            final IdinResource idinResource = new IdinResource();
            final Response response = idinResource.start(BANK_CODE_VALID);

            assertEquals(504, response.getStatus());
            assertInstanceOf(Map.class, response.getEntity());
            @SuppressWarnings("unchecked") final Map<String, Object> body = (Map<String, Object>) response.getEntity();
            assertEquals(504, body.get("status"));
            assertEquals("Failure in system", body.get("message"));
        }
    }

    @Test
    public void start_inValidUrl_throwsIdinException() {
        final IdinIssuers idinIssuers = mock(IdinIssuers.class);
        when(idinIssuers.containsBankCode(BANK_CODE_VALID)).thenReturn(true);

        final IdinConfiguration idinConfiguration = mock(IdinConfiguration.class);
        when(idinConfiguration.getIdinIssuers()).thenReturn(idinIssuers);

        final AuthenticationResponse authenticationResponse = mock(AuthenticationResponse.class);
        when(authenticationResponse.getIsError()).thenReturn(false);
        when(authenticationResponse.getTransactionID()).thenReturn(TRANSACTION_ID);
        when(authenticationResponse.getIssuerAuthenticationURL()).thenReturn(ISSUER_AUTHENTICATION_INVALID_URL);

        try (final MockedStatic<IdinConfiguration> idinConfigurationMockedStatic = mockStatic(IdinConfiguration.class);
             final MockedConstruction<Communicator> ignored =
                     mockConstruction(Communicator.class, (communicator, context) -> when(communicator.newAuthenticationRequest(any(AuthenticationRequest.class))).thenReturn(authenticationResponse))) {

            idinConfigurationMockedStatic.when(IdinConfiguration::getInstance).thenReturn(idinConfiguration);

            final IdinResource idinResource = new IdinResource();
            final Response response = idinResource.start(BANK_CODE_VALID);
            assertEquals(502, response.getStatus());
            assertInstanceOf(Map.class, response.getEntity());
            @SuppressWarnings("unchecked") final Map<String, Object> body = (Map<String, Object>) response.getEntity();
            assertEquals(502, body.get("status"));
            assertEquals("Invalid issuerAuthenticationURL", body.get("message"));
            assertEquals("Ontvangen redirect-URL is ongeldig.", body.get("description"));
        }
    }

    @Test
    public void authenticated_returnsRedirectToErrorWhenNoTransactionId() {
        final IdinConfiguration idinConfiguration = mock(IdinConfiguration.class);
        when(idinConfiguration.isHttpsEnabled()).thenReturn(false);
        when(idinConfiguration.getReturnUrl()).thenReturn(RETURN_URL);

        try (final MockedStatic<IdinConfiguration> idinConfigurationMockedStatic = mockStatic(IdinConfiguration.class)) {
            idinConfigurationMockedStatic.when(IdinConfiguration::getInstance).thenReturn(idinConfiguration);
            final IdinResource idinResource = new IdinResource();
            final Response response = idinResource.authenticated("error", "ec");
            assertEquals(303, response.getStatus());
            assertEquals(URI.create(ERROR_PAGE), response.getHeaders().getFirst(LOCATION));
        }
    }

    @Test
    public void authenticated_returnsNullWhenTransactionNotFound() {
        final IdinConfiguration idinConfiguration = mock(IdinConfiguration.class);
        when(idinConfiguration.isHttpsEnabled()).thenReturn(false);
        when(idinConfiguration.getReturnUrl()).thenReturn(RETURN_URL);

        try (final MockedStatic<IdinConfiguration> idinConfigurationMockedStatic = mockStatic(IdinConfiguration.class);
             final MockedStatic<OpenTransactions> openTransactionsMockedStatic = mockStatic(OpenTransactions.class)) {
            idinConfigurationMockedStatic.when(IdinConfiguration::getInstance).thenReturn(idinConfiguration);
            openTransactionsMockedStatic.when(() -> OpenTransactions.findTransaction(TRANSACTION_ID)).thenReturn(null);

            final IdinResource idinResource = new IdinResource();
            final Response response = idinResource.authenticated(TRANSACTION_ID, ENTRANCE_CODE);
            assertNull(response);
        }
    }

    @Test
    public void authenticated_returnsNullWhenEntranceCodeMismatch() {
        final IdinConfiguration idinConfiguration = mock(IdinConfiguration.class);
        when(idinConfiguration.isHttpsEnabled()).thenReturn(false);
        when(idinConfiguration.getReturnUrl()).thenReturn(RETURN_URL);

        final IdinTransaction idinTransaction = mock(IdinTransaction.class);
        when(idinTransaction.getEntranceCode()).thenReturn("different");

        try (final MockedStatic<IdinConfiguration> idinConfigurationMockedStatic = mockStatic(IdinConfiguration.class);
             final MockedStatic<OpenTransactions> openTransactionsMockedStatic = mockStatic(OpenTransactions.class)) {
            idinConfigurationMockedStatic.when(IdinConfiguration::getInstance).thenReturn(idinConfiguration);
            openTransactionsMockedStatic.when(() -> OpenTransactions.findTransaction(TRANSACTION_ID)).thenReturn(idinTransaction);

            final IdinResource idinResource = new IdinResource();
            final Response response = idinResource.authenticated(TRANSACTION_ID, ENTRANCE_CODE);
            assertNull(response);
        }
    }

    @Test
    public void authenticated_throwsIdinExceptionOnErrorResponse() {
        final IdinConfiguration idinConfiguration = mock(IdinConfiguration.class);
        when(idinConfiguration.isHttpsEnabled()).thenReturn(false);
        when(idinConfiguration.getReturnUrl()).thenReturn(RETURN_URL);

        final IdinTransaction idinTransaction = mock(IdinTransaction.class);
        when(idinTransaction.getEntranceCode()).thenReturn(ENTRANCE_CODE);

        final StatusResponse statusResponse = mock(StatusResponse.class);
        when(statusResponse.getIsError()).thenReturn(true);
        final ErrorResponse errorResponse = mock(ErrorResponse.class);
        when(statusResponse.getErrorResponse()).thenReturn(errorResponse);

        try (final MockedStatic<IdinConfiguration> idinConfigurationMockedStatic = mockStatic(IdinConfiguration.class);
             final MockedStatic<OpenTransactions> openTransactionsMockedStatic = mockStatic(OpenTransactions.class);
             final MockedConstruction<Communicator> ignored =
                     mockConstruction(Communicator.class, (communicator, context) ->
                             when(communicator.getResponse(any(StatusRequest.class))).thenReturn(statusResponse))) {

            idinConfigurationMockedStatic.when(IdinConfiguration::getInstance).thenReturn(idinConfiguration);
            openTransactionsMockedStatic.when(() -> OpenTransactions.findTransaction(TRANSACTION_ID)).thenReturn(idinTransaction);

            final IdinResource idinResource = new IdinResource();
            assertThrows(IdinException.class, () -> idinResource.authenticated(TRANSACTION_ID, ENTRANCE_CODE));
        }
    }

    @Test
    public void authenticated_success_setsJwtCookie_and_redirectsToEnroll() {
        final IdinConfiguration idinConfiguration = mock(IdinConfiguration.class);
        when(idinConfiguration.isHttpsEnabled()).thenReturn(false);
        when(idinConfiguration.getEnrollUrl()).thenReturn(ENROLL_URL);
        when(idinConfiguration.getReturnUrl()).thenReturn(RETURN_URL);
        // attributes config used by createIssueJWT
        when(idinConfiguration.getInitialsAttribute()).thenReturn("initialsAttr");
        when(idinConfiguration.getLastnameAttribute()).thenReturn("lastnameAttr");
        when(idinConfiguration.getBirthdateAttribute()).thenReturn("birthdateAttr");
        when(idinConfiguration.getGenderAttribute()).thenReturn("genderAttr");
        when(idinConfiguration.getAddressAttribute()).thenReturn("addressAttr");
        when(idinConfiguration.getCityAttribute()).thenReturn("cityAttr");
        when(idinConfiguration.getPostalcodeAttribute()).thenReturn("postalAttr");
        when(idinConfiguration.getCountryAttribute()).thenReturn("countryAttr");
        when(idinConfiguration.getSchemeManager()).thenReturn("scheme");
        when(idinConfiguration.getIdinIssuer()).thenReturn("issuer");
        when(idinConfiguration.getIdinCredential()).thenReturn("cred");
        when(idinConfiguration.isAgeLimitsCredentialEnabled()).thenReturn(false);
        when(idinConfiguration.getServerName()).thenReturn("server");
        when(idinConfiguration.getHumanReadableName()).thenReturn("Server");
        final io.jsonwebtoken.SignatureAlgorithm sig = io.jsonwebtoken.SignatureAlgorithm.RS256;
        when(idinConfiguration.getJwtAlgorithm()).thenReturn(sig);
        final java.security.PrivateKey pk = mock(java.security.PrivateKey.class);
        when(idinConfiguration.getJwtPrivateKey()).thenReturn(pk);

        final IdinTransaction idinTransaction = mock(IdinTransaction.class);
        when(idinTransaction.getEntranceCode()).thenReturn(ENTRANCE_CODE);

        final StatusResponse statusResponse = mock(StatusResponse.class);
        when(statusResponse.getIsError()).thenReturn(false);
        when(statusResponse.getStatus()).thenReturn(StatusResponse.Success);
        final SamlResponse samlResponse = mock(SamlResponse.class);

        final Map<String, String> attributes = new HashMap<>();
        attributes.put("urn:nl:bvn:bankid:1.0:consumer.dateofbirth", "19800131");
        attributes.put("urn:nl:bvn:bankid:1.0:consumer.initials", "A.B.");
        attributes.put("urn:nl:bvn:bankid:1.0:consumer.legallastname", "Last");
        attributes.put("urn:nl:bvn:bankid:1.0:consumer.gender", "1");
        attributes.put("urn:nl:bvn:bankid:1.0:consumer.street", "Main");
        attributes.put("urn:nl:bvn:bankid:1.0:consumer.houseno", "12");
        attributes.put("urn:nl:bvn:bankid:1.0:consumer.postalcode", "1234AB");
        attributes.put("urn:nl:bvn:bankid:1.0:consumer.city", "City");
        attributes.put("urn:nl:bvn:bankid:1.0:consumer.country", "NL");

        when(samlResponse.getAttributes()).thenReturn(attributes);
        when(statusResponse.getSamlResponse()).thenReturn(samlResponse);

        try (final MockedStatic<IdinConfiguration> idinConfigurationMockedStatic = mockStatic(IdinConfiguration.class);
             final MockedStatic<ApiClient> apiClientMockedStatic = mockStatic(ApiClient.class);
             final MockedStatic<OpenTransactions> openTransactionsMockedStatic = mockStatic(OpenTransactions.class);
             final MockedConstruction<Communicator> ignored =
                     mockConstruction(Communicator.class, (communicator, context) ->
                             when(communicator.getResponse(any(StatusRequest.class))).thenReturn(statusResponse))) {

            idinConfigurationMockedStatic.when(IdinConfiguration::getInstance).thenReturn(idinConfiguration);
            openTransactionsMockedStatic.when(() -> OpenTransactions.findTransaction(TRANSACTION_ID)).thenReturn(idinTransaction);

            final org.irmacard.api.common.issuing.IdentityProviderRequest dummyIpRequest = mock(org.irmacard.api.common.issuing.IdentityProviderRequest.class);
            apiClientMockedStatic.when(() -> ApiClient.getIdentityProviderRequest(any(), anyLong())).thenReturn(dummyIpRequest);
            apiClientMockedStatic.when(() -> ApiClient.getSignedIssuingJWT(eq(dummyIpRequest), anyString(), anyString(), eq(sig), eq(pk)))
                    .thenReturn("created.jwt");

            final IdinResource idinResource = new IdinResource();
            final Response response = idinResource.authenticated(TRANSACTION_ID, ENTRANCE_CODE);

            assertNotNull(response);
            assertEquals(303, response.getStatus());
            assertEquals(URI.create(ENROLL_URL), response.getHeaders().getFirst(LOCATION));
            final Map<String, NewCookie> cookies = response.getCookies();
            assertTrue(cookies.containsKey("jwt"));
            assertEquals("created.jwt", cookies.get("jwt").getValue());

            verify(idinTransaction).handled();
            verify(idinTransaction).finished();

            apiClientMockedStatic.verify(() -> ApiClient.getSignedIssuingJWT(eq(dummyIpRequest), anyString(), anyString(), eq(sig), eq(pk)));
        }
    }

    @Test
    public void authenticated_success_missingAttributes_setsErrorCookie_and_redirectsToError() {
        final IdinConfiguration idinConfiguration = mock(IdinConfiguration.class);
        when(idinConfiguration.isHttpsEnabled()).thenReturn(false);
        when(idinConfiguration.getEnrollUrl()).thenReturn(ENROLL_URL);
        when(idinConfiguration.getReturnUrl()).thenReturn(RETURN_URL);

        final IdinTransaction idinTransaction = mock(IdinTransaction.class);
        when(idinTransaction.getEntranceCode()).thenReturn(ENTRANCE_CODE);

        final StatusResponse statusResponse = mock(StatusResponse.class);
        when(statusResponse.getIsError()).thenReturn(false);
        when(statusResponse.getStatus()).thenReturn(StatusResponse.Success);
        final SamlResponse samlResponse = mock(SamlResponse.class);

        final Map<String, String> attributes = new HashMap<>();
        attributes.put("urn:nl:bvn:bankid:1.0:consumer.dateofbirth", "19800131");
        attributes.put("urn:nl:bvn:bankid:1.0:consumer.initials", "A.B.");
        attributes.put("urn:nl:bvn:bankid:1.0:consumer.legallastname", "Last");
        attributes.put("urn:nl:bvn:bankid:1.0:consumer.gender", "1");
        attributes.put("urn:nl:bvn:bankid:1.0:consumer.street", "Main");
        attributes.put("urn:nl:bvn:bankid:1.0:consumer.houseno", "12");
        attributes.put("urn:nl:bvn:bankid:1.0:consumer.postalcode", "1234AB");
        // deliberately omit city to trigger nullOrEmptyAttributes -> error
        attributes.put("urn:nl:bvn:bankid:1.0:consumer.country", "NL");

        when(samlResponse.getAttributes()).thenReturn(attributes);
        when(statusResponse.getSamlResponse()).thenReturn(samlResponse);

        try (final MockedStatic<IdinConfiguration> idinConfigurationMockedStatic = mockStatic(IdinConfiguration.class);
             final MockedStatic<ApiClient> apiClientMockedStatic = mockStatic(ApiClient.class);
             final MockedStatic<OpenTransactions> openTransactionsMockedStatic = mockStatic(OpenTransactions.class);
             final MockedConstruction<Communicator> ignored =
                     mockConstruction(Communicator.class, (communicator, context) ->
                             when(communicator.getResponse(any(StatusRequest.class))).thenReturn(statusResponse))) {

            idinConfigurationMockedStatic.when(IdinConfiguration::getInstance).thenReturn(idinConfiguration);
            openTransactionsMockedStatic.when(() -> OpenTransactions.findTransaction(TRANSACTION_ID)).thenReturn(idinTransaction);

            final IdinResource idinResource = new IdinResource();
            final Response response = idinResource.authenticated(TRANSACTION_ID, ENTRANCE_CODE);

            assertNotNull(response);
            assertEquals(303, response.getStatus());
            assertEquals(URI.create(ERROR_PAGE), response.getHeaders().getFirst(LOCATION));
            final Map<String, NewCookie> cookies = response.getCookies();
            assertTrue(cookies.containsKey("error"));
            assertTrue(cookies.get("error").getValue().toLowerCase().contains("leverde niet voldoende"));

            verify(idinTransaction).handled();
            verify(idinTransaction).finished();

            // ensure no issuing JWT created
            apiClientMockedStatic.verify(() -> ApiClient.getSignedIssuingJWT((HashMap<CredentialIdentifier, HashMap<String, String>>) any(), anyString(), anyString(), any(), any()), times(0));
        }
    }

    @Test
    public void getGenderString_and_invalid() throws Exception {
        final IdinResource idinResource = new IdinResource();
        final Method getGenderString = IdinResource.class.getDeclaredMethod("getGenderString", String.class);
        getGenderString.setAccessible(true);

        assertEquals("unknown", getGenderString.invoke(idinResource, "0"));
        assertEquals("male", getGenderString.invoke(idinResource, "1"));
        assertEquals("female", getGenderString.invoke(idinResource, "2"));
        assertEquals("not applicable", getGenderString.invoke(idinResource, "9"));

        try {
            getGenderString.invoke(idinResource, "5");
            fail("Expected RuntimeException for unknown gender");
        } catch (final InvocationTargetException ite) {
            assertInstanceOf(RuntimeException.class, ite.getCause());
        }
    }

    @Test
    public void dobParsingAndFormatting() throws Exception {
        final IdinResource idinResource = new IdinResource();
        final Method getDobObject = IdinResource.class.getDeclaredMethod("getDobObject", String.class);
        final Method getDobString = IdinResource.class.getDeclaredMethod("getDobString", Date.class);
        getDobObject.setAccessible(true);
        getDobString.setAccessible(true);

        final Date dob = (Date) getDobObject.invoke(idinResource, "19800131");
        final String formatted = (String) getDobString.invoke(idinResource, dob);
        assertEquals("31-01-1980", formatted);
    }

    @Test
    public void ageAttributes_pastAndFuture() throws Exception {
        final IdinResource idinResource = new IdinResource();
        final Method ageAttributes = IdinResource.class.getDeclaredMethod("ageAttributes", int[].class, Date.class);
        ageAttributes.setAccessible(true);

        final int[] ages = new int[]{12, 16, 18, 21, 65};

        // very old birth date -> all should be "yes"
        final Method getDobObject = IdinResource.class.getDeclaredMethod("getDobObject", String.class);
        getDobObject.setAccessible(true);
        final Date veryOld = (Date) getDobObject.invoke(idinResource, "19000101");
        @SuppressWarnings("unchecked") final HashMap<String, String> attrsOld = (HashMap<String, String>) ageAttributes.invoke(idinResource, (Object) ages, veryOld);
        for (final int a : ages) {
            assertEquals("yes", attrsOld.get("over" + a));
        }

        // future birth date -> all should be "no"
        final Date future = (Date) getDobObject.invoke(idinResource, "21000101");
        @SuppressWarnings("unchecked") final HashMap<String, String> attrsFuture = (HashMap<String, String>) ageAttributes.invoke(idinResource, (Object) ages, future);
        for (final int a : ages) {
            assertEquals("no", attrsFuture.get("over" + a));
        }
    }

    @Test
    public void isNullOrEmpty_and_nullOrEmptyAttributes() throws Exception {
        final IdinResource idinResource = new IdinResource();
        final Method isNullOrEmpty = IdinResource.class.getDeclaredMethod("isNullOrEmpty", String.class);
        final Method nullOrEmptyAttributes = IdinResource.class.getDeclaredMethod("nullOrEmptyAttributes", Map.class, String.class);
        isNullOrEmpty.setAccessible(true);
        nullOrEmptyAttributes.setAccessible(true);

        assertTrue((Boolean) isNullOrEmpty.invoke(idinResource, (Object) null));
        assertTrue((Boolean) isNullOrEmpty.invoke(idinResource, ""));
        assertFalse((Boolean) isNullOrEmpty.invoke(idinResource, "x"));

        // build a complete attributes map
        final Map<String, String> good = new HashMap<>();
        good.put("urn:nl:bvn:bankid:1.0:consumer.dateofbirth", "19800101");
        good.put("urn:nl:bvn:bankid:1.0:consumer.initials", "A.B.");
        good.put("urn:nl:bvn:bankid:1.0:consumer.legallastname", "Last");
        good.put("urn:nl:bvn:bankid:1.0:consumer.gender", "1");
        good.put("urn:nl:bvn:bankid:1.0:consumer.street", "Main");
        good.put("urn:nl:bvn:bankid:1.0:consumer.houseno", "12");
        good.put("urn:nl:bvn:bankid:1.0:consumer.city", "City");
        good.put("urn:nl:bvn:bankid:1.0:consumer.postalcode", "1234AB");
        good.put("urn:nl:bvn:bankid:1.0:consumer.country", "NL");
        // BIN is optional

        assertFalse((Boolean) nullOrEmptyAttributes.invoke(idinResource, good, "trx"));

        // missing a required attribute -> should be true
        final Map<String, String> bad = new HashMap<>(good);
        bad.remove("urn:nl:bvn:bankid:1.0:consumer.city");
        assertTrue((Boolean) nullOrEmptyAttributes.invoke(idinResource, bad, "trx"));
    }

    @Test
    public void credId_validation() throws Exception {
        final Method credId = IdinResource.class.getDeclaredMethod("credId", String.class, String.class, String.class);
        credId.setAccessible(true);

        try {
            credId.invoke(null, "", "a", "b");
            fail("Expected IllegalStateException for blank scheme");
        } catch (final InvocationTargetException ite) {
            assertInstanceOf(IllegalStateException.class, ite.getCause());
        }

        final Object cid = credId.invoke(null, "scheme", "issuer", "cred");
        assertNotNull(cid);
        assertEquals("org.irmacard.credentials.info.CredentialIdentifier", cid.getClass().getName());
    }

    @Test
    public void createIssueJWT_returnsSignedJwt() throws Exception {
        final IdinConfiguration idinConfiguration = mock(IdinConfiguration.class);
        when(idinConfiguration.getInitialsAttribute()).thenReturn("initialsAttr");
        when(idinConfiguration.getLastnameAttribute()).thenReturn("lastnameAttr");
        when(idinConfiguration.getBirthdateAttribute()).thenReturn("birthdateAttr");
        when(idinConfiguration.getGenderAttribute()).thenReturn("genderAttr");
        when(idinConfiguration.getAddressAttribute()).thenReturn("addressAttr");
        when(idinConfiguration.getCityAttribute()).thenReturn("cityAttr");
        when(idinConfiguration.getPostalcodeAttribute()).thenReturn("postalAttr");
        when(idinConfiguration.getCountryAttribute()).thenReturn("countryAttr");
        when(idinConfiguration.getSchemeManager()).thenReturn("scheme");
        when(idinConfiguration.getIdinIssuer()).thenReturn("issuer");
        when(idinConfiguration.getIdinCredential()).thenReturn("cred");
        when(idinConfiguration.isAgeLimitsCredentialEnabled()).thenReturn(false);
        when(idinConfiguration.getServerName()).thenReturn("server");
        when(idinConfiguration.getHumanReadableName()).thenReturn("Server");
        final io.jsonwebtoken.SignatureAlgorithm sig = io.jsonwebtoken.SignatureAlgorithm.RS256;
        when(idinConfiguration.getJwtAlgorithm()).thenReturn(sig);
        final java.security.PrivateKey pk = mock(java.security.PrivateKey.class);
        when(idinConfiguration.getJwtPrivateKey()).thenReturn(pk);

        // prepare attributes map expected by createIssueJWT
        final Map<String, String> attributes = new HashMap<>();
        attributes.put("urn:nl:bvn:bankid:1.0:consumer.dateofbirth", "19800131");
        attributes.put("urn:nl:bvn:bankid:1.0:consumer.initials", "A.B.");
        attributes.put("urn:nl:bvn:bankid:1.0:consumer.legallastname", "Last");
        attributes.put("urn:nl:bvn:bankid:1.0:consumer.gender", "1");
        attributes.put("urn:nl:bvn:bankid:1.0:consumer.street", "Main");
        attributes.put("urn:nl:bvn:bankid:1.0:consumer.houseno", "12");
        attributes.put("urn:nl:bvn:bankid:1.0:consumer.postalcode", "1234AB");
        attributes.put("urn:nl:bvn:bankid:1.0:consumer.city", "City");
        attributes.put("urn:nl:bvn:bankid:1.0:consumer.country", "NL");

        try (final MockedStatic<IdinConfiguration> idinConfigurationMockedStatic = mockStatic(IdinConfiguration.class);
             final MockedStatic<ApiClient> apiClientMockedStatic = mockStatic(ApiClient.class)) {
            idinConfigurationMockedStatic.when(IdinConfiguration::getInstance).thenReturn(idinConfiguration);

            final org.irmacard.api.common.issuing.IdentityProviderRequest dummyIpRequest = mock(org.irmacard.api.common.issuing.IdentityProviderRequest.class);
            apiClientMockedStatic.when(() -> ApiClient.getIdentityProviderRequest(any(), anyLong())).thenReturn(dummyIpRequest);

            apiClientMockedStatic.when(() -> ApiClient.getSignedIssuingJWT(eq(dummyIpRequest), anyString(), anyString(), eq(sig), eq(pk)))
                    .thenReturn("signed.jwt.token");

            final IdinResource idinResource = new IdinResource();

            final Method createIssueJWT = IdinResource.class.getDeclaredMethod("createIssueJWT", Map.class);
            createIssueJWT.setAccessible(true);
            final String jwt = (String) createIssueJWT.invoke(idinResource, attributes);
            assertEquals("signed.jwt.token", jwt);
        }
    }

}
