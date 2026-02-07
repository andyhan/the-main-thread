package com.example.architecture;

import static com.tngtech.archunit.lang.syntax.ArchRuleDefinition.classes;
import static com.tngtech.archunit.library.Architectures.layeredArchitecture;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import com.enofex.taikai.Taikai;
import com.tngtech.archunit.core.domain.JavaClasses;
import com.tngtech.archunit.core.importer.ClassFileImporter;
import com.tngtech.archunit.lang.ArchRule;

import jakarta.enterprise.context.ApplicationScoped;

class ArchitectureTest {

    @Test
    void shouldFollowBasicJavaRules() {
        Taikai.builder()
                .namespace("com.example.architecture")
                .java(java -> java
                        .noUsageOfDeprecatedAPIs()
                        .methodsShouldNotDeclareGenericExceptions())
                .build()
                .check();
    }

    @Test
    void shouldFollowNamingConventions() {
        Taikai.builder()
                .namespace("com.example.architecture")
                .java(java -> java
                        .naming(naming -> naming
                                .classesShouldNotMatch(".*Impl")
                                .interfacesShouldNotHavePrefixI()
                                .constantsShouldFollowConventions()))
                .build()
                .check();
    }

    @Test
    void shouldHaveCleanImports() {
        Taikai.builder()
                .namespace("com.example.architecture")
                .java(java -> java
                        .imports(imports -> imports
                                .shouldHaveNoCycles()
                                .shouldNotImport("..internal..")
                                .shouldNotImport("org.junit..")))
                .build()
                .check();
    }

    @Test
    void shouldFollowLayeredArchitecture() {
        Taikai.builder()
                .namespace("com.example.architecture")
                .java(java -> java
                        .classesShouldResideInPackage(".*Service", "..service..")
                        .classesShouldResideInPackage(".*Repository", "..repository.."))
                .build()
                .check();
    }

    @Test
    void shouldUseConstructorInjection() {
        Taikai.builder()
                .namespace("com.example.architecture")
                .java(java -> java
                        .fieldsShouldNotBePublic())
                .build()
                .check();
    }

    @Test
    void servicesShouldBeApplicationScoped() {
        Taikai.builder()
                .namespace("com.example.architecture")
                .java(java -> java
                        .classesShouldBeAnnotatedWith(".*Service", ApplicationScoped.class))
                .build()
                .check();
    }

    @Test
    @DisplayName("Resources should only depend on services")
    void resourcesShouldOnlyDependOnServices() {
        JavaClasses classes = new ClassFileImporter()
                .importPackages("com.example.architecture");

        ArchRule rule = classes()
                .that().haveNameMatching(".*Resource")
                .should().onlyDependOnClassesThat()
                .haveNameMatching(".*Service|java..*|jakarta..*");

        rule.check(classes);
    }

    @Test
    @DisplayName("Should respect layered architecture")
    void shouldRespectLayeredArchitecture() {
        JavaClasses classes = new ClassFileImporter()
                .importPackages("com.example.architecture");

        layeredArchitecture()
                .consideringAllDependencies()
                .layer("Resource").definedBy("..resource..")
                .layer("Service").definedBy("..service..")
                .layer("Repository").definedBy("..repository..")
                .whereLayer("Resource").mayNotBeAccessedByAnyLayer()
                .whereLayer("Service").mayOnlyBeAccessedByLayers("Resource")
                .whereLayer("Repository").mayOnlyBeAccessedByLayers("Service")
                .check(classes);
    }

}