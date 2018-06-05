package interfaces.service;

import domain.Owner;
import dto.ThinInvoiceDto;
import exceptions.InvoiceException;
import interfaces.domain.IInvoice;

import javax.ejb.LocalBean;
import javax.ejb.Remote;
import javax.ejb.Stateless;
import java.util.ArrayList;

public interface IInvoiceService {

    IInvoice findInvoiceByInvoiceNumber(long invoiceNumber) throws InvoiceException;

    ArrayList<ThinInvoiceDto> findInvoiceByUser(Owner owner) throws InvoiceException;

    boolean payInvoice(long invoiceNumber, String paymentDetails) throws InvoiceException;

    void generateInvoices() throws InvoiceException;

    void generateInvoicesForVehiclesOfForeignCountries() throws InvoiceException;
}
