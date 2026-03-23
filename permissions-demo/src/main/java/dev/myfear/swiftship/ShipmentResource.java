package dev.myfear.swiftship;

import java.util.List;

import io.quarkus.hibernate.orm.rest.data.panache.PanacheEntityResource;
import io.quarkus.panache.common.Page;
import io.quarkus.panache.common.Sort;
import io.quarkus.security.PermissionsAllowed;

public interface ShipmentResource extends PanacheEntityResource<Shipment, Long> {

    @Override
    @PermissionsAllowed("shipment:read")
    List<Shipment> list(Page page, Sort sort);

    @Override
    @PermissionsAllowed("shipment:read")
    long count();

    @Override
    @PermissionsAllowed("shipment:read")
    Shipment get(Long id);

    @Override
    @PermissionsAllowed("shipment:admin")
    Shipment add(Shipment shipment);

    @Override
    @PermissionsAllowed("shipment:admin")
    Shipment update(Long id, Shipment shipment);

    @Override
    @PermissionsAllowed("shipment:admin")
    boolean delete(Long id);
}
