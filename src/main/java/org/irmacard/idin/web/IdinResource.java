package org.irmacard.idin.web;

import javax.ws.rs.*;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;

@Path("v1/idin")
public class IdinResource {
	@GET
	@Path("/start")
	public Response start() {
		// TODO
		return Response.accepted("OK").build();
	}
}
