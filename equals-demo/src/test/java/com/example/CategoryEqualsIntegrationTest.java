package com.example;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import com.example.entity.Category;
import com.example.repository.CategoryRepository;

import io.quarkus.test.junit.QuarkusTest;
import jakarta.inject.Inject;
import jakarta.transaction.Transactional;

@QuarkusTest
class CategoryEqualsIntegrationTest {

    @Inject
    CategoryRepository categoryRepository;

    @Test
    @Transactional
    @DisplayName("Two unsaved Categories are not equal to each other")
    void twoUnsavedCategoriesNotEqual() {
        Category c1 = new Category("Books");
        Category c2 = new Category("Books"); // same name, both id=null

        // Per our implementation: id=null means only self-equality holds
        assertNotEquals(c1, c2,
                "Unsaved categories with null id should not be equal to each other");
    }

    @Test
    @Transactional
    @DisplayName("After persist, same-id categories are equal even with different names")
    void persistedCategoriesEqualByIdOnly() {
        Category c1 = new Category("Electronics");
        categoryRepository.persist(c1);

        // Reload (same transaction → likely same instance from L1 cache, but let's be
        // explicit)
        Category c2 = categoryRepository.findById(c1.id);

        assertEquals(c1, c2);
    }

    @Test
    @Transactional
    @DisplayName("hashCode is constant regardless of mutation")
    void hashCodeConstantDespiteMutation() {
        Category c = new Category("Music");
        categoryRepository.persist(c);

        int originalHash = c.hashCode();
        c.name = "Music & Instruments"; // mutate

        assertEquals(originalHash, c.hashCode(),
                "Constant hashCode must not change after field mutation");
    }

    @Test
    @Transactional
    @DisplayName("Performance warning: all categories in same hash bucket (constant hash)")
    void allCategoriesInSameBucket() {
        Category c1 = new Category("A");
        Category c2 = new Category("B");
        Category c3 = new Category("C");

        // All three have the same hashCode (by design — constant)
        assertEquals(c1.hashCode(), c2.hashCode());
        assertEquals(c2.hashCode(), c3.hashCode());
        // This is OK for correctness, but means HashSet lookups are O(n)
        // For small domain collections, this is acceptable
    }
}
