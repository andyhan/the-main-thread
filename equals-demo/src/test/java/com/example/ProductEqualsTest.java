package com.example;

import org.junit.jupiter.api.Test;

import com.example.entity.Product;
import com.example.entity.Tag;

import nl.jqno.equalsverifier.EqualsVerifier;
import nl.jqno.equalsverifier.Warning;

/**
 * Plain unit test (no @QuarkusTest) so Product is not Hibernate-enhanced;
 * EqualsVerifier would otherwise hit abstract delegation when accessing fields.
 */
class ProductEqualsTest {

    @Test
    void product_equalsAndHashCode_satisfyContract() {
        Tag tag1 = new Tag();
        tag1.id = 1L;
        tag1.name = "Cat1";
        Tag tag2 = new Tag();
        tag2.id = 2L;
        tag2.name = "Cat2";

        EqualsVerifier.forClass(Product.class)
                .suppress(Warning.NONFINAL_FIELDS)
                .withPrefabValues(Tag.class, tag1, tag2)
                .withOnlyTheseFields("sku")
                .verify();
    }
}