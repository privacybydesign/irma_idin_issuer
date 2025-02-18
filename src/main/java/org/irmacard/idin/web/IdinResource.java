package org.irmacard.idin.web;

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

import javax.ws.rs.*;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.NewCookie;
import javax.ws.rs.core.Response;
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


	private static Random random = new Random();
	private static SecureRandom secureRandom = new SecureRandom();
	private static Logger logger = LoggerFactory.getLogger(IdinResource.class);

	private static String successURL = IdinConfiguration.getInstance().getEnrollUrl();
	private static String errorURL = IdinConfiguration.getInstance().getReturnUrl()+"/error.html";

	@GET
	@Path("/banks")
	@Produces(MediaType.APPLICATION_JSON)
	public Map<String,List<DirectoryResponseBase.Issuer>> banks() throws IOException {
		logger.info("Bank list requested");
		return IdinConfiguration.getInstance().getIdinIssuers().getIssuers();
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

	@GET
	@Path("/openTransactions")
	@Produces(MediaType.TEXT_PLAIN)
	public String openTransactions (){
		return OpenTransactions.getOpenTransactions();
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
	public Response start (String bank){
		if (!IdinConfiguration.getInstance().getIdinIssuers().containsBankCode(bank)){
			throw new RuntimeException("Illegal bankcode received");
		}
		// Create request
		String merchantReference = new BigInteger(130, random).toString(32);
		//iDIN lib wants the random MerchantReference to start with a letter.
		merchantReference = "a"+merchantReference;

		String entranceCode = new RandomString(40, secureRandom).nextString();
		AuthenticationRequest request = new AuthenticationRequest(entranceCode,IDIN_ATTRIBUTES,bank,AssuranceLevel.Loa3,"nl",merchantReference);

		logger.info("Session started for bank {} with merchantReference {}", bank, merchantReference);

		// Execute request
		AuthenticationResponse response = new Communicator().newAuthenticationRequest(request);

		// Handle request result
		if (response.getIsError()) {
			logError(response.getErrorResponse());
			throw new IdinException(response.getErrorResponse());
		}
		logger.info("trxid {}: session created at bank, redirecting to {}",
				response.getTransactionID(),
				response.getIssuerAuthenticationURL());
		IdinTransaction it = new IdinTransaction(response.getTransactionID(), entranceCode);
		OpenTransactions.addTransaction(it);
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

	@GET
	@Path("/return")
	@Consumes(MediaType.APPLICATION_FORM_URLENCODED)
	public Response authenticated(@DefaultValue("error") @QueryParam("trxid") String trxID, @QueryParam("ec") String ec){
		NewCookie[] cookies = new NewCookie[1];
		String followupURL = errorURL;

		boolean isHttpsEnabled = IdinConfiguration.getInstance().isHttpsEnabled();
		if (trxID.equals("error") ){
			//landing on the return page without a trxid. Something is wrong
			cookies[0] = new NewCookie("error","Something unexpected went wrong","/",null,null,60,isHttpsEnabled);
			followupURL = errorURL;
		} else {
			logger.info("trxid {}: return url called", trxID);

			IdinTransaction transaction = OpenTransactions.findTransaction(trxID);
			if (transaction == null) {
				logger.info("transaction with trxid {} could not be found", trxID);
				return null;
			} else if (!transaction.getEntranceCode().equals(ec)) {
				// Wrong entrance code is used
				logger.info("ec {} of trxid {} does not match with actual ec {}", ec, trxID, transaction.getEntranceCode());
				return null;
			}

			StatusRequest sr = new StatusRequest(trxID);
			StatusResponse response = new Communicator().getResponse(sr);
			if (response.getIsError()) {
				logError(response.getErrorResponse());
				throw new IdinException(response.getErrorResponse());
			} else {
				logger.info("trxid {}: response status {}", trxID, response.getStatus());
				transaction.handled();
				switch (response.getStatus()) {
					case StatusResponse.Success:
						Map<String, String> attributes = response.getSamlResponse().getAttributes();
						logger.info("trxid {}: BIN {}", trxID, attributes.get(idinSamlBinKey));
						if (nullOrEmptyAttributes(attributes,trxID)){
							cookies[0] = new NewCookie("error","De iDIN transactie leverde niet voldoende attributen op. Helaas kunnen wij hierdoor niet overgaan tot uitgifte van attributen","/",null,null,60,isHttpsEnabled);
							followupURL = errorURL;
						} else {
							//redirect to issuing page
							followupURL = successURL;
							String jwt = createIssueJWT(attributes);
							cookies[0] = new NewCookie("jwt", jwt, "/", null, null, 600, isHttpsEnabled);
						}
						transaction.finished();
						break;
					case StatusResponse.Cancelled:
						followupURL = errorURL;
						cookies[0] = new NewCookie("error", "De iDIN transactie is geannuleerd. Keer terug naar de iDIN issue pagina om het nog eens te proberen.", "/", null, null, 60, isHttpsEnabled);
						transaction.finished();
						break;
					case StatusResponse.Expired:
						followupURL = errorURL;
						cookies[0] = new NewCookie("error", "De iDIN sessie is verlopen. Keer terug naar de iDIN issue pagina om het nog eens te proberen. Als dit probleem zich blijft voordoen, neem dan contact op met uw bank.", "/", null, null, 60, isHttpsEnabled);
						transaction.finished();
						break;
					case StatusResponse.Open:
					case StatusResponse.Pending:
						break;
					case StatusResponse.Failure:
					default:
						transaction.finished();
						followupURL = errorURL;
						cookies[0] = new NewCookie("error", "Er is iets onverwachts misgegaan. Keer terug naar de iDIN issue pagina om het nog eens te proberen. Als dit probleem zich blijft voordoen, neem dan contact op met uw bank.", "/", null, null, 60, isHttpsEnabled);
						break;
				}
			}
		}
		try {
			URI followupURI = new URI(followupURL);
			return Response.seeOther(followupURI).cookie(cookies).build();
		} catch (URISyntaxException e) {
			e.printStackTrace();
		}
		return null;
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

		int[] ages = {12,16,18,21,65};
		HashMap<String,String> ageAttrs = ageAttributes(ages, dob);

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
		attrs.putAll(ageAttrs);

		//add iDIN data credential
		credentials.put(new CredentialIdentifier(
				IdinConfiguration.getInstance().getSchemeManager(),
				IdinConfiguration.getInstance().getIdinIssuer(),
				IdinConfiguration.getInstance().getIdinCredential()
		), attrs);

		//add age limits credential if enabled
		if (IdinConfiguration.getInstance().isAgeLimitsCredentialEnabled()) {
			credentials.put(new CredentialIdentifier(
					IdinConfiguration.getInstance().getSchemeManager(),
					IdinConfiguration.getInstance().getAgeLimitsIssuer(),
					IdinConfiguration.getInstance().getAgeLimitsCredential()
			), ageAttrs);
		}

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

}
