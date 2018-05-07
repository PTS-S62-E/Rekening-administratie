package service;

import communication.RegistrationMovement;
import dao.InvoiceDao;
import domain.Invoice;
import domain.InvoiceDetails;
import domain.Owner;
import domain.Ownership;
import dto.AdministrationDto;
import dto.JourneyDto;
import dto.TranslocationDto;
import exceptions.CommunicationException;
import exceptions.InvoiceException;
import exceptions.OwnerException;
import interfaces.domain.IInvoice;
import interfaces.domain.IInvoiceDetail;
import interfaces.service.IInvoiceService;
import io.sentry.Sentry;
import util.LocalDateUtil;

import javax.ejb.EJB;
import javax.ejb.LocalBean;
import javax.ejb.Stateless;
import javax.faces.bean.SessionScoped;
import javax.inject.Inject;
import java.io.IOException;
import java.math.BigDecimal;
import java.util.ArrayList;

@Stateless
@LocalBean
public class InvoiceService implements IInvoiceService {

    @EJB
    InvoiceDao invoiceDao;

    @EJB
    OwnerService ownerService;

    public InvoiceService() { }

    /**
     * Find an invoice based on the invoiceNumber
     * @param invoiceNumber number of the invoice that it should return
     * @return returns the invoice, if found, else null (probably InvoiceException)
     * @throws InvoiceException thrown when the invoice couldn't be found
     */
    @Override
    public IInvoice findInvoiceByInvoiceNumber(long invoiceNumber) throws InvoiceException {
        if(invoiceNumber < 1) { throw new InvoiceException("Please provide an invoice number"); }

        return invoiceDao.findInvoiceByInvoiceNumer(invoiceNumber);
    }

    @Override
    public ArrayList<IInvoice> findInvoiceByUser(long userId) throws InvoiceException {
        if(userId < 0) { throw new InvoiceException("Please provide a valid userId"); }

        return invoiceDao.findInvoiceByUser(userId);
    }

    @Override
    public boolean payInvoice(long invoiceNumber, String paymentDetails) throws InvoiceException {
        if(invoiceNumber < 1) { throw new InvoiceException("Please provide an invoice number"); }
        if(paymentDetails.isEmpty()) { throw new InvoiceException("Please provide payment details"); }

        return invoiceDao.payInvoice(invoiceNumber, paymentDetails);
    }

    /**
     * Generate all the god damn invoices
     *
     * This method will fetch all owners from the database and will loop through them. Within the loop we get the ownerships for the user.
     * If the user has a current ownership for a vehicle, we get all translocations for the vehicle from an external API and the process these
     * translocations.
     *
     * When the translocations are processed, an array of new of InvoiceDetails will be created. All these object represent the routes that are visible
     * on the invoice.
     *
     * This method is usually called using a message queue. With a small number of users, the invoice generation could be done fast, but with
     * a growing number of users, it's a better alternative to use a message queue.
     * @throws InvoiceException
     */
    @Override
    public void generateInvoices() throws InvoiceException {
        //TODO: Check if the translocation already belongs to an invoice
        try {
            ArrayList<Owner> owners = (ArrayList<Owner>) ownerService.getAllOwners();

            for(Owner owner : owners) {
                ArrayList<Ownership> ownerships = (ArrayList<Ownership>) owner.getOwnership();

                if(!ownerships.isEmpty()) {
                    for (Ownership ownership : ownerships) {
                        long vehicleId = ownership.getVehicleId();
                        AdministrationDto administrationDto = RegistrationMovement.getInstance().getTranslocationsForVehicleId(vehicleId, LocalDateUtil.getCurrentDate(), LocalDateUtil.getCurrentDateMinusOneMonth());

                        ArrayList<InvoiceDetails> invoiceDetails = new ArrayList<>();
                        for (JourneyDto journey : administrationDto.getJourneys()) {
                            InvoiceDetails details = new InvoiceDetails((ArrayList<TranslocationDto>) journey.getTranslocations(), "Complete Journey", new BigDecimal(10.0));
                            invoiceDetails.add(details);
                        }

                        invoiceDao.createInvoice(invoiceDetails, owner, vehicleId);
                    }
                }
            }
        } catch (OwnerException e) {
            throw new InvoiceException(e.getMessage());
        } catch (IOException | CommunicationException e) {
            Sentry.capture(e);
            throw new InvoiceException("couldn't generate invoice.");
        }

    }

    private boolean checkIfTranslocationIsProcessed(long translocationId, Invoice invoice) {
        for(InvoiceDetails details : invoice.getInvoiceDetails()) {
            if(details.getLocationPointsIds().contains(translocationId)) { return true; }
        }

        return false;
    }
}
