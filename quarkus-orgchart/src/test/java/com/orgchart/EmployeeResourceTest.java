package com.orgchart;

import io.quarkus.test.junit.QuarkusTest;
import org.junit.jupiter.api.Test;

import static io.restassured.RestAssured.given;
import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.CoreMatchers.notNullValue;
import static org.hamcrest.Matchers.greaterThanOrEqualTo;

@QuarkusTest
class EmployeeResourceTest {

    @Test
    void listAll_returnsOkAndJsonArray() {
        given()
                .when().get("/api/employees")
                .then()
                .statusCode(200)
                .contentType("application/json")
                .body("$", notNullValue())
                .body("size()", greaterThanOrEqualTo(0));
    }

    @Test
    void hierarchy_returnsOkAndJsonArray() {
        given()
                .when().get("/api/employees/hierarchy")
                .then()
                .statusCode(200)
                .contentType("application/json")
                .body("$", notNullValue())
                .body("size()", greaterThanOrEqualTo(1))
                .body("[0].firstName", notNullValue())
                .body("[0].lastName", notNullValue())
                .body("[0].children", notNullValue());
    }

    @Test
    void create_returnsCreatedAndEmployee() {
        given()
                .contentType("application/json")
                .body("""
                        {
                            "firstName": "New",
                            "lastName": "Employee",
                            "title": "QA Engineer",
                            "email": "new.employee@example.com",
                            "department": "Engineering"
                        }
                        """)
                .when().post("/api/employees")
                .then()
                .statusCode(201)
                .contentType("application/json")
                .body("firstName", is("New"))
                .body("lastName", is("Employee"))
                .body("title", is("QA Engineer"))
                .body("email", is("new.employee@example.com"))
                .body("id", notNullValue());
    }
}
