package org.irmacard.ideal.web;

import com.ing.ideal.connector.IdealConnector;
import com.ing.ideal.connector.IdealException;
import com.ing.ideal.connector.Issuer;
import com.ing.ideal.connector.Transaction;
import org.irmacard.api.common.ApiClient;
import org.irmacard.api.common.AttributeDisjunction;
import org.irmacard.api.common.AttributeDisjunctionList;
import org.irmacard.api.common.issuing.IdentityProviderRequest;
import org.irmacard.credentials.info.CredentialIdentifier;
import org.javalite.common.Base64;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.ws.rs.*;
import javax.ws.rs.core.CacheControl;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import java.io.IOException;
import java.math.BigDecimal;
import java.util.*;

@Path("v1/ideal")
public class IdealResource {

	private static Logger logger = LoggerFactory.getLogger(IdealResource.class);

	@GET
	@Path("/banks")
	@Produces(MediaType.APPLICATION_JSON)
	public Response banks() throws IOException {
		logger.info("Bank list requested");
		CacheControl cc = new CacheControl();
		cc.setMaxAge(86400); // 1 day
		Map<String, List<Issuer>> issuers = IdealConfiguration.getInstance().getIdealIssuers().getIssuers();
		return Response.status(200).entity(issuers).cacheControl(cc).build();
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

	@POST
	@Path("/start")
	@Produces(MediaType.TEXT_PLAIN)
	public Response start (@FormParam("bank") String bank){
		if (!IdealConfiguration.getInstance().getIdealIssuers().containsBankCode(bank)){
			throw new RuntimeException("Illegal bankcode received");
		}
		try {
			Transaction transaction = new Transaction();
			transaction.setAmount(new BigDecimal("1.00"));
			transaction.setEntranceCode("ideal"); // code to resume the session
			transaction.setIssuerID(bank);
			transaction.setPurchaseID("1");
			transaction.setDescription("iDeal transactie ter authenticate");
			IdealConnector connector = new IdealConnector();
			transaction = connector.requestTransaction(transaction);
			String url = transaction.getIssuerAuthenticationURL();

			logger.info("trxid {}: session created at bank, redirecting to {}",
					transaction.getTransactionID(),
					transaction.getIssuerAuthenticationURL());
			return Response.status(Response.Status.OK).entity(url).build();
		} catch (IdealException e) {
			e.printStackTrace(); // TODO
			return Response.status(Response.Status.BAD_GATEWAY).entity(e.getConsumerMessage()).build();
		}
	}

	@POST
	@Path("/return")
	public Response authenticated(@DefaultValue("error") @FormParam("trxid") String trxID){
		Transaction response;
		try {
			IdealConnector connector = new IdealConnector();
			response = connector.requestTransactionStatus(trxID);
		} catch (IdealException e) {
			e.printStackTrace();
			return Response.status(Response.Status.BAD_GATEWAY).entity("consumermsg:" + e.getConsumerMessage()).build();
		}

		// The response we received was valid, but it may be something other
		// than "Success". Not sure when that happens though...
		if (!response.isSuccess()) {
			switch (response.getStatus()) {
				case "Cancelled":
				case "Expired":
					break;
				default:
					logger.error("Unexpected non-success status: " + response.getStatus());
			}
			return Response.status(Response.Status.BAD_GATEWAY).entity("ideal-status:" + response.getStatus()).build();
		}

		// Build the attributes.
		HashMap<String, String> attributes = new HashMap<>(3);
		attributes.put("fullname", response.getConsumerName());
		attributes.put("iban", response.getConsumerIBAN());
		attributes.put("bic", response.getConsumerBIC());

		// Build the credential.
		HashMap<CredentialIdentifier, HashMap<String, String>> credentials = new HashMap<>();
		credentials.put(new CredentialIdentifier(
				IdealConfiguration.getInstance().getSchemeManager(),
				IdealConfiguration.getInstance().getIdealIssuer(),
				IdealConfiguration.getInstance().getIbanCredential()
		), attributes);
		Calendar calendar = Calendar.getInstance();
		calendar.add(Calendar.YEAR, 1);
		IdentityProviderRequest iprequest = ApiClient.getIdentityProviderRequest(credentials, calendar.getTimeInMillis()/1000);

		// Build a JWT to request this credential.
		String jwt = ApiClient.getSignedIssuingJWT(iprequest,
				IdealConfiguration.getInstance().getServerName(),
				IdealConfiguration.getInstance().getHumanReadableName(),
				IdealConfiguration.getInstance().getJwtAlgorithm(),
				IdealConfiguration.getInstance().getJwtPrivateKey()
		);

		byte[] rawToken = IdinResource.makeToken(response.getConsumerBIC(), response.getConsumerIBAN());
		String token = Base64.getUrlEncoder().withoutPadding().encodeToString(rawToken);

		IdealApplication.openDatabase();
		IdinToken rec = new IdinToken();
		rec.set("hashedToken", IdinResource.hashToken(rawToken));
		rec.saveIt();

		byte[] rawSignature = IdinResource.signToken(rawToken);
		String signature = Base64.getUrlEncoder().withoutPadding().encodeToString(rawSignature);
		String signedToken = token + ":" + signature;

		IdealSuccessResponse entity = new IdealSuccessResponse();
		entity.jwt = jwt;
		entity.token = signedToken;
		return Response.status(Response.Status.OK).entity(entity).build();
	}
}
