package com.acabaaqui.resources;

import com.acabaaqui.services.ViaCepService;
import jakarta.inject.Inject;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.PathParam;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;

@Path("/addresses/cep")
@Produces(MediaType.APPLICATION_JSON)
public class ViaCepResource {

    @Inject
    ViaCepService viaCepService;

    @GET
    @Path("/{cep}")
    public Response consultar(@PathParam("cep") String cep) {
        try {
            return Response.ok(viaCepService.consultar(cep)).build();
        } catch (jakarta.ws.rs.WebApplicationException exception) {
            int status = exception.getResponse().getStatus();
            return Response.status(status)
                    .entity(new AddressResource.ErrorResponse(exception.getMessage())).build();
        }
    }
}
