package domain;

import javax.persistence.*;
import java.io.Serializable;
import java.util.ArrayList;

@Entity
@Table(name = "Owner")
@NamedQueries({
        @NamedQuery(name = "Owner.findById", query = "SELECT o FROM Owner o WHERE o.id = :id")
})
public class Owner implements Serializable {

    @Id
    @GeneratedValue(strategy = GenerationType.AUTO)
    private long id;

    @Column(name = "name")
    private String name;

    @Column(name = "address")
    private String address;

    @Column(name = "city")
    private String city;

    @Column(name = "postalCode")
    private String postalCode;

    @OneToMany(mappedBy = "owner")
    private ArrayList<Ownership> ownership;

    public Owner() { }

    public long getId() {
        return id;
    }

    public void setId(long id) {
        this.id = id;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getAddress() {
        return address;
    }

    public void setAddress(String address) {
        this.address = address;
    }

    public String getCity() {
        return city;
    }

    public void setCity(String city) {
        this.city = city;
    }

    public String getPostalCode() {
        return postalCode;
    }

    public void setPostalCode(String postalCode) {
        this.postalCode = postalCode;
    }

    public ArrayList<Ownership> getOwnership() {
        return ownership;
    }

    public void setOwnership(ArrayList<Ownership> ownership) {
        this.ownership = ownership;
    }

    public void setOwnership(Ownership ownership) {
        this.ownership.add(ownership);
    }
}