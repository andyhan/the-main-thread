package com.example;

import java.util.List;
import java.util.UUID;

import jakarta.transaction.Transactional;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.PathParam;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.core.MediaType;

@Path("/orders")
public class OrderResource {

    @GET
    @Path("/create")
    @Transactional
    public String createOrder() {
        OrderEntity order = new OrderEntity();
        order.customerId = UUID.randomUUID().toString();
        order.amount = Math.random() * 1000;
        order.persist();
        return "ok";
    }

    @GET
    @Path("/find")
    public long findOrders() {
        return OrderEntity.count("amount > 100");
    }

    // SLOW QUERY 1: Full table scan with LIKE on unindexed column
    @GET
    @Path("/search/{customerId}")
    @Produces(MediaType.APPLICATION_JSON)
    public List<OrderEntity> searchByCustomerId(@PathParam("customerId") String customerId) {
        // This will do a full table scan because customerId is not indexed
        // and LIKE with leading wildcard prevents index usage
        return OrderEntity.list("customerId LIKE ?1", "%" + customerId + "%");
    }

    // SLOW QUERY 2: Complex aggregation without proper indexes
    @GET
    @Path("/expensive-aggregation")
    @Produces(MediaType.APPLICATION_JSON)
    public long expensiveAggregation() {
        // This performs multiple aggregations on unindexed columns
        // Counts customers who have more than one order with amount > 100
        return OrderEntity.count(
            "SELECT COUNT(DISTINCT customerId) " +
            "FROM OrderEntity " +
            "WHERE amount > 100 " +
            "GROUP BY customerId " +
            "HAVING COUNT(*) > 1"
        );
    }

    // SLOW QUERY 3: Range query on unindexed column
    @GET
    @Path("/range/{min}/{max}")
    @Produces(MediaType.APPLICATION_JSON)
    public List<OrderEntity> findByAmountRange(
            @PathParam("min") double min,
            @PathParam("max") double max) {
        // Range queries on unindexed columns require full table scan
        return OrderEntity.list("amount BETWEEN ?1 AND ?2", min, max);
    }

    // SLOW QUERY 4: Sorting on unindexed column with large result set
    @GET
    @Path("/sorted-by-amount")
    @Produces(MediaType.APPLICATION_JSON)
    public List<OrderEntity> sortedByAmount() {
        // Sorting requires loading all data and sorting in memory
        // without an index on amount
        return OrderEntity.list("ORDER BY amount DESC");
    }

    // Helper: Bulk create for testing
    @GET
    @Path("/bulk-create/{count}")
    @Transactional
    public String bulkCreate(@PathParam("count") int count) {
        for (int i = 0; i < count; i++) {
            OrderEntity order = new OrderEntity();
            order.customerId = UUID.randomUUID().toString();
            order.amount = Math.random() * 1000;
            order.persist();
        }
        return "Created " + count + " orders";
    }
}