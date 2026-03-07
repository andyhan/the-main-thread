package com.example;

import java.util.List;

import jakarta.inject.Inject;
import jakarta.ws.rs.Consumes;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.POST;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.QueryParam;
import jakarta.ws.rs.core.MediaType;

@Path("/orders")
@Produces(MediaType.APPLICATION_JSON)
@Consumes(MediaType.APPLICATION_JSON)
public class OrderResource {

    @Inject
    OrderService orderService;

    @POST
    public Order create(Order order) {
        return orderService.create(order.customerId, order.product);
    }

    @GET
    public List<Order> list(@QueryParam("customerId") String customerId) {
        return orderService.findByCustomer(customerId);
    }
}