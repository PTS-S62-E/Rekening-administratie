package dao;

import com.rekeningrijden.europe.dtos.SubInvoiceDto;
import communication.RegistrationMovement;
import domain.Invoice;
import domain.InvoiceDetails;
import domain.Owner;
import dto.VehicleDto;
import exceptions.CommunicationException;
import exceptions.InvoiceException;
import exceptions.OwnershipException;
import interfaces.domain.IInvoice;
import interfaces.dao.IInvoiceDao;
import interfaces.domain.IInvoiceDetail;
import io.sentry.Sentry;
import service.OwnerService;
import service.OwnershipService;
import util.LocalDateUtil;

import javax.ejb.EJB;
import javax.ejb.LocalBean;
import javax.ejb.Stateless;
import javax.enterprise.context.ApplicationScoped;
import javax.inject.Inject;
import javax.inject.Named;
import javax.persistence.EntityManager;
import javax.persistence.NoResultException;
import javax.persistence.PersistenceContext;
import javax.persistence.Query;
import java.io.IOException;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;


/**
 * ~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~
 * | Created by juleskreutzer
 * | Date: 20-03-18
 * |
 * | Project Info:
 * | Project Name: RekeningAdministratie
 * | Project Package Name: dao
 * ~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~
 */

@Stateless
@LocalBean
public class InvoiceDao implements IInvoiceDao {

    @PersistenceContext(unitName = "administratieUnit")
    EntityManager em;

    private final String countryCode = "FI";

    public InvoiceDao() { }

    @EJB
    private OwnershipService ownershipService;


    @Override
    public boolean createInvoice(Invoice invoice) throws InvoiceException {
        if(invoice == null) { throw new InvoiceException("Please provide an invoice"); }
        if(invoice.invoiceDetails() == null || invoice.invoiceDetails().size() < 1) { throw new InvoiceException("No Invoice details found."); }
        if(invoice.getCountry() == null || invoice.getCountry().isEmpty()) { throw new InvoiceException("No country code provided in Invoice."); }
        if(invoice.getInvoiceDate() == null) { throw new InvoiceException("No InvoiceDate provided in Invoice."); }
        if (invoice.getPrice() < 0) {
            throw new InvoiceException("No positive price provided in Invoice");
        }
        try {
            for(InvoiceDetails detail : invoice.getInvoiceDetails()) {
                em.persist(detail);
                em.flush();
            }
            em.persist(invoice);
            em.flush();
        } catch(Exception e) {
            Sentry.capture(e);
            throw new InvoiceException(e.getMessage());
        }

        return true;
    }

    @Override
    public boolean createInvoice(ArrayList<InvoiceDetails> invoiceDetails, Owner owner, long vehicleId) throws InvoiceException {
        if(invoiceDetails == null || invoiceDetails.size() < 1) { throw new InvoiceException("Please provide invoiceDetails to generate the invoice."); }

        Invoice invoice = new Invoice(invoiceDetails, this.countryCode, LocalDateUtil.getCurrentDate(), owner, vehicleId);

        this.createInvoice(invoice);

        return true;

    }

    @Override
    public boolean createInvoice(ArrayList<InvoiceDetails> invoiceDetails, Owner owner, long vehicleId, String countryCode) throws InvoiceException {
        if(invoiceDetails == null || invoiceDetails.size() < 1) { throw new InvoiceException("Please provide invoiceDetails to generate the invoice."); }
        if(countryCode == null || countryCode.isEmpty()) { throw new InvoiceException("Please provide the country for this invoice."); }

        LocalDateTime currentDate = LocalDateTime.now();

        Invoice invoice = new Invoice(invoiceDetails, countryCode, currentDate.toString(), owner, vehicleId);

        this.createInvoice(invoice);

        return true;
    }

    @Override
    public boolean createInvoice(ArrayList<InvoiceDetails> invoiceDetails, Owner owner, long vehicleId, String countryCode, LocalDateTime invoiceDate) throws InvoiceException {
        if(invoiceDetails == null || invoiceDetails.size() < 1) { throw new InvoiceException("Please provide invoiceDetails to generate the invoice."); }
        if(countryCode == null || countryCode.isEmpty()) { throw new InvoiceException("Please provide the country for this invoice."); }
        if(invoiceDate == null) { throw new InvoiceException("Please provide an Invoice date."); }

        Invoice invoice = new Invoice(invoiceDetails, countryCode, invoiceDate.toString(), owner, vehicleId);

        this.createInvoice(invoice);

        return true;
    }

    @Override
    public boolean createExternalInvoice(SubInvoiceDto subInvoiceDto) throws InvoiceException {
        if(subInvoiceDto == null) { throw new InvoiceException("Please provide an external Invoice object"); }

        try {
            VehicleDto vehicleDto = RegistrationMovement.getInstance().getVehicleBySerialNumber(subInvoiceDto.getCarTrackerId());

            Owner owner;
            try {
                owner = ownershipService.findOwnershipByVehicleId(vehicleDto.getId()).get(0).getOwner();
            } catch (OwnershipException e) {
                throw new InvoiceException(e.getMessage());
            }

            Invoice invoice = new Invoice(subInvoiceDto.getCountry(), subInvoiceDto.getInvoiceDate(), owner, vehicleDto.getId(), subInvoiceDto.getPrice());


            return this.createInvoice(invoice);
        } catch (CommunicationException | IOException e) {
            throw new InvoiceException(e.getMessage());
        }
    }

    @Override
    public IInvoice findInvoiceByInvoiceNumer(long invoiceNumber) throws InvoiceException {
        if(invoiceNumber < 1) { throw new InvoiceException("Please provide an Invoice number."); }

        Query q = em.createNamedQuery("Invoice.findByInvoiceNumber");
        q.setParameter("id", invoiceNumber);

        try {
            return(IInvoice) q.getSingleResult();
        } catch(NoResultException nre) {
            throw new InvoiceException("Couldn't find invoice with provided invoice number");
        }
    }

    @Override
    public List<Object[]> findInvoiceByUser(long userId) throws InvoiceException {
        if(userId < 1) { throw new InvoiceException("Please provide a valid userId"); }

        Query q = em.createNamedQuery("Invoice.findThinInvoiceByUserId");
        q.setParameter("userId", userId);

        try {
            return (List<Object[]>) q.getResultList();
        } catch(NoResultException nre) {
            throw new InvoiceException("Couldn't find invoice(s) for provided user");
        }
    }

    @Override
    public List<Object[]> findInvoicesByVehicleId(long vehicleId) throws InvoiceException {
        if(vehicleId < 1) { throw new InvoiceException("Please provie a valid vehicleId"); }

        Query q = em.createNamedQuery("Invoice.findThinInvoiceByVehicleId");
        q.setParameter("vehicleId", vehicleId);

        try {
            return (List<Object[]>) q.getResultList();
        } catch (NoResultException nre) {
            throw new InvoiceException("Couldn't find invoice(s) for provided vehicleId");
        }
    }

    public ArrayList<IInvoice> findFullInvoiceByUser(long userId) throws InvoiceException {
        if(userId < 1) { throw new InvoiceException("Please provide a valid userId"); }

        Query q = em.createNamedQuery("Invoice.findByUserId");
        q.setParameter("userId", userId);

        try {
            return (ArrayList<IInvoice>) q.getResultList();
        } catch(NoResultException nre) {
            throw new InvoiceException("Couldn\t find invoice(s) for provided user");
        }
    }

    @Override
    public ArrayList<IInvoice> findInvoicesByUserAndVehicleId(long userId, long vehicleId) throws InvoiceException {
        if(userId < 1) { throw new InvoiceException("Please provide a userId"); }
        if(vehicleId < 1) { throw new InvoiceException("Please provide a vehicleId"); }

        Query q = em.createNamedQuery("Invoice.findInvoiceByUserIdAndVehicleId");
        q.setParameter("userId", userId);
        q.setParameter("vehicleId", vehicleId);

        try {
            return (ArrayList<IInvoice>) q.getResultList();
        } catch(NoResultException nre) {
            throw new InvoiceException("Couldn't find any invoices with provided userId and vehicleId");
        }
    }

    @Override
    public boolean payInvoice(long invoiceNumber, String paymentDetails) throws InvoiceException {
        if(invoiceNumber < 1) { throw new InvoiceException("Please provide an invoice number"); }
        if(paymentDetails.isEmpty()) { throw new InvoiceException("Please provide payment details"); }

        IInvoice invoice = this.findInvoiceByInvoiceNumer(invoiceNumber);
        invoice.setPaymentStatus(true);
        invoice.setPaymentDetails(paymentDetails);

        em.merge(invoice);

        return true;
    }
}
