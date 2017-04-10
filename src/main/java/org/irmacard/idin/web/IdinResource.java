package org.irmacard.idin.web;

import net.bankid.merchant.library.*;
import net.bankid.merchant.library.internal.DirectoryResponseBase;
import org.irmacard.api.common.ApiClient;
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
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Random;

@Path("v1/idin")
public class IdinResource {
	public final static int IDIN_ATTRIBUTES = ServiceIds.Address
			| ServiceIds.ConsumerBin
			| ServiceIds.DateOfBirth
			| ServiceIds.Email
			| ServiceIds.Gender
			| ServiceIds.Telephone
			| ServiceIds.Name;

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
	private static final String idinSamlPostalCodeKey = "urn:nl:bvn:bankid:1.0:consumer.postalcode";


	private static Random random = new Random();
	private static Logger logger = LoggerFactory.getLogger(IdinResource.class);

	private static String successURL = IdinConfiguration.getInstance().getUrl()+"/enroll.html";
	private static String errorURL = IdinConfiguration.getInstance().getUrl()+"/error.html";

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


	private boolean isHttpsEnabled = false;

	@GET
	@Path("/banks")
	@Produces(MediaType.APPLICATION_JSON)
	public Map<String,List<DirectoryResponseBase.Issuer>> banks() throws IOException {
		return IdinConfiguration.getInstance().getIdinIssuers().getIssuers();
	}

	@POST
	@Path("/start")
	@Consumes(MediaType.APPLICATION_FORM_URLENCODED)
	public Response start (String bank){
		// Create request
		String merchantReference = new BigInteger(130, random).toString(32);
		//iDIN lib wants the random MerchantReference to start with a letter.
		merchantReference = "a"+merchantReference;

		AuthenticationRequest request = new AuthenticationRequest(entranceCode,IDIN_ATTRIBUTES,bank,AssuranceLevel.Loa3,"nl",merchantReference);

		// Execute request
		AuthenticationResponse response = new Communicator().newAuthenticationRequest(request);

		// Handle request result
		if (response.getIsError()) {
			logError(response.getErrorResponse());
			throw new IdinException(response.getErrorResponse());
		}
		return Response.accepted(response.getIssuerAuthenticationURL()).build();
	}

	private void logError (ErrorResponse err){
		logger.error("============ERROR getting issuer authentication URL============");
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
	public Response authenticated(@DefaultValue("error") @QueryParam("trxid") String trxID){
		NewCookie[] cookies = new NewCookie[1];
		String followupURL = errorURL;

		if (trxID.equals("error") ){
			//landing on the return page without a trxid. Something is wrong
			cookies[0] = new NewCookie("error","Something unexpected went wrong","/",null,null,60,false,isHttpsEnabled);
			followupURL = errorURL;
		} else {
			StatusRequest sr = new StatusRequest(trxID);
			StatusResponse response = new Communicator().getResponse(sr);
			if (response.getIsError()) {
				logger.warn("Received iDIN error: " + response.getErrorResponse().getErrorCode() + " " + response.getErrorResponse().getErrorMessage());
				throw new IdinException(response.getErrorResponse());
			} else {
				switch (response.getStatus()) {
					case StatusResponse.Success:
						//redirect to issuing page
						followupURL = successURL;
						Map<String, String> attributes = response.getSamlResponse().getAttributes();
						String jwt = createIssueJWT(attributes);
						cookies[0] = new NewCookie("jwt", jwt, "/", null, null, 60, false, isHttpsEnabled);
						break;
					case StatusResponse.Cancelled:
						followupURL = errorURL;
						cookies[0] = new NewCookie("error", "De iDIN transactie is geannuleerd. Keer terug naar de iDIN issue pagina om het nog eens te proberen. Als dit probleem zich blijft voordoen, neem dan contact op met uw bank.", "/", null, null, 60, false, isHttpsEnabled);
						break;
					case StatusResponse.Expired:
						followupURL = errorURL;
						cookies[0] = new NewCookie("error", "De iDIN sessie is verlopen. Keer terug naar de iDIN issue pagina om het nog eens te proberen. Als dit probleem zich blijft voordoen, neem dan contact op met uw bank.", "/", null, null, 60, false, isHttpsEnabled);
						break;
					case StatusResponse.Open:
					case StatusResponse.Pending:
					case StatusResponse.Failure:
					default:
						followupURL = errorURL;
						cookies[0] = new NewCookie("error", "Er is iets onverwachts misgegaan. Keer terug naar de iDIN issue pagina om het nog eens te proberen. Als dit probleem zich blijft voordoen, neem dan contact op met uw bank.", "/", null, null, 60, false, isHttpsEnabled);
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

	private String createIssueJWT(Map<String, String> attributes) {
		HashMap<CredentialIdentifier, HashMap<String, String>> credentials = new HashMap<>();

		HashMap<String,String> attrs = new HashMap<>();
		//attrs.put(IdinConfiguration.getInstance().getTelephoneAttribute(), attributes.get(idinSamlTelephoneKey));
		//attrs.put(IdinConfiguration.getInstance().getEmailAttribute(), attributes.get(idinSamlEmailKey));
		//attrs.put(IdinConfiguration.getInstance().getCountryAttribute(), attributes.get(idinSamlCountryKey));
		attrs.put(IdinConfiguration.getInstance().getInitialsAttribute(), attributes.get(idinSamlInitialsKey));
		attrs.put(IdinConfiguration.getInstance().getLastnameAttribute(),
				(attributes.get(idinSamlLastnamePrefixKey)==null?"":attributes.get(idinSamlLastnamePrefixKey)+ " ")
						+ attributes.get(idinSamlLastNameKey));
		attrs.put(IdinConfiguration.getInstance().getBirthdateAttribute(), attributes.get(idinSamlBirthdateKey));
		attrs.put(IdinConfiguration.getInstance().getGenderAttribute(),attributes.get(idinSamlGenderKey));
		attrs.put(IdinConfiguration.getInstance().getAddressAttribute(), attributes.get(idinSamlStreetKey) +" "+attributes.get(idinSamlHouseNoKey));
		attrs.put(IdinConfiguration.getInstance().getCityAttribute(), attributes.get(idinSamlCityKey));
		attrs.put(IdinConfiguration.getInstance().getPostalcodeAttribute(),attributes.get(idinSamlPostalCodeKey));


		credentials.put(new CredentialIdentifier(
				IdinConfiguration.getInstance().getSchemeManager(),
				IdinConfiguration.getInstance().getIdinIssuer(),
				IdinConfiguration.getInstance().getIdinCredential()
		), attrs);

		return ApiClient.getIssuingJWT(credentials,
				IdinConfiguration.getInstance().getHumanReadableName(),
				false,
				null,
				null
		);

	}
}
