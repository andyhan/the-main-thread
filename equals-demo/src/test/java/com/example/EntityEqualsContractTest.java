package com.example;

import java.math.BigDecimal;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import com.example.entity.Category;
import com.example.entity.OrderLine;
import com.example.entity.Product;

import nl.jqno.equalsverifier.EqualsVerifier;
import nl.jqno.equalsverifier.Warning;

class EntityEqualsContractTest {

    // Product (Business Key: SKU)

    @Nested
    @DisplayName("Product — business key equality (SKU)")
    class ProductEqualsTest {

        @Test
        @DisplayName("equals/hashCode contract is satisfied")
        void contract_satisfied() {
            Product p1 = new Product("SKU-X", null, null);
            Product p2 = new Product("SKU-Y", null, null);
            EqualsVerifier.forClass(Product.class)
                    .suppress(Warning.NONFINAL_FIELDS) // JPA entities are non-final
                    .withPrefabValues(Product.class, p1, p2)
                    .withOnlyTheseFields("sku")
                    .verify();
        }

        @Test
        @DisplayName("Two Products with same SKU are equal")
        void sameSkuIsEqual() {
            Product p1 = new Product("SKU-001", "Widget A", BigDecimal.TEN);
            Product p2 = new Product("SKU-001", "Widget B", BigDecimal.ONE); // Different name!

            assert p1.equals(p2) : "Same SKU → must be equal";
            assert p1.hashCode() == p2.hashCode() : "Same SKU → same hashCode";
        }

        @Test
        @DisplayName("hashCode is stable before and after setting id (simulating persist)")
        void hashCodeStableAcrossPersist() {
            Product p = new Product("SKU-STABLE", "Test", BigDecimal.ZERO);
            int hashBefore = p.hashCode();

            // Simulate what JPA does when persisting (sets the id)
            p.id = 99L;
            int hashAfter = p.hashCode();

            assert hashBefore == hashAfter : "hashCode must not change when id is assigned";
        }

        @Test
        @DisplayName("Product works correctly in a HashSet across simulated transaction boundary")
        void worksInHashSetAcrossTransactionBoundary() {
            Product original = new Product("SKU-TX-001", "Widget", BigDecimal.TEN);
            original.id = 1L; // Simulates post-persist state

            java.util.Set<Product> set = new java.util.HashSet<>();
            set.add(original);

            // Simulate loading same entity in a new session (new object, same data)
            Product reloaded = new Product("SKU-TX-001", "Widget", BigDecimal.TEN);
            reloaded.id = 1L;

            assert set.contains(reloaded) : "Same SKU → must be found in Set";
        }

        @Test
        @DisplayName("null is never equal to a Product")
        void nullSafety() {
            Product p = new Product("SKU-NULL", "Test", BigDecimal.ONE);
            assert !p.equals(null) : "equals(null) must return false";
        }

        @Test
        @DisplayName("Products with different SKUs are not equal")
        void differentSkuNotEqual() {
            Product p1 = new Product("SKU-A", "Same Name", BigDecimal.TEN);
            Product p2 = new Product("SKU-B", "Same Name", BigDecimal.TEN);
            assert !p1.equals(p2);
        }
    }

    // OrderLine (UUID Strategy)

    @Nested
    @DisplayName("OrderLine — UUID-based equality")
    class OrderLineEqualsTest {

        @Test
        @DisplayName("equals/hashCode contract is satisfied")
        void contract_satisfied() {
            EqualsVerifier.forClass(OrderLine.class)
                    .suppress(Warning.NONFINAL_FIELDS)
                    .withOnlyTheseFields("uuid")
                    .verify();
        }

        @Test
        @DisplayName("Two distinct OrderLine instances are NOT equal (each has unique UUID)")
        void distinctInstancesAreNotEqual() {
            OrderLine line1 = new OrderLine();
            OrderLine line2 = new OrderLine();

            assert !line1.equals(line2) : "Two new OrderLines should not be equal";
            // (UUID collision probability: 1 in 2^122 — negligible)
        }

        @Test
        @DisplayName("Same instance is always equal to itself")
        void sameInstanceIsEqual() {
            OrderLine line = new OrderLine();
            assert line.equals(line);
            assert line.hashCode() == line.hashCode();
        }

        @Test
        @DisplayName("hashCode is stable even before id is set")
        void hashCodeStableBeforePersist() {
            OrderLine line = new OrderLine();
            int hash1 = line.hashCode();
            int hash2 = line.hashCode();
            assert hash1 == hash2;

            line.id = 100L; // simulate persist
            assert line.hashCode() == hash1 : "hashCode must not change after id assignment";
        }
    }

    // Category (Constant Hash / DB Id Strategy)

    @Nested
    @DisplayName("Category — database id equality with constant hashCode")
    class CategoryEqualsTest {

        @Test
        @DisplayName("equals/hashCode contract is satisfied")
        void contract_satisfied() {
            EqualsVerifier.forClass(Category.class)
                    .suppress(Warning.NONFINAL_FIELDS)
                    .suppress(Warning.IDENTICAL_COPY_FOR_VERSIONED_ENTITY) // id can be null
                    .suppress(Warning.SURROGATE_KEY) // equality by id only
                    .suppress(Warning.STRICT_HASHCODE) // hashCode is intentionally constant
                    .suppress(Warning.JPA_GETTER) // PanacheEntity uses public field id, no getter
                    .verify();
        }

        @Test
        @DisplayName("Two unsaved Categories with same name are NOT equal (both have null id)")
        void unsavedCategoriesAreNotEqual() {
            Category c1 = new Category("Electronics");
            Category c2 = new Category("Electronics");
            // Same name, but different objects, both null id
            // equals() returns false when id is null (only self-equality holds)
            assert !c1.equals(c2) : "Unsaved entities with null id must not be equal to each other";
        }

        @Test
        @DisplayName("Category with id is equal to another Category with same id")
        void savedCategoriesWithSameIdAreEqual() {
            Category c1 = new Category("Electronics");
            c1.id = 5L;
            Category c2 = new Category("Different Name");
            c2.id = 5L;

            assert c1.equals(c2) : "Same DB id → same entity";
            assert c1.hashCode() == c2.hashCode();
        }

        @Test
        @DisplayName("hashCode is constant regardless of state")
        void hashCodeIsConstant() {
            Category c = new Category("Books");
            int hashBefore = c.hashCode();
            c.id = 7L;
            c.name = "Changed";
            assert c.hashCode() == hashBefore : "hashCode must be constant";
        }
        
    }
}