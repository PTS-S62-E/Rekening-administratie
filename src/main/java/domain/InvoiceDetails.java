package domain;

import com.pts62.common.europe.ITransLocation;
import exceptions.InvoiceException;
import interfaces.IInvoiceDetail;

import java.math.BigDecimal;
import java.util.ArrayList;

/**
 * ~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~
 * | Created by juleskreutzer
 * | Date: 20-03-18
 * |
 * | Project Info:
 * | Project Name: RekeningAdministratie
 * | Project Package Name: domain
 * ~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~
 */
public class InvoiceDetails implements IInvoiceDetail {

    private ArrayList<ITransLocation> locationPoints;
    private String description;
    private BigDecimal price;

    public InvoiceDetails() { }

    public InvoiceDetails(ArrayList<ITransLocation> locationPoints, String description, BigDecimal price) throws InvoiceException {
        if(locationPoints == null || locationPoints.size() < 1) { throw new InvoiceException("No Locationpoints provded for InvoiceDetails."); }
        if(description.isEmpty()) { throw new InvoiceException("Please provide a description for this InvoiceDetail."); }
        if(price == null || price.compareTo(BigDecimal.ZERO) < 0) { throw new InvoiceException("Please provide a positive price for this InvoiceDetail."); }

        this.locationPoints = locationPoints;
        this.description = description;
        this.price = price;
    }

    @Override
    public ArrayList<ITransLocation> locationPoints() {
        return this.locationPoints;
    }

    @Override
    public String description() {
        return this.description;
    }

    @Override
    public BigDecimal price() {
        return this.price;
    }
}