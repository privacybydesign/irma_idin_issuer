package org.irmacard.idin.web;

import jakarta.ws.rs.*;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.NewCookie;
import jakarta.ws.rs.core.Response;
import net.bankid.merchant.library.*;
import net.bankid.merchant.library.internal.DirectoryResponseBase;
import org.irmacard.api.common.ApiClient;
import org.irmacard.api.common.AttributeDisjunction;
import org.irmacard.api.common.AttributeDisjunctionList;
import org.irmacard.api.common.issuing.IdentityProviderRequest;
import org.irmacard.credentials.info.AttributeIdentifier;
import org.irmacard.credentials.info.CredentialIdentifier;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.math.BigInteger;
import java.net.URI;
import java.net.URISyntaxException;
import java.security.SecureRandom;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.*;

@Path("v1/idin")
public class IdinResource {
    public final static int IDIN_ATTRIBUTES = ServiceIds.Address
            | ServiceIds.ConsumerBin
            | ServiceIds.DateOfBirth
            | ServiceIds.Gender
            | ServiceIds.Name;

    private static final String IDIN_SAML_BIN_KEY = "urn:nl:bvn:bankid:1.0:consumer.bin";
    private static final String IDIN_SAML_CITY_KEY = "urn:nl:bvn:bankid:1.0:consumer.city";
    private static final String IDIN_SAML_LASTNAME_PREFIX_KEY = "urn:nl:bvn:bankid:1.0:consumer.legallastnameprefix";
    private static final String IDIN_SAML_LAST_NAME_KEY = "urn:nl:bvn:bankid:1.0:consumer.legallastname";
    private static final String IDIN_SAML_INITIALS_KEY = "urn:nl:bvn:bankid:1.0:consumer.initials";
    private static final String IDIN_SAML_BIRTHDATE_KEY = "urn:nl:bvn:bankid:1.0:consumer.dateofbirth";
    private static final String IDIN_SAML_COUNTRY_KEY = "urn:nl:bvn:bankid:1.0:consumer.country";
    private static final String IDIN_SAML_GENDER_KEY = "urn:nl:bvn:bankid:1.0:consumer.gender";
    private static final String IDIN_SAML_STREET_KEY = "urn:nl:bvn:bankid:1.0:consumer.street";
    private static final String IDIN_SAML_HOUSE_NO_KEY = "urn:nl:bvn:bankid:1.0:consumer.houseno";
    private static final String IDIN_SAML_HOUSE_NO_SUF_KEY = "urn:nl:bvn:bankid:1.0:consumer.housenosuf";
    private static final String IDIN_SAML_ADDRESS_EXTRA_KEY = "urn:nl:bvn:bankid:1.0:consumer.addressextra";
    private static final String IDIN_SAML_POSTAL_CODE_KEY = "urn:nl:bvn:bankid:1.0:consumer.postalcode";


    private static final Random RANDOM = new Random();
    private static final SecureRandom SECURE_RANDOM = new SecureRandom();
    private static final Logger LOGGER = LoggerFactory.getLogger(IdinResource.class);

    private static final String SUCCESS_URL = IdinConfiguration.getInstance().getEnrollUrl();
    private static final String ERROR_URL = IdinConfiguration.getInstance().getReturnUrl() + "/error.html";

    @GET
    @Path("/banks")
    @Produces(MediaType.APPLICATION_JSON)
    public Map<String, List<DirectoryResponseBase.Issuer>> banks() throws IOException {
        LOGGER.info("Bank list requested");
        return IdinConfiguration.getInstance().getIdinIssuers().getIssuers();
    }

    @GET
    @Path("/verify")
    @Produces(MediaType.TEXT_PLAIN)
    public String getVerificationJWT() {
        final AttributeDisjunctionList list = new AttributeDisjunctionList(1);
        list.add(new AttributeDisjunction("Geboortedatum", getIdinBdAttributeIdentifier()));
        return ApiClient.getDisclosureJWT(
                list,
                IdinConfiguration.getInstance().getServerName(),
                IdinConfiguration.getInstance().getHumanReadableName(),
                IdinConfiguration.getInstance().getJwtAlgorithm(),
                IdinConfiguration.getInstance().getJwtPrivateKey()
        );
    }

    @GET
    @Path("/openTransactions")
    @Produces(MediaType.TEXT_PLAIN)
    public String openTransactions() {
        return OpenTransactions.getOpenTransactions();
    }

    private AttributeIdentifier getIdinBdAttributeIdentifier() {
        final IdinConfiguration conf = IdinConfiguration.getInstance();
        return new AttributeIdentifier(
                new CredentialIdentifier(
                        conf.getSchemeManager(),
                        conf.getIdinIssuer(),
                        conf.getIdinCredential()),
                conf.getBirthdateAttribute()
        );
    }

    @POST
    @Path("/start")
    @Consumes(MediaType.APPLICATION_FORM_URLENCODED)
    public Response start(final String bank) {
        if (!IdinConfiguration.getInstance().getIdinIssuers().containsBankCode(bank)) {
            throw new RuntimeException("Illegal bankcode received");
        }
        // Create request
        String merchantReference = new BigInteger(130, RANDOM).toString(32);
        //iDIN lib wants the random MerchantReference to start with a letter.
        merchantReference = "a" + merchantReference;

        final String entranceCode = new RandomString(40, SECURE_RANDOM).nextString();
        final AuthenticationRequest request = new AuthenticationRequest(entranceCode, IDIN_ATTRIBUTES, bank, AssuranceLevel.Loa3, "nl", merchantReference);

        LOGGER.info("Session started for bank {} with merchantReference {}", bank, merchantReference);

        // Execute request
        final AuthenticationResponse response = new Communicator().newAuthenticationRequest(request);

        // Handle request result
        if (response.getIsError()) {
            logError(response.getErrorResponse());
            throw new IdinException(response.getErrorResponse());
        }
        LOGGER.info("trxid {}: session created at bank, redirecting to {}",
                response.getTransactionID(),
                response.getIssuerAuthenticationURL());
        final IdinTransaction it = new IdinTransaction(response.getTransactionID(), entranceCode);
        OpenTransactions.addTransaction(it);
        return Response.accepted(response.getIssuerAuthenticationURL()).build();
    }

    private void logError(final ErrorResponse err) {
        LOGGER.error("============================ ERROR ============================");
        LOGGER.error(err.toString());
        LOGGER.error(err.getConsumerMessage());
        LOGGER.error(err.getErrorCode());
        LOGGER.error(err.getErrorDetails());
        LOGGER.error(err.getErrorMessage());
        LOGGER.error(err.getSuggestedAction());
        LOGGER.error("===============================================================");
    }

    @GET
    @Path("/return")
    @Consumes(MediaType.APPLICATION_FORM_URLENCODED)
    public Response authenticated(@DefaultValue("error") @QueryParam("trxid") final String trxID, @QueryParam("ec") final String ec) {
        final NewCookie[] cookies = new NewCookie[1];
        String followupURL = ERROR_URL;

        final boolean isHttpsEnabled = IdinConfiguration.getInstance().isHttpsEnabled();
        if (trxID.equals("error")) {
            //landing on the return page without a trxid. Something is wrong
            cookies[0] = new NewCookie.Builder("error").value("Something unexpected went wrong").path("/").domain(null).comment(null).maxAge(60).secure(isHttpsEnabled).build();
            followupURL = ERROR_URL;
        } else {
            LOGGER.info("trxid {}: return url called", trxID);

            final IdinTransaction transaction = OpenTransactions.findTransaction(trxID);
            if (transaction == null) {
                LOGGER.info("transaction with trxid {} could not be found", trxID);
                return null;
            } else if (!transaction.getEntranceCode().equals(ec)) {
                // Wrong entrance code is used
                LOGGER.info("ec {} of trxid {} does not match with actual ec {}", ec, trxID, transaction.getEntranceCode());
                return null;
            }

            final StatusRequest sr = new StatusRequest(trxID);
            final StatusResponse response = new Communicator().getResponse(sr);
            if (response.getIsError()) {
                logError(response.getErrorResponse());
                throw new IdinException(response.getErrorResponse());
            } else {
                LOGGER.info("trxid {}: response status {}", trxID, response.getStatus());
                transaction.handled();
                switch (response.getStatus()) {
                    case StatusResponse.Success:
                        final Map<String, String> attributes = response.getSamlResponse().getAttributes();
                        LOGGER.info("trxid {}: BIN {}", trxID, attributes.get(IDIN_SAML_BIN_KEY));
                        if (nullOrEmptyAttributes(attributes, trxID)) {
                            cookies[0] = new NewCookie.Builder("error").value("De iDIN transactie leverde niet voldoende attributen op. Helaas kunnen wij hierdoor niet overgaan tot uitgifte van attributen").path("/").domain(null).comment(null).maxAge(60).secure(isHttpsEnabled).build();
                            followupURL = ERROR_URL;
                        } else {
                            //redirect to issuing page
                            followupURL = SUCCESS_URL;
                            final String jwt = createIssueJWT(attributes);
                            cookies[0] = new NewCookie.Builder("jwt").value(jwt).path("/").domain(null).comment(null).maxAge(600).secure(isHttpsEnabled).build();
                        }
                        transaction.finished();
                        break;
                    case StatusResponse.Cancelled:
                        followupURL = ERROR_URL;
                        cookies[0] = new NewCookie.Builder("error").value("De iDIN transactie is geannuleerd. Keer terug naar de iDIN issue pagina om het nog eens te proberen.").path("/").domain(null).comment(null).maxAge(60).secure(isHttpsEnabled).build();
                        transaction.finished();
                        break;
                    case StatusResponse.Expired:
                        followupURL = ERROR_URL;
                        cookies[0] = new NewCookie.Builder("error").value("De iDIN sessie is verlopen. Keer terug naar de iDIN issue pagina om het nog eens te proberen. Als dit probleem zich blijft voordoen, neem dan contact op met uw bank.").path("/").domain(null).comment(null).maxAge(60).secure(isHttpsEnabled).build();
                        transaction.finished();
                        break;
                    case StatusResponse.Open:
                    case StatusResponse.Pending:
                        break;
                    case StatusResponse.Failure:
                    default:
                        transaction.finished();
                        followupURL = ERROR_URL;
                        cookies[0] = new NewCookie.Builder("error").value("Er is iets onverwachts misgegaan. Keer terug naar de iDIN issue pagina om het nog eens te proberen. Als dit probleem zich blijft voordoen, neem dan contact op met uw bank.").path("/").domain(null).comment(null).maxAge(60).secure(isHttpsEnabled).build();
                        break;
                }
            }
        }
        try {
            final URI followupURI = new URI(followupURL);
            return Response.seeOther(followupURI).cookie(cookies).build();
        } catch (final URISyntaxException e) {
            LOGGER.error(e.getMessage(), e);
        }
        return null;
    }

    private String getGenderString(final String isoCode) {
        return switch (isoCode) {
            case "0" -> "unknown";
            case "1" -> "male";
            case "2" -> "female";
            case "9" -> "not applicable";
            default -> throw new RuntimeException("Unknown Gender value");
        };
    }

    private String getDobString(final Date dob) {
        final SimpleDateFormat ourDateFormat = new SimpleDateFormat("dd-MM-yyyy");
        return ourDateFormat.format(dob);
    }

    private Date getDobObject(final String bd) {
        final SimpleDateFormat idinDateFormat = new SimpleDateFormat("yyyyMMdd");
        Date dob = null;
        try {
            dob = idinDateFormat.parse(bd);
        } catch (final ParseException e) {
            throw new RuntimeException(e);
        }
        return dob;
    }

    public HashMap<String, String> ageAttributes(final int[] ages, final Date dob) {
        final HashMap<String, String> attrs = new HashMap<>();

        for (final int age : ages) {
            final Calendar c = Calendar.getInstance();
            c.add(Calendar.YEAR, -1 * age);
            final Date ageDate = c.getTime();

            final String attrValue;
            attrValue = dob.before(ageDate) ? "yes" : "no";
            attrs.put("over" + age, attrValue);
        }

        return attrs;
    }

    private boolean isNullOrEmpty(final String val) {
        return val == null || val.isEmpty();
    }

    private boolean nullOrEmptyAttributes(final Map<String, String> attributes, final String trxId) {
        boolean valueMissing = false;
        if (isNullOrEmpty(attributes.get(IDIN_SAML_BIRTHDATE_KEY))) {
            valueMissing = true;
            LOGGER.error("trxid {}: saml is missing birthdate value", trxId);
        }
        if (isNullOrEmpty(attributes.get(IDIN_SAML_INITIALS_KEY))) {
            valueMissing = true;
            LOGGER.error("trxid {}: saml is missing initials value", trxId);
        }
        if (isNullOrEmpty(attributes.get(IDIN_SAML_LAST_NAME_KEY))) {
            valueMissing = true;
            LOGGER.error("trxid {}: saml is missing lastname value", trxId);
        }
        if (isNullOrEmpty(attributes.get(IDIN_SAML_GENDER_KEY))) {
            valueMissing = true;
            LOGGER.error("trxid {}: saml is missing gender value", trxId);
        }
        if (isNullOrEmpty(attributes.get(IDIN_SAML_STREET_KEY))) {
            valueMissing = true;
            LOGGER.error("trxid {}: saml is missing street value", trxId);
        }
        if (isNullOrEmpty(attributes.get(IDIN_SAML_HOUSE_NO_KEY))) {
            valueMissing = true;
            LOGGER.error("trxid {}: saml is missing house no value", trxId);
        }
        if (isNullOrEmpty(attributes.get(IDIN_SAML_CITY_KEY))) {
            valueMissing = true;
            LOGGER.error("trxid {}: saml is missing city value", trxId);
        }
        if (isNullOrEmpty(attributes.get(IDIN_SAML_POSTAL_CODE_KEY))) {
            valueMissing = true;
            LOGGER.error("trxid {}: saml is missing postal code value", trxId);
        }
        if (isNullOrEmpty(attributes.get(IDIN_SAML_COUNTRY_KEY))) {
            valueMissing = true;
            LOGGER.error("trxid {}: saml is missing country value", trxId);
        }
        if (isNullOrEmpty(attributes.get(IDIN_SAML_BIN_KEY))) {
            //not aborting for missing bin, but it is recorded in the logs
            LOGGER.error("trxid {}: saml is missing bin value", trxId);
        }
        return valueMissing;
    }

    private String createIssueJWT(final Map<String, String> attributes) {
        final HashMap<CredentialIdentifier, HashMap<String, String>> credentials = new HashMap<>();
        final Date dob = getDobObject(attributes.get(IDIN_SAML_BIRTHDATE_KEY));

        String addressLine = attributes.get(IDIN_SAML_STREET_KEY) + " " + attributes.get(IDIN_SAML_HOUSE_NO_KEY);
        if (attributes.get(IDIN_SAML_HOUSE_NO_SUF_KEY) != null) {
            addressLine += attributes.get(IDIN_SAML_HOUSE_NO_SUF_KEY);
        }
        if (attributes.get(IDIN_SAML_ADDRESS_EXTRA_KEY) != null) {
            addressLine += "; " + attributes.get(IDIN_SAML_ADDRESS_EXTRA_KEY);
        }

        final int[] ages = {12, 16, 18, 21, 65};
        final HashMap<String, String> ageAttrs = ageAttributes(ages, dob);

        //get iDIN data credential
        final HashMap<String, String> attrs = new HashMap<>();
        attrs.put(IdinConfiguration.getInstance().getInitialsAttribute(), attributes.get(IDIN_SAML_INITIALS_KEY));
        attrs.put(IdinConfiguration.getInstance().getLastnameAttribute(),
                (attributes.get(IDIN_SAML_LASTNAME_PREFIX_KEY) == null ? "" : attributes.get(IDIN_SAML_LASTNAME_PREFIX_KEY) + " ")
                        + attributes.get(IDIN_SAML_LAST_NAME_KEY));
        attrs.put(IdinConfiguration.getInstance().getBirthdateAttribute(),
                getDobString(dob));
        attrs.put(IdinConfiguration.getInstance().getGenderAttribute(), getGenderString(attributes.get(IDIN_SAML_GENDER_KEY)));
        attrs.put(IdinConfiguration.getInstance().getAddressAttribute(), addressLine);
        attrs.put(IdinConfiguration.getInstance().getCityAttribute(), attributes.get(IDIN_SAML_CITY_KEY));
        attrs.put(IdinConfiguration.getInstance().getPostalcodeAttribute(), attributes.get(IDIN_SAML_POSTAL_CODE_KEY));
        attrs.put(IdinConfiguration.getInstance().getCountryAttribute(), attributes.get(IDIN_SAML_COUNTRY_KEY));
        attrs.putAll(ageAttrs);

        //add iDIN data credential
        LOGGER.info("Now entering credentials");
        LOGGER.info("SchemeManager: {}, IdinIssuer: {}, IdinCredentials: {}", IdinConfiguration.getInstance().getSchemeManager(), IdinConfiguration.getInstance().getIdinIssuer(), IdinConfiguration.getInstance().getIdinCredential());
        credentials.put(credId(
                IdinConfiguration.getInstance().getSchemeManager(),
                IdinConfiguration.getInstance().getIdinIssuer(),
                IdinConfiguration.getInstance().getIdinCredential()
        ), attrs);

        //add age limits credential if enabled
        LOGGER.info("Now entering credentials for age limit");
        LOGGER.info("SchemeManager: {}, AgeLimitsIssuer: {}, AgeLimitCredentials: {}", IdinConfiguration.getInstance().getSchemeManager(), IdinConfiguration.getInstance().getAgeLimitsIssuer(), IdinConfiguration.getInstance().getAgeLimitsCredential());
        if (IdinConfiguration.getInstance().isAgeLimitsCredentialEnabled()) {
            credentials.put(credId(
                    IdinConfiguration.getInstance().getSchemeManager(),
                    IdinConfiguration.getInstance().getAgeLimitsIssuer(),
                    IdinConfiguration.getInstance().getAgeLimitsCredential()
            ), ageAttrs);
        }

        final Calendar calendar = Calendar.getInstance();
        calendar.add(Calendar.YEAR, 1);

        final IdentityProviderRequest iprequest = ApiClient.getIdentityProviderRequest(credentials, calendar.getTimeInMillis() / 1000);

        return ApiClient.getSignedIssuingJWT(iprequest,
                IdinConfiguration.getInstance().getServerName(),
                IdinConfiguration.getInstance().getHumanReadableName(),
                IdinConfiguration.getInstance().getJwtAlgorithm(),
                IdinConfiguration.getInstance().getJwtPrivateKey()
        );

    }

    private static CredentialIdentifier credId(final String scheme, final String issuer, final String credential) {
        if (scheme.isBlank() || issuer.isBlank() || credential.isBlank()) {
            throw new IllegalStateException(String.format(
                    "Missing parts for credential id (scheme='%s', issuer='%s', credential='%s')", scheme, issuer, credential));
        }
        return new CredentialIdentifier(scheme, issuer, credential);
    }

}
