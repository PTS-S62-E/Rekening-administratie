package rest;

import com.pts62.common.finland.util.JsonExceptionMapper;
import domain.Owner;
import domain.Ownership;
import dto.OwnershipWithVehicleDto;
import exceptions.OwnershipException;
import io.sentry.Sentry;
import service.OwnershipService;
import util.jwt.JWTRequired;
import javax.ejb.EJB;
import javax.ejb.Stateless;
import javax.json.Json;
import javax.ws.rs.*;
import javax.ws.rs.core.Response;
import java.util.ArrayList;

import static javax.ws.rs.core.MediaType.APPLICATION_JSON;

@Path("/vehicles")
@Stateless
public class VehicleApi {

    @EJB
    private OwnershipService ownershipService;

    @GET
    @Path("id/{id}/history/ownership")
    @JWTRequired
    @Produces(APPLICATION_JSON)
    public ArrayList<Owner> getVehicleOwnershipHistory(@PathParam("id") long vehicleId) {
        if(vehicleId < 1) { throw new WebApplicationException(Response.status(Response.Status.NOT_ACCEPTABLE).entity("Please provide a valid vehicleId").build()); }

        try {
            ArrayList<Ownership> ownerships = ownershipService.findOwnershipByVehicleId(vehicleId);

            ArrayList<Owner> result = new ArrayList<>();

            for(Ownership os : ownerships) {
                result.add(os.getOwner());
            }

            return result;
        } catch (OwnershipException e) {
            Sentry.capture(e);
            throw JsonExceptionMapper.mapException(Response.Status.INTERNAL_SERVER_ERROR, e.getMessage());
        }
    }

    @GET
    @Path("licensePlate/{licensePlate}/history/ownership")
    @Produces(APPLICATION_JSON)
    @JWTRequired
    public OwnershipWithVehicleDto getOwnershipWithVehicleDto(@PathParam("licensePlate") String licensePlate) {

        if(licensePlate == null || licensePlate.equals("")) {
            throw JsonExceptionMapper.mapException(Response.Status.NOT_ACCEPTABLE, "Please provide a valid licenseplate");
        }

        try {
            return ownershipService.findOwnershipByLicensePlate(licensePlate);
        } catch (Exception e) {
            Sentry.capture(e);
            throw JsonExceptionMapper.mapException(Response.Status.INTERNAL_SERVER_ERROR, e.getMessage());
        }
    }
}
