package org.irmacard.ideal.web;

import com.ing.ideal.connector.Issuer;
import org.irmacard.api.common.ApiClient;
import org.irmacard.api.common.AttributeDisjunction;
import org.irmacard.api.common.AttributeDisjunctionList;
import org.irmacard.api.common.issuing.IdentityProviderRequest;
import org.irmacard.credentials.info.CredentialIdentifier;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.ws.rs.*;
import javax.ws.rs.core.MediaType;
import java.io.IOException;
import java.util.*;

@Path("v1/ideal")
public class IdealResource {

	private static Random random = new Random();
	private static Logger logger = LoggerFactory.getLogger(IdealResource.class);

	private static String successURL = IdealConfiguration.getInstance().getReturnUrl()+"/enroll.html";
	private static String errorURL = IdealConfiguration.getInstance().getReturnUrl()+"/error.html";


	@GET
	@Path("/banks")
	@Produces(MediaType.APPLICATION_JSON)
	public Map<String,List<Issuer>> banks() throws IOException {
		logger.info("Bank list requested");
		return IdealConfiguration.getInstance().getIdealIssuers().getIssuers();
	}

	@GET
	@Path("/create-email-disclosure-req")
	@Produces(MediaType.TEXT_PLAIN)
	public String getDiscloseEmailRequest() throws IOException {
		IdealConfiguration conf = IdealConfiguration.getInstance();

		// Request an email address.
		// TODO: use more email address sources.
		AttributeDisjunctionList requestAttrs = new AttributeDisjunctionList(1);
		requestAttrs.add(new AttributeDisjunction("Email", "pbdf.pbdf.email.email"));
		return ApiClient.getDisclosureJWT(requestAttrs,
				conf.getServerName(),
				conf.getHumanReadableName(),
				conf.getJwtAlgorithm(),
				conf.getJwtPrivateKey());
	}

	//TODO
/*	@GET
	@Path("/verify-iDEAL")
	@Produces(MediaType.TEXT_PLAIN)
	public String getVerificationJWT () {
		AttributeDisjunctionList list = new AttributeDisjunctionList(1);
		list.add(new AttributeDisjunction("IBAN", getIdealIBANAttributeIdentifier()));
		return ApiClient.getDisclosureJWT(
				list,
				IdealConfiguration.getInstance().getServerName(),
				IdealConfiguration.getInstance().getHumanReadableName(),
				IdealConfiguration.getInstance().getJwtAlgorithm(),
				IdealConfiguration.getInstance().getJwtPrivateKey()
		);
	}

	@GET
	@Path("/openTransactions-iDEAL")
	@Produces(MediaType.TEXT_PLAIN)
	public String openTransactions (){
		return OpenTransactions.getOpenTransactions();
	}


	@POST
	@Path("/start-iDEAL")
	@Consumes(MediaType.APPLICATION_FORM_URLENCODED)
	public Response start (String bank){
		if (!IdealConfiguration.getInstance().getIdealIssuers().containsBankCode(bank)){
			throw new RuntimeException("Illegal bankcode received");
		}
		// Create request
		String merchantReference = new BigInteger(130, random).toString(32);
		//iDIN lib wants the random MerchantReference to start with a letter.
		merchantReference = "a"+merchantReference;

		logger.info("Session started for bank {} with merchantReference {}", bank, merchantReference);

		// Execute request
		AuthenticationResponse response = new Connector();

		// Handle request result
		if (response.getIsError()) {
			logError(response.getErrorResponse());
			throw new IdinException(response.getErrorResponse());
		}
		logger.info("trxid {}: session created at bank, redirecting to {}",
				response.getTransactionID(),
				response.getIssuerAuthenticationURL());
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
	public Response authenticated(@DefaultValue("error") @QueryParam("trxid") String trxID){
		NewCookie[] cookies = new NewCookie[1];
		String followupURL = errorURL;

		boolean isHttpsEnabled = IdealConfiguration.getInstance().isHttpsEnabled();
		if (trxID.equals("error") ){
			//landing on the return page without a trxid. Something is wrong
			cookies[0] = new NewCookie("error","Something unexpected went wrong","/",null,null,60,isHttpsEnabled);
			followupURL = errorURL;
		} else {
			logger.info("trxid {}: return url called", trxID);

			StatusRequest sr = new StatusRequest(trxID);
			StatusResponse response = new Communicator().getResponse(sr);
			if (response.getIsError()) {
				logError(response.getErrorResponse());
				throw new IdinException(response.getErrorResponse());
			} else {
				logger.info("trxid {}: response status {}", trxID, response.getStatus());
				switch (response.getStatus()) {
					case StatusResponse.Success:
						Map<String, String> attributes = response.getSamlResponse().getAttributes();
						logger.info("trxid {}: BIN {}", trxID, attributes.get(idinSamlBinKey));
						if (nullOrEmptyAttributes(attributes,trxID)){
							cookies[0] = new NewCookie("error","De iDIN transactie leverde niet voldoende attributen op. Helaas kunnen wij hierdoor niet overgaan tot uitgifte van attributen","/",null,null,60,isHttpsEnabled);
							followupURL = errorURL;
						}else {
							//redirect to issuing page
							followupURL = successURL;
							IdinRecord.New(attributes.get(idinSamlBinKey));
							String jwt = createIssueJWT(attributes);
							cookies[0] = new NewCookie("jwt", jwt, "/", null, null, 600, isHttpsEnabled);
						}
						break;
					case StatusResponse.Cancelled:
						followupURL = errorURL;
						cookies[0] = new NewCookie("error", "De iDIN transactie is geannuleerd. Keer terug naar de iDIN issue pagina om het nog eens te proberen.", "/", null, null, 60, isHttpsEnabled);
						break;
					case StatusResponse.Expired:
						followupURL = errorURL;
						cookies[0] = new NewCookie("error", "De iDIN sessie is verlopen. Keer terug naar de iDIN issue pagina om het nog eens te proberen. Als dit probleem zich blijft voordoen, neem dan contact op met uw bank.", "/", null, null, 60, isHttpsEnabled);
						break;
					case StatusResponse.Open:
					case StatusResponse.Pending:
						OpenTransactions.addTransaction(trxID);
						break;
					case StatusResponse.Failure:
					default:
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


	private boolean isNullOrEmpty(String val){
		return val == null || val.length()==0;
	}
	private boolean nullOrEmptyAttributes (Map<String,String> attributes, String trxId){
		boolean valueMissing = false;
		if (isNullOrEmpty(attributes.get(idinSamlBirthdateKey))){
			valueMissing = true;
			logger.error("trxid {}: saml is missing birthdate value", trxId);
		}


		return valueMissing;
	}
*/
	private String createIssueJWT(Map<String, String> attributes) {
		HashMap<CredentialIdentifier, HashMap<String, String>> credentials = new HashMap<>();

		//get iDIN data credential
		HashMap<String,String> attrs = new HashMap<>();
		//attrs.put(IdinConfiguration.getInstance().getInitialsAttribute(), attributes.get(idinSamlInitialsKey));

		//add iDIN data credential
		credentials.put(new CredentialIdentifier(
				IdealConfiguration.getInstance().getSchemeManager(),
				IdealConfiguration.getInstance().getIdealIssuer(),
				IdealConfiguration.getInstance().getIbanCredential()
		), attrs);

		Calendar calendar = Calendar.getInstance();
		calendar.add(Calendar.YEAR, 1);

		IdentityProviderRequest iprequest = ApiClient.getIdentityProviderRequest(credentials, calendar.getTimeInMillis()/1000);

		return ApiClient.getSignedIssuingJWT(iprequest,
				IdealConfiguration.getInstance().getServerName(),
				IdealConfiguration.getInstance().getHumanReadableName(),
				IdealConfiguration.getInstance().getJwtAlgorithm(),
				IdealConfiguration.getInstance().getJwtPrivateKey()
		);

	}

}
