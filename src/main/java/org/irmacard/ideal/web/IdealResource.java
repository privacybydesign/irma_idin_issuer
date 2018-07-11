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
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.ws.rs.*;
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

*/
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
			transaction.setEntranceCode("test");
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
			logger.error("Unexpected non-success status: " + response.getStatus());
			return Response.status(Response.Status.BAD_GATEWAY).entity("status:" + response.getStatus()).build();
		}

		// Build the attributes.
		HashMap<String, String> attributes = new HashMap<>(3);
		attributes.put("name", response.getConsumerName());
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

		return Response.status(Response.Status.OK).entity(jwt).build();
	}
}
