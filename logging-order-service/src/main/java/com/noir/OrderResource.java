package com.noir;

import org.jboss.logging.Logger;

import com.noir.audit.AuditService;
import com.noir.payments.PaymentService;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import jakarta.ws.rs.Consumes;
import jakarta.ws.rs.POST;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;

@Path("/orders")
@ApplicationScoped
public class OrderResource {

    private static final Logger LOG = Logger.getLogger(OrderResource.class);

    @Inject
    PaymentService paymentService;

    @Inject
    AuditService auditService;

    @POST
    @Consumes(MediaType.APPLICATION_JSON)
    @Produces(MediaType.APPLICATION_JSON)
    public Response placeOrder(OrderRequest request) {

        LOG.infof("Order received: orderId=%s userId=%s amount=%.2f",
                request.orderId(), request.userId(), request.amount());

        paymentService.charge(request.orderId(), request.amount());

        auditService.auditOrder(request.orderId(), request.userId());

        LOG.debugf("Order processing complete: orderId=%s",
                request.orderId());

        return Response.ok().build();
    }
}