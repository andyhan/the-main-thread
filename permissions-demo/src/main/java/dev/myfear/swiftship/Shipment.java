package dev.myfear.swiftship;

import io.quarkus.hibernate.orm.panache.PanacheEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Table;

@Entity
@Table(name = "shipment")
public class Shipment extends PanacheEntity {

    @Column(name = "tracking_number")
    public String trackingNumber;

    @Column(name = "destination")
    public String destination;

    @Column(name = "status")
    public String status;
}