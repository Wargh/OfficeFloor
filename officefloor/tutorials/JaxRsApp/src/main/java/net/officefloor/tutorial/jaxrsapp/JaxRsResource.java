package net.officefloor.tutorial.jaxrsapp;

import javax.ws.rs.Consumes;
import javax.ws.rs.GET;
import javax.ws.rs.POST;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.Produces;
import javax.ws.rs.core.MediaType;

/**
 * JAX-RS resource.
 * 
 * @author Daniel Sagenschneider
 */
@Path("/jaxrs")
public class JaxRsResource {

	@GET
	@Produces(MediaType.TEXT_PLAIN)
	public String get() {
		return "GET";
	}

	@GET
	@Path("/path/{param}")
	public String pathParam(@PathParam("param") String param) {
		return param;
	}

	@POST
	@Path("/json")
	@Consumes("application/json")
	@Produces("application/json")
	public JsonResponse json(JsonRequest request) {
		return new JsonResponse(request.getInput());
	}

}