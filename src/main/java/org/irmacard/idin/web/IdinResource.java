package org.irmacard.idin.web;

import net.bankid.merchant.library.*;

import javax.ws.rs.Consumes;
import javax.ws.rs.POST;
import javax.ws.rs.Path;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import java.math.BigInteger;
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

	private static Random random = new Random();

	@POST
	@Path("/start")
	@Consumes(MediaType.TEXT_PLAIN)
	public Response start(String bank) {
		// Create request
		String transactionId = new BigInteger(130, random).toString(32);
		String merchantReference = new BigInteger(130, random).toString(32);
		AuthenticationRequest request = new AuthenticationRequest(
				transactionId, IDIN_ATTRIBUTES, bank, AssuranceLevel.Loa3, null, merchantReference);

		// Execute request
		AuthenticationResponse response = new Communicator().newAuthenticationRequest(request);

		// Handle request result
		if (response.getIsError())
			throw new RuntimeException(response.getErrorResponse().getConsumerMessage());
		return Response.accepted(response.getIssuerAuthenticationURL()).build();
	}
}
