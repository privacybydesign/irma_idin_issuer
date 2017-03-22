package org.irmacard.idin.web;

import net.bankid.merchant.library.*;
import net.bankid.merchant.library.internal.DirectoryResponseBase;
import org.slf4j.*;

import javax.ws.rs.*;
import javax.ws.rs.core.*;
import javax.ws.rs.QueryParam;
import java.io.IOException;
import java.math.BigInteger;
import java.net.URI;
import java.net.URISyntaxException;
import java.util.*;

@Path("v1/idin")
public class IdinResource {
	public final static int IDIN_ATTRIBUTES = ServiceIds.Address
			| ServiceIds.ConsumerBin
			| ServiceIds.DateOfBirth
			| ServiceIds.Email
			| ServiceIds.Gender
			| ServiceIds.Telephone
			| ServiceIds.Name;

	private static Random random = new Random();
	private static Logger logger = LoggerFactory.getLogger(IdinResource.class);

	private static String returnURL = "http://localhost:8080/irma_idin_server/enroll.html";
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
		logger.warn("called it!");
		logger.warn(bank);

		// Create request
		String merchantReference = new BigInteger(130, random).toString(32);
		//iDIN lib wants the random MerchantReference to start with a letter.
		merchantReference = "a"+merchantReference;
		logger.error(">>>" + merchantReference);
		AuthenticationRequest request = new AuthenticationRequest("successHIO100OIHtest",IDIN_ATTRIBUTES,bank,AssuranceLevel.Loa3,"nl",merchantReference);

		// Execute request
		AuthenticationResponse response = new Communicator().newAuthenticationRequest(request);

		// Handle request result
		if (response.getIsError())
			throw new RuntimeException(response.getErrorResponse().getConsumerMessage());
		return Response.accepted(response.getIssuerAuthenticationURL()).build();
	}

	@GET
	@Path("/return")
	@Consumes(MediaType.APPLICATION_FORM_URLENCODED)
	public Response authenticated(@DefaultValue("error") @QueryParam("trxid") String trxID){
		NewCookie[] cookies = new NewCookie[1];

		if (trxID.equals("error") ){
			//TODO show error page
		}
		StatusRequest sr = new StatusRequest(trxID);
		StatusResponse response = new Communicator().getResponse(sr);

		if (response.getIsError()){
			logger.error(response.getErrorResponse().getErrorMessage());
			//TODO: show error page to user
		} else if (response.getStatus().equals(StatusResponse.Success)){
			//redirect to issuing page
			Map<String, String> attributes = response.getSamlResponse().getAttributes();
			cookies[0] = new NewCookie("city", attributes.get("urn:nl:bvn:bankid:1.0:consumer.city"),"/",null,null,60, isHttpsEnabled);
			//for (Map.Entry<String, String> entry : attributes.entrySet())
			//{
			//	logger.info(entry.getKey() + "/" + entry.getValue());
			//}
		}
		try {
			URI issueURI = new URI(returnURL);
			return Response.seeOther(issueURI).cookie(cookies).build();
		} catch (URISyntaxException e) {
			e.printStackTrace();
		}
		return null;
	}
}
