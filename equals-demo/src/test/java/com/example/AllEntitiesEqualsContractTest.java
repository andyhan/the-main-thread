package com.example;

import java.util.stream.Stream;

import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;

import com.example.entity.Category;
import com.example.entity.OrderLine;
import com.example.entity.Product;

import nl.jqno.equalsverifier.EqualsVerifier;
import nl.jqno.equalsverifier.Warning;

class AllEntitiesEqualsContractTest {

    static Stream<Class<?>> entityClasses() {
        return Stream.of(
                Category.class,
                Product.class,
                OrderLine.class);
    }

    @ParameterizedTest(name = "{0} — equals/hashCode contract")
    @MethodSource("entityClasses")
    void allEntities_satisfyEqualsContract(Class<?> entityClass) {
        var verifier = EqualsVerifier.forClass(entityClass)
                .suppress(Warning.NONFINAL_FIELDS)
                .suppress(Warning.IDENTICAL_COPY_FOR_VERSIONED_ENTITY);

        if (entityClass == Category.class) {
            verifier.suppress(Warning.SURROGATE_KEY)
                    .suppress(Warning.STRICT_HASHCODE) // Category uses constant hashCode
                    .suppress(Warning.JPA_GETTER) // PanacheEntity uses public field id, no getter
                    .verify();
        } else if (entityClass == Product.class) {
            Product p1 = new Product("SKU-A", null, null);
            Product p2 = new Product("SKU-B", null, null);
            verifier.withPrefabValues(Product.class, p1, p2)
                    .withOnlyTheseFields("sku")
                    .verify();
        } else if (entityClass == OrderLine.class) {
            verifier.withOnlyTheseFields("uuid")
                    .verify();
        } else {
            verifier.verify();
        }
    }
}