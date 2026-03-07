package com.example;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.List;

import org.junit.jupiter.api.Test;

import io.quarkus.test.InjectMock;
import io.quarkus.test.component.QuarkusComponentTest;
import jakarta.inject.Inject;

@QuarkusComponentTest
class OrderServiceComponentTest {

    @Inject
    OrderService orderService;

    @InjectMock
    OrderRepository repository;

    @InjectMock
    OrderAudit orderAudit;

    @Test
    void findOrdersForCustomer() {
        when(repository.findByCustomer("customer-42"))
                .thenReturn(List.of(new Order("customer-42", "monitor")));

        List<Order> orders = orderService.findByCustomer("customer-42");

        assertThat(orders).hasSize(1);
    }

    @Test
    void createOrder_persistsAndRecordsAudit() {
        doAnswer(invocation -> {
            Order o = invocation.getArgument(0);
            o.id = 1L;
            return null;
        }).when(repository).persist(any(Order.class));

        Order order = orderService.create("customer-1", "tablet");

        assertThat(order.id).isEqualTo(1L);
        assertThat(order.customerId).isEqualTo("customer-1");
        assertThat(order.product).isEqualTo("tablet");
        verify(repository).persist(any(Order.class));
        verify(orderAudit).recordCreated(order);
    }
}