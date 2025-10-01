package org.irmacard.idin.web;

import io.jsonwebtoken.SignatureAlgorithm;
import jakarta.ws.rs.core.Response;
import net.bankid.merchant.library.*;
import net.bankid.merchant.library.internal.DirectoryResponseBase;
import org.irmacard.api.common.ApiClient;
import org.junit.jupiter.api.Test;
import org.mockito.MockedConstruction;
import org.mockito.MockedStatic;

import java.net.URI;
import java.security.PrivateKey;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

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
    public static final String TRANSACTION_ID = "trx-123";
    public static final String ENTRANCE_CODE = "ec-xyz";

    @Test
    public void banks() throws Exception {
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
            @SuppressWarnings("unchecked")
            final Map<String, Object> body = (Map<String, Object>) response.getEntity();
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
            @SuppressWarnings("unchecked")
            final Map<String, Object> body = (Map<String, Object>) response.getEntity();
            assertEquals(ISSUER_AUTHENTICATION_URL, body.get("redirectUrl"));
            assertEquals(TRANSACTION_ID, body.get("trxid"));
            openTransactionsMockedStatic.verify(() -> OpenTransactions.addTransaction(any(IdinTransaction.class)));
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
            assertTrue(response.getEntity() instanceof Map);
            @SuppressWarnings("unchecked")
            final Map<String, Object> body = (Map<String, Object>) response.getEntity();
            assertEquals(504, body.get("status"));
            assertEquals("Failure in system", body.get("message"));
        }
    }

    @Test
    public void banks_handlesEmptyIssuersMap() throws Exception {
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
}
