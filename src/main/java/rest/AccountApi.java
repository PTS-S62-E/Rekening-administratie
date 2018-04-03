package rest;

import com.fasterxml.jackson.databind.JsonNode;
import domain.Owner;
import exceptions.AccountException;
import service.AccountService;

import javax.ejb.EJB;
import javax.ejb.Stateless;
import javax.inject.Inject;
import javax.ws.rs.*;
import javax.ws.rs.core.Response;

import java.util.HashMap;
import java.util.Map;
import java.util.logging.Logger;

import static javax.ws.rs.core.MediaType.APPLICATION_JSON;

@Path("/accounts")
@Stateless
public class AccountApi {

    @EJB
    AccountService service;

    @POST
    @Path("/login")
    @Produces(APPLICATION_JSON)
    @Consumes(APPLICATION_JSON)
    public HashMap<String, Object> login(JsonNode data) {
        Logger logger = Logger.getLogger(getClass().getName());
        logger.warning(data.asText());
        if(data.get("email") == null || data.get("email").asText().isEmpty()) { throw new WebApplicationException(Response.Status.NOT_ACCEPTABLE); }
        if(data.get("password") == null || data.get("password").asText().isEmpty()) { throw new WebApplicationException(Response.Status.NOT_ACCEPTABLE); }

        String email = data.get("email").asText();
        String password = data.get("password").asText();

        try {
            Owner owner = service.login(email, password);
            if(owner == null) { throw new WebApplicationException(Response.Status.INTERNAL_SERVER_ERROR); }

            String token = service.generateJWT(email);

            HashMap<String, Object> result = new HashMap<String, Object>();

            result.put("token", token);
            result.put("owner", owner);

            return result;
        } catch (AccountException e) {
            throw new WebApplicationException(e.getMessage(), Response.Status.UNAUTHORIZED);
        }

    }
}
