package rest;

import com.fasterxml.jackson.core.io.JsonEOFException;
import com.fasterxml.jackson.databind.JsonNode;
import com.pts62.common.finland.util.JsonExceptionMapper;
import domain.Account;
import domain.Owner;
import domain.Ownership;
import exceptions.AccountException;
import exceptions.OwnerException;
import exceptions.OwnershipException;
import io.sentry.Sentry;
import service.AccountService;
import service.OwnerService;
import service.OwnershipService;
import util.HashUtility;
import util.jwt.JWTRequired;
import util.jwt.JWTUtility;

import javax.ejb.EJB;
import javax.ejb.Stateless;
import javax.inject.Inject;
import javax.ws.rs.*;
import javax.ws.rs.core.Response;

import java.text.ParseException;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.logging.Logger;

import static javax.ws.rs.core.MediaType.APPLICATION_JSON;

@Path("/accounts")
@Stateless
public class AccountApi {

    @EJB
    AccountService service;

    @EJB
    OwnerService ownerService;

    @EJB
    OwnershipService ownershipService;

    @POST
    @Path("/login")
    @Produces(APPLICATION_JSON)
    @Consumes(APPLICATION_JSON)
    public HashMap<String, Object> login(JsonNode data) {
        if(data == null) { throw JsonExceptionMapper.mapException(Response.Status.UNAUTHORIZED, ""); }
        if(data.get("email") == null || data.get("email").asText().isEmpty()) { throw JsonExceptionMapper.mapException(Response.Status.NOT_ACCEPTABLE, "Please provide an email address"); }
        if(data.get("password") == null || data.get("password").asText().isEmpty()) { throw JsonExceptionMapper.mapException(Response.Status.NOT_ACCEPTABLE, "Please provide a password"); }

        String email = data.get("email").asText();
        String password = data.get("password").asText();

        String hashedPassword = HashUtility.hash(password);

        try {
            Owner owner = service.login(email, hashedPassword);
            if(owner == null) { throw new WebApplicationException(Response.Status.INTERNAL_SERVER_ERROR); }

            String token = service.generateJWT(email);

            HashMap<String, Object> result = new HashMap<String, Object>();

            result.put("token", token);
            result.put("owner", owner);

            return result;
        } catch (AccountException e) {
            throw JsonExceptionMapper.mapException(Response.Status.UNAUTHORIZED, e.getMessage());
        }

    }

    @POST
    @Path("/")
    @Consumes(APPLICATION_JSON)
    public boolean createAccount(JsonNode data) {
        HashMap<String, Object> result = new HashMap<String, Object>();
        // Based on the provided JsonNode data, we should be able to create a new Owner and Account instance;
        if(data == null) { throw JsonExceptionMapper.mapException(Response.Status.NOT_ACCEPTABLE, "PLease provide some data"); }
        if(data.get("email") == null || data.get("email").asText().isEmpty()) { throw JsonExceptionMapper.mapException(Response.Status.NOT_ACCEPTABLE, "Please provide an email address"); }
        if(data.get("password") == null || data.get("password").asText().isEmpty()) { throw JsonExceptionMapper.mapException(Response.Status.NOT_ACCEPTABLE, "Please provide a password");}
        if(data.get("name") == null || data.get("name").asText().isEmpty()) { throw JsonExceptionMapper.mapException(Response.Status.NOT_ACCEPTABLE, "Please provide a name"); }
        if(data.get("address") == null || data.get("address").asText().isEmpty()) { throw JsonExceptionMapper.mapException(Response.Status.NOT_ACCEPTABLE, "Please provide an address"); }
        if(data.get("city") == null || data.get("city").asText().isEmpty()) { throw JsonExceptionMapper.mapException(Response.Status.NOT_ACCEPTABLE, "Please provide a city"); }
        if(data.get("postalCode") == null || data.get("postalCode").asText().isEmpty()) { throw JsonExceptionMapper.mapException(Response.Status.NOT_ACCEPTABLE, "Please provide a postal code"); }

        // Store everything we need in a variable
        String name = data.get("name").asText();
        String address = data.get("address").asText();
        String city = data.get("city").asText();
        String postalCode = data.get("postalCode").asText();
        String email = data.get("email").asText();
        String password = data.get("password").asText();

        String hashedPassword = HashUtility.hash(password);

        Owner owner = new Owner(name, address, city, postalCode);
        Account account = new Account(email, hashedPassword, owner);

        try {
            service.createAccount(account);
            return true;
        } catch (AccountException e) {
            Sentry.capture(e);
            throw JsonExceptionMapper.mapException(Response.Status.INTERNAL_SERVER_ERROR, e.getMessage());
        }
    }

    @GET
    @Path("/cars")
    @Produces(APPLICATION_JSON)
    public List<Ownership> getVehicleOwnerships(@HeaderParam("Authorization") String token) {
        if(token == null || token.isEmpty()) { throw JsonExceptionMapper.mapException(Response.Status.UNAUTHORIZED, "Please provide an access token."); }

        try {
            Account account = service.findByEmailAddress(JWTUtility.getSubject(token));

            return ownershipService.getFatOwnerships(account);
        } catch (AccountException | OwnershipException e) {
            throw JsonExceptionMapper.mapException(Response.Status.NOT_ACCEPTABLE, e.getMessage());
        }
    }

    @POST
    @Path("/cars")
    @Produces(APPLICATION_JSON)
    public void addOwnershipToUser(@HeaderParam("Authorization") String token, JsonNode data) {
        if(token == null || token.isEmpty()) { throw JsonExceptionMapper.mapException(Response.Status.UNAUTHORIZED, "Please provide an access token."); }
        if(data == null) { throw JsonExceptionMapper.mapException(Response.Status.NOT_ACCEPTABLE, "Nothing to process");}
        if(data.get("vehicleId") == null || data.get("vehicleId").asText().isEmpty()) { throw JsonExceptionMapper.mapException(Response.Status.NOT_ACCEPTABLE, "Please provide a vehicleId"); }
        if(data.get("fromDate") == null || data.get("fromDate").asText().isEmpty()) { throw JsonExceptionMapper.mapException(Response.Status.NOT_ACCEPTABLE, "Please provide a fromDate"); }
        // Didn't check if toDate is provided, because this value may be null

        DateTimeFormatter formatter = DateTimeFormatter.ofPattern("dd-MM-yyy");

        try {
            Account account = service.findByEmailAddress(JWTUtility.getSubject(token));

            long vehicleId = 0;
            try {
                vehicleId = Long.parseLong(data.get("vehicleId").asText());
            } catch(NumberFormatException e) {
                throw JsonExceptionMapper.mapException(Response.Status.INTERNAL_SERVER_ERROR, "vehicleId must be a parsable to a number");
            }

            String fromDateString = data.get("fromDate").asText();

            LocalDate fromDate = LocalDate.parse(fromDateString, formatter);
            LocalDate toDate = null;

            if(data.get("toDate") != null && !data.get("toDate").asText().isEmpty()) {
                String toDateString = data.get("toDate").asText();
                toDate = LocalDate.parse(toDateString, formatter);
            }

            Ownership ownership = new Ownership(account.getOwner(), vehicleId, fromDate, toDate);
            ownerService.addOwnership(account.getOwner(), ownership);


        } catch (AccountException | OwnerException | NumberFormatException e) {
            throw JsonExceptionMapper.mapException(Response.Status.INTERNAL_SERVER_ERROR, e.getMessage());
        } catch (Exception e) {
            Sentry.capture(e);
            throw JsonExceptionMapper.mapException(Response.Status.INTERNAL_SERVER_ERROR, e.getMessage());
        }


    }
}
