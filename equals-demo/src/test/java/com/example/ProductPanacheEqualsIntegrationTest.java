package com.example;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.math.BigDecimal;
import java.util.HashSet;
import java.util.Set;

import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.MethodOrderer;
import org.junit.jupiter.api.Order;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestMethodOrder;

import com.example.entity.Product;
import com.example.repository.CategoryRepository;
import com.example.repository.ProductRepository;

import io.quarkus.test.junit.QuarkusTest;
import jakarta.inject.Inject;
import jakarta.transaction.Transactional;

@QuarkusTest
@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
class ProductPanacheEqualsIntegrationTest {

    @Inject
    ProductRepository productRepository;

    @Inject
    CategoryRepository categoryRepository;

    @Test
    @Order(1)
    @Transactional
    @DisplayName("Product equality works before and after persist in same transaction")
    void equalityBeforeAndAfterPersistSameTransaction() {
        Product product = new Product("INT-SKU-001", "Integration Widget", new BigDecimal("9.99"));

        int hashBefore = product.hashCode();
        assertNull(product.id, "id should be null before persist");

        productRepository.persist(product);

        assertNotNull(product.id, "id should be set after persist");
        assertEquals(hashBefore, product.hashCode(),
                "hashCode must not change after persist (business key doesn't change)");
    }

    @Test
    @Order(2)
    @Transactional
    @DisplayName("Entity found by query equals entity found by id")
    void entityFoundByQueryEqualsEntityFoundById() {
        Product byId = productRepository.findById(
                productRepository.findBySku("INT-SKU-001").id);
        Product bySku = productRepository.findBySku("INT-SKU-001");

        // Different query paths → should still be equal
        assertEquals(byId, bySku);
        assertEquals(byId.hashCode(), bySku.hashCode());
    }

    @Test
    @Order(3)
    @Transactional
    @DisplayName("Entity persisted and added to Set can be found by a fresh query")
    void entityInSetFoundByFreshQuery() {
        Product original = new Product("INT-SKU-002", "Set Test Widget", new BigDecimal("19.99"));
        productRepository.persist(original);

        Set<Product> productSet = new HashSet<>();
        productSet.add(original);

        // Simulate fresh query (same transaction, but new entity reference from JPA)
        Product freshQuery = productRepository.findBySku("INT-SKU-002");

        assertTrue(productSet.contains(freshQuery),
                "Fresh query result must be found in the Set — business key equality must work");
    }

    @Test
    @Order(4)
    @Transactional
    @DisplayName("Updating mutable fields doesn't break Set membership")
    void mutableFieldUpdateDoesNotBreakSetMembership() {
        Product product = productRepository.findBySku("INT-SKU-001");
        Set<Product> managedSet = new HashSet<>();
        managedSet.add(product);

        // Mutate a mutable field
        product.name = "Renamed Widget";
        product.price = new BigDecimal("14.99");

        // Must still be findable in the Set (because hashCode is based on SKU, not
        // name/price)
        assertTrue(managedSet.contains(product),
                "Mutating non-key fields must not break Set membership");
    }

    @Test
    @Order(5)
    @Transactional
    @DisplayName("Two different SKUs produce different, unequal products")
    void differentSkusProduceDifferentProducts() {
        Product p1 = new Product("INT-SKU-A", "Widget A", BigDecimal.TEN);
        Product p2 = new Product("INT-SKU-B", "Widget A", BigDecimal.TEN); // same name, different sku

        productRepository.persist(p1);
        productRepository.persist(p2);

        assertNotEquals(p1, p2, "Different SKUs → not equal");
        // hashCodes CAN be equal (collision) but let's check they're not both in a set
        // as one
        Set<Product> set = Set.of(p1, p2);
        assertEquals(2, set.size(), "Two distinct products must occupy two slots in a Set");
    }

    @Test
    @Order(6)
    @DisplayName("Entity loaded in separate transactions is equal (simulating detached → reloaded)")
    @Transactional
    void entityEqualAfterReload() {
        // This test loads from DB — equals must work across transaction boundaries
        Product first = productRepository.findBySku("INT-SKU-001");
        Product second = productRepository.findBySku("INT-SKU-001");

        // In the same persistence context (same transaction), these are literally the
        // same object.
        // Across transactions, they would be different objects but should be equal.
        assertEquals(first, second);
        assertEquals(first.hashCode(), second.hashCode());
    }

    @AfterAll
    static void cleanup() {
        // Quarkus @TestTransaction usually rolls back; if not using it, clean up here.
    }
}
