package org.irmacard.idx.web;

import com.google.gson.reflect.TypeToken;
import io.jsonwebtoken.ExpiredJwtException;
import net.bankid.merchant.library.*;
import net.bankid.merchant.library.internal.DirectoryResponseBase;
import org.bouncycastle.util.encoders.Hex;
import org.apache.commons.codec.binary.Base64;
import org.irmacard.api.common.ApiClient;
import org.irmacard.api.common.AttributeDisjunction;
import org.irmacard.api.common.AttributeDisjunctionList;
import org.irmacard.api.common.JwtParser;
import org.irmacard.api.common.issuing.IdentityProviderRequest;
import org.irmacard.credentials.info.AttributeIdentifier;
import org.irmacard.credentials.info.CredentialIdentifier;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.crypto.Mac;
import javax.crypto.SecretKeyFactory;
import javax.crypto.spec.PBEKeySpec;
import javax.crypto.spec.SecretKeySpec;
import javax.ws.rs.*;
import javax.ws.rs.core.CacheControl;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import java.io.IOException;
import java.lang.reflect.Type;
import java.math.BigInteger;
import java.security.InvalidKeyException;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.security.spec.InvalidKeySpecException;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.*;
import java.util.Arrays;

@Path("v1/idin")
public class IdinResource {
	public final static int IDIN_ATTRIBUTES = ServiceIds.Address
			| ServiceIds.ConsumerBin
			| ServiceIds.DateOfBirth
			| ServiceIds.Gender
			| ServiceIds.Name;

	private static final String idinSamlBinKey = "urn:nl:bvn:bankid:1.0:consumer.bin";
	private static final String idinSamlCityKey = "urn:nl:bvn:bankid:1.0:consumer.city";
	private static final String idinSamlLastnamePrefixKey = "urn:nl:bvn:bankid:1.0:consumer.legallastnameprefix";
	private static final String idinSamlLastNameKey = "urn:nl:bvn:bankid:1.0:consumer.legallastname";
	private static final String idinSamlInitialsKey = "urn:nl:bvn:bankid:1.0:consumer.initials";
	private static final String idinSamlEmailKey = "urn:nl:bvn:bankid:1.0:consumer.email";
	private static final String idinSamlBirthdateKey = "urn:nl:bvn:bankid:1.0:consumer.dateofbirth";
	private static final String idinSamlCountryKey = "urn:nl:bvn:bankid:1.0:consumer.country";
	private static final String idinSamlGenderKey = "urn:nl:bvn:bankid:1.0:consumer.gender";
	private static final String idinSamlTelephoneKey = "urn:nl:bvn:bankid:1.0:consumer.telephone";
	private static final String idinSamlStreetKey = "urn:nl:bvn:bankid:1.0:consumer.street";
	private static final String idinSamlHouseNoKey = "urn:nl:bvn:bankid:1.0:consumer.houseno";
	private static final String idinSamlHouseNoSufKey = "urn:nl:bvn:bankid:1.0:consumer.housenosuf";
	private static final String idinSamlAddressExtraKey = "urn:nl:bvn:bankid:1.0:consumer.addressextra";
	private static final String idinSamlPostalCodeKey = "urn:nl:bvn:bankid:1.0:consumer.postalcode";

	private static final String HMAC_ALGORITHM = "HmacSHA512";

	private static Random random = new Random();
	private static Logger logger = LoggerFactory.getLogger(IdinResource.class);

	//Issuer response codes
	private static String entranceCode = "successHIO100OIHtest";
	//private static String entranceCode = "cancelledHIO200OIHtest";
	//private static String entranceCode = "expiredHIO300OIHtest";
	//private static String entranceCode = "openHIO400OIHtest";
	//private static String entranceCode = "failureHIO500OIHtest";

	//Issuer error messages
	//private static String entranceCode = "systemUnavailabilityHIO702OIHtest";
	//private static String entranceCode = "receivedXMLNotValidHIO701OIHtest";
	//private static String entranceCode = "invalidElectronicSignatureHIO703OIHtest";
	//private static String entranceCode = "versionNumberInvalidHIO704OIHtest";
	//private static String entranceCode = "productSpecificErrorHIO705OIHtest";


	@GET
	@Path("/banks")
	@Produces(MediaType.APPLICATION_JSON)
	public Response banks() throws IOException {
		logger.info("Bank list requested");
		CacheControl cc = new CacheControl();
		cc.setMaxAge(86400); // 1 day
		Map<String,List<DirectoryResponseBase.Issuer>> issuers = IdinConfiguration.getInstance().getIdinIssuers().getIssuers();
		return Response.status(200).entity(issuers).cacheControl(cc).build();
	}

	@GET
	@Path("/create-iban-disclosure-req")
	@Produces(MediaType.TEXT_PLAIN)
	public String getDiscloseEmailRequest() throws IOException {
		IdinConfiguration conf = IdinConfiguration.getInstance();

		// Request an email address.
		AttributeDisjunctionList requestAttrs = new AttributeDisjunctionList(2);
		requestAttrs.add(new AttributeDisjunction("BIC", conf.getIdealBICAttribute()));
		requestAttrs.add(new AttributeDisjunction("IBAN", conf.getIdealIBANAttribute()));
		return ApiClient.getDisclosureJWT(requestAttrs,
				conf.getServerName(),
				conf.getHumanReadableName(),
				conf.getJwtAlgorithm(),
				conf.getJwtPrivateKey());
	}

	@GET
	@Path("/verify")
	@Produces(MediaType.TEXT_PLAIN)
	public String getVerificationJWT () {
		AttributeDisjunctionList list = new AttributeDisjunctionList(1);
		list.add(new AttributeDisjunction("Geboortedatum", getIdinBdAttributeIdentifier()));
		return ApiClient.getDisclosureJWT(
				list,
				IdinConfiguration.getInstance().getServerName(),
				IdinConfiguration.getInstance().getHumanReadableName(),
				IdinConfiguration.getInstance().getJwtAlgorithm(),
				IdinConfiguration.getInstance().getJwtPrivateKey()
		);
	}


	private AttributeIdentifier getIdinBdAttributeIdentifier() {
		IdinConfiguration conf = IdinConfiguration.getInstance();
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
	public Response start (@FormParam("bank") String bank, @FormParam("token") String token){
		if (!IdinConfiguration.getInstance().getIdinIssuers().containsBankCode(bank)){
			return Response.status(Response.Status.BAD_REQUEST).entity("error:invalid-bank").build();
		}
		if (token == null) {
			return Response.status(Response.Status.BAD_REQUEST).entity("error:missing-params").build();
		}
		if (loadTokenRecord(token) == null) {
			return Response.status(Response.Status.BAD_REQUEST).entity("error:invalid-token").build();
		}

		// Create request
		String merchantReference = new BigInteger(130, random).toString(32);
		//iDIN lib wants the random MerchantReference to start with a letter.
		merchantReference = "a"+merchantReference;

		AuthenticationRequest request = new AuthenticationRequest(entranceCode,IDIN_ATTRIBUTES,bank,AssuranceLevel.Loa3,"nl",merchantReference);

		logger.info("Session started for bank {} with merchantReference {}", bank, merchantReference);

		// Execute request
		AuthenticationResponse response = new Communicator().newAuthenticationRequest(request);

		// Handle request result
		if (response.getIsError()) {
			logError(response.getErrorResponse());
			return Response.status(Response.Status.BAD_GATEWAY).entity("error:" + response.getErrorResponse().getConsumerMessage()).build();
		}
		logger.info("trxid {}: session created at bank, redirecting to {}",
				response.getTransactionID(),
				response.getIssuerAuthenticationURL());
		IdinOpenTransactions.getIdinOpenTransactions().addTransaction(response.getTransactionID());
		return Response.accepted(response.getIssuerAuthenticationURL()).build();
	}

	private void logError (ErrorResponse err){
		logger.error("============================ ERROR ============================");
		logger.error(err.toString());
		logger.error(err.getConsumerMessage());
		logger.error(err.getErrorCode());
		logger.error(err.getErrorDetails());
		logger.error(err.getErrorMessage());
		logger.error(err.getSuggestedAction());
		logger.error("===============================================================");
	}

	@POST
	@Path("/return")
	public Response authenticated(@FormParam("trxid") String trxID, @FormParam("token") String token) {
		IdinToken record = loadTokenRecord(token);
		if (record == null) {
			return Response.status(Response.Status.BAD_REQUEST).entity("error:invalid-token").build();
		}
		StatusRequest sr = new StatusRequest(trxID);
		StatusResponse response = new Communicator().getResponse(sr);
		if (response.getIsError()) {
			logError(response.getErrorResponse());
			throw new IdinException(response.getErrorResponse());
		}
		logger.info("trxid {}: response status {}", trxID, response.getStatus());
		switch (response.getStatus()) {
			case StatusResponse.Open:
			case StatusResponse.Pending:
				// do not remove transaction
				break;
			default:
				IdinOpenTransactions.getIdinOpenTransactions().removeTransaction(trxID);
				break;
		}
		switch (response.getStatus()) {
			case StatusResponse.Success:
				Map<String, String> attributes = response.getSamlResponse().getAttributes();
				logger.info("trxid {}: BIN {}", trxID, attributes.get(idinSamlBinKey));
				if (nullOrEmptyAttributes(attributes,trxID)) {
					return Response.status(Response.Status.BAD_GATEWAY).entity("error:missing-idin-attributes").build();
				}
				String jwt = createIssueJWT(attributes);
				// This is a used token.
				record.delete();
				return Response.status(Response.Status.OK).entity(jwt).build();
			case StatusResponse.Cancelled:
				return Response.status(Response.Status.BAD_GATEWAY).entity("idin-status:Cancelled").build();
			case StatusResponse.Expired:
				return Response.status(Response.Status.BAD_GATEWAY).entity("idin-status:Expired").build();
			case StatusResponse.Open:
			case StatusResponse.Pending:
				return Response.status(Response.Status.BAD_GATEWAY).entity("idin-status:Open").build();
			case StatusResponse.Failure:
			default:
				return Response.status(Response.Status.BAD_GATEWAY).entity("idin-status:other").build();
		}
	}

	@POST
	@Path("/get-token")
	public Response generateToken(@FormParam("jwt") String jwt) {
		Map<AttributeIdentifier, String> disclosureAttrs;
		try {
			Type t = new TypeToken<Map<AttributeIdentifier, String>>() {}.getType();
			// TODO: max age
			JwtParser<Map<AttributeIdentifier, String>> parser =
					new JwtParser<>(t, true, 120, "disclosure_result", "attributes");
			parser.setSigningKey(IdinConfiguration.getInstance().getApiServerPublicKey());
			parser.parseJwt(jwt);
			disclosureAttrs = parser.getPayload();
		} catch (ExpiredJwtException e) {
			return Response.status(Response.Status.BAD_REQUEST).entity("error:invalid-jwt").build();
		}

		IdinConfiguration conf = IdinConfiguration.getInstance();
		String bic = disclosureAttrs.get(new AttributeIdentifier(conf.getIdealBICAttribute()));
		String iban = disclosureAttrs.get(new AttributeIdentifier(conf.getIdealIBANAttribute()));
		if (bic == null || iban == null) {
			logger.error("cannot find IBAN/BIC attributes in provided JWT");
			return Response.status(Response.Status.BAD_REQUEST).entity("error:attributes-not-found-in-jwt").build();
		}

		byte[] rawToken = IdinResource.makeToken(bic, iban);
		String token = Base64.encodeBase64URLSafeString(rawToken);

		byte[] rawSignature = IdinResource.signToken(rawToken);
		String signature = Base64.encodeBase64URLSafeString(rawSignature);
		String signedToken = token + ":" + signature;

		// Check whether the token is listed in the database
		if (IdinResource.loadTokenRecord(signedToken) == null) {
			return Response.status(Response.Status.BAD_REQUEST).entity("error:invalid-token").build();
		}

		return Response.status(Response.Status.OK).entity(signedToken).build();
	}

	private String getGenderString(String isoCode){
		switch (isoCode){
			case "0":
				return "unknown";
			case "1":
				return "male";
			case "2":
				return "female";
			case "9":
				return "not applicable";
			default:
				throw new RuntimeException("Unknown Gender value");
		}
	}

	private String getDobString (Date dob){
		SimpleDateFormat ourDateFormat = new SimpleDateFormat("dd-MM-yyyy");
		return ourDateFormat.format(dob);
	}

	private Date getDobObject (String bd){
		SimpleDateFormat idinDateFormat = new SimpleDateFormat("yyyyMMdd");
		Date dob = null;
		try {
			dob = idinDateFormat.parse(bd);
		} catch (ParseException e) {
			throw new RuntimeException(e);
		}
		return dob;
	}

	public HashMap<String, String> ageAttributes(int[] ages, Date dob) {
		HashMap<String, String> attrs = new HashMap<>();

		for (int age : ages) {
			Calendar c = Calendar.getInstance();
			c.add(Calendar.YEAR, -1 * age);
			Date ageDate = c.getTime();

			String attrValue;
			attrValue = dob.before(ageDate) ? "yes" : "no";
			attrs.put("over" + age, attrValue);
		}

		return attrs;
	}

	private boolean isNullOrEmpty(String val){
		return val == null || val.length()==0;
	}
	private boolean nullOrEmptyAttributes (Map<String,String> attributes, String trxId){
		boolean valueMissing = false;
		if (isNullOrEmpty(attributes.get(idinSamlBirthdateKey))){
			valueMissing = true;
			logger.error("trxid {}: saml is missing birthdate value", trxId);
		}
		if (isNullOrEmpty(attributes.get(idinSamlInitialsKey))){
			valueMissing = true;
			logger.error("trxid {}: saml is missing initials value", trxId);
		}
		if (isNullOrEmpty(attributes.get(idinSamlLastNameKey))){
			valueMissing = true;
			logger.error("trxid {}: saml is missing lastname value", trxId);
		}
		if (isNullOrEmpty(attributes.get(idinSamlGenderKey))){
			valueMissing = true;
			logger.error("trxid {}: saml is missing gender value", trxId);
		}
		if (isNullOrEmpty(attributes.get(idinSamlStreetKey))){
			valueMissing = true;
			logger.error("trxid {}: saml is missing street value", trxId);
		}
		if (isNullOrEmpty(attributes.get(idinSamlHouseNoKey))){
			valueMissing = true;
			logger.error("trxid {}: saml is missing house no value", trxId);
		}
		if (isNullOrEmpty(attributes.get(idinSamlCityKey))){
			valueMissing = true;
			logger.error("trxid {}: saml is missing city value", trxId);
		}
		if (isNullOrEmpty(attributes.get(idinSamlPostalCodeKey))){
			valueMissing = true;
			logger.error("trxid {}: saml is missing postal code value", trxId);
		}
		if (isNullOrEmpty(attributes.get(idinSamlCountryKey))){
			valueMissing = true;
			logger.error("trxid {}: saml is missing country value", trxId);
		}
		if (isNullOrEmpty(attributes.get(idinSamlBinKey))){
			//not aborting for missing bin, but it is recorded in the logs
			logger.error("trxid {}: saml is missing bin value", trxId);
		}
		return valueMissing;
	}

	private String createIssueJWT(Map<String, String> attributes) {
		HashMap<CredentialIdentifier, HashMap<String, String>> credentials = new HashMap<>();
		Date dob = getDobObject(attributes.get(idinSamlBirthdateKey));

		String addressLine = attributes.get(idinSamlStreetKey) +" "+attributes.get(idinSamlHouseNoKey);
		if (attributes.get(idinSamlHouseNoSufKey) != null) {
			addressLine += attributes.get(idinSamlHouseNoSufKey);
		}
		if (attributes.get(idinSamlAddressExtraKey) != null) {
			addressLine += "; " + attributes.get(idinSamlAddressExtraKey);
		}

		//get iDIN data credential
		HashMap<String,String> attrs = new HashMap<>();
		attrs.put(IdinConfiguration.getInstance().getInitialsAttribute(), attributes.get(idinSamlInitialsKey));
		attrs.put(IdinConfiguration.getInstance().getLastnameAttribute(),
				(attributes.get(idinSamlLastnamePrefixKey)==null?"":attributes.get(idinSamlLastnamePrefixKey)+ " ")
						+ attributes.get(idinSamlLastNameKey));
		attrs.put(IdinConfiguration.getInstance().getBirthdateAttribute(),
				getDobString(dob));
		attrs.put(IdinConfiguration.getInstance().getGenderAttribute(),getGenderString(attributes.get(idinSamlGenderKey)));
		attrs.put(IdinConfiguration.getInstance().getAddressAttribute(), addressLine);
		attrs.put(IdinConfiguration.getInstance().getCityAttribute(), attributes.get(idinSamlCityKey));
		attrs.put(IdinConfiguration.getInstance().getPostalcodeAttribute(),attributes.get(idinSamlPostalCodeKey));
		attrs.put(IdinConfiguration.getInstance().getCountryAttribute(), attributes.get(idinSamlCountryKey));

		//add iDIN data credential
		credentials.put(new CredentialIdentifier(
				IdinConfiguration.getInstance().getSchemeManager(),
				IdinConfiguration.getInstance().getIdinIssuer(),
				IdinConfiguration.getInstance().getIdinCredential()
		), attrs);

		//add age limits credential
		int[] ages = {12,16,18,21,65};
		credentials.put(new CredentialIdentifier(
				IdinConfiguration.getInstance().getSchemeManager(),
				IdinConfiguration.getInstance().getAgeLimitsIssuer(),
				IdinConfiguration.getInstance().getAgeLimitsCredential()
		), ageAttributes(ages, dob));

		Calendar calendar = Calendar.getInstance();
		calendar.add(Calendar.YEAR, 1);

		IdentityProviderRequest iprequest = ApiClient.getIdentityProviderRequest(credentials, calendar.getTimeInMillis()/1000);

		return ApiClient.getSignedIssuingJWT(iprequest,
				IdinConfiguration.getInstance().getServerName(),
				IdinConfiguration.getInstance().getHumanReadableName(),
				IdinConfiguration.getInstance().getJwtAlgorithm(),
				IdinConfiguration.getInstance().getJwtPrivateKey()
		);

	}

	/**
	 * Load the token record from the database, or abort when anything at all
	 * seems fishy about it. It returns null when the token is invalid, not
	 * signed correctly, or does not occur in the database.
	 */
	public static IdinToken loadTokenRecord(String token) {
		if (token == null) {
			logger.error("token == null");
			return null;
		}
		String[] parts = token.split(":");
		if (parts.length != 2) {
			logger.error("token: parts.length != 2");
			return null;
		}
		try {
			byte[] rawToken = Base64.decodeBase64(parts[0]);
			byte[] rawSignature = Base64.decodeBase64(parts[1]);
			byte[] rawComputedSignature = signToken(rawToken);
			if (!org.bouncycastle.util.Arrays.constantTimeAreEqual(rawSignature, rawComputedSignature)) {
				// The signature on the token was not valid.
				logger.error("token: signature mismatch");
				return null;
			}
			// TODO: validate the parameter in the database.
			IdinApplication.openDatabase();
			return IdinToken.findFirst("hashedToken = ?", hashToken(rawToken));
		} catch (IllegalArgumentException e) {
			logger.error("token: not found");
			// invalid base64
			return null;
		}
	}

	/**
	 * Anonymize the BIC and IBAN numbers by hashing them using PBKDF2.
	 * The goal is to treat the IBAN/BIC like a regular low-entropy password so
	 * that trying all possible IBAN numbers is really difficult. Think of banks
	 * that issue very predictable (low entropy) IBAN numbers for which it is
	 * feasible to compute lower numbers.
	 * Sadly we can't really use a salt as we need to index the token in a
	 * database and we don't have the equivalent of a username.
	 */
	public static byte[] makeToken(String bic, String iban) {
		String input = bic + "-" + iban;
		String salt = IdinConfiguration.getInstance().getStaticSalt();
		if (salt.length() == 0) {
			// Make sure we have a salt configured - just in case.
			throw new IllegalStateException("no static salt configured");
		}
		PBEKeySpec spec = new PBEKeySpec(input.toCharArray(), salt.getBytes(), 10000, 32 * 8);
		try {
			SecretKeyFactory skf = SecretKeyFactory.getInstance("PBKDF2WithHmacSHA512");
			return skf.generateSecret(spec).getEncoded();
		} catch (NoSuchAlgorithmException | InvalidKeySpecException e) {
			// should not happen
			throw new RuntimeException("no support for PBKDF2-HMAC-SHA512?", e);
		}
	}

	/**
	 * Hash a token to be stored in the database.
	 * The reason a hash is applied first is to avoid timing attacks while
	 * retrieving a token. When looking up a token the database compares the
	 * user-supplied token to tokens in the database in a way that's certainly
	 * not constant-time. Hashing it first makes timing attacks impossible.
	 */
	public static String hashToken(byte[] token) {
		try {
			MessageDigest md = MessageDigest.getInstance("SHA-512");
			md.update(token);
			byte[] digest = Arrays.copyOf(md.digest(), 32); // SHA512/256
			return Hex.toHexString(digest);
		} catch (NoSuchAlgorithmException e) {
			// very unlikely
			throw new RuntimeException("could not instantiate SHA512 hash", e);
		}
	}

	/**
	 * Sign a token for the happy flow from iDeal to iDIN, without pause.
	 * Because it is signed we can be sure
	 * @return the HMAC signature
	 */
	public static byte[] signToken(byte[] token) {
		String key = IdinConfiguration.getInstance().getHMACKey();
		if (key.length() == 0) {
			// Make sure we have a salt configured - just in case.
			throw new IllegalStateException("no HMAC key configured");
		}
		try {
			SecretKeySpec spec = new SecretKeySpec(key.getBytes(), HMAC_ALGORITHM);
			Mac mac = Mac.getInstance(HMAC_ALGORITHM);
			mac.init(spec);
			byte[] signature = mac.doFinal(token);
			// 32 bytes (256 bits) is long enough.
			return Arrays.copyOf(signature, 32);
		} catch (NoSuchAlgorithmException | InvalidKeyException e) {
			// should not happen
			throw new RuntimeException("unexpected missing algorithm", e);
		}
	}
}
