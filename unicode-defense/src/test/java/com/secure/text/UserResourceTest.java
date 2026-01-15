package com.secure.text;

import static io.restassured.RestAssured.given;
import static org.hamcrest.CoreMatchers.containsString;

import org.junit.jupiter.api.Test;

import io.quarkus.test.junit.QuarkusTest;
import io.restassured.http.ContentType;

@QuarkusTest
public class UserResourceTest {

    @Test
    void validUserIsAccepted() {
        given()
                .contentType(ContentType.JSON)
                .body("""
                        {
                          "username": "duke",
                          "email": "duke@java.io",
                          "bio": "I love coffee"
                        }
                        """)
                .when()
                .post("/api/users")
                .then()
                .statusCode(200);
    }

    @Test
    void invisibleCharactersAreRejected() {
        given()
                .contentType(ContentType.JSON)
                .body("""
                        {
                          "username": "duke\\u200B",
                          "email": "evil@test.com",
                          "bio": "Normal bio"
                        }
                        """)
                .when()
                .post("/api/users")
                .then()
                .statusCode(400)
                .body(containsString("Username contains"));
    }

    @Test
    void bidiOverrideIsRejected() {
        given()
                .contentType(ContentType.JSON)
                .body("""
                        {
                          "username": "hacker",
                          "email": "hacker@test.com",
                          "bio": "file \\u202Etxt.exe"
                        }
                        """)
                .when()
                .post("/api/users")
                .then()
                .statusCode(400);
    }

    @Test
    void homographAttackIsRejected() {
        given()
                .contentType(ContentType.JSON)
                .body("""
                        {
                          "username": "p\\u0430ypal",
                          "email": "phish@test.com",
                          "bio": "normal"
                        }
                        """)
                .when()
                .post("/api/users")
                .then()
                .statusCode(400)
                .body(containsString("suspicious"));
    }
}