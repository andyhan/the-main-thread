package com.example.auth;

import static io.restassured.RestAssured.given;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.notNullValue;

import org.eclipse.microprofile.config.inject.ConfigProperty;
import org.junit.jupiter.api.Test;

import io.quarkiverse.mailpit.test.InjectMailbox;
import io.quarkiverse.mailpit.test.Mailbox;
import io.quarkiverse.mailpit.test.WithMailbox;
import io.quarkus.test.junit.QuarkusTest;
import io.restassured.http.ContentType;

@QuarkusTest
@WithMailbox
class MailpitIntegrationTest {

    @InjectMailbox
    Mailbox mailbox;

    @ConfigProperty(name = "app.mail.from")
    String fromAddress;

    @Test
    void requestMagicLinkSendsEmailWithLink() {
        given()
                .contentType(ContentType.JSON)
                .body("{\"email\":\"tutorial@example.com\",\"displayName\":\"Tutorial\",\"password\":\"very-long-password-123\"}")
                .when()
                .post("/auth/register");

        given()
                .contentType(ContentType.JSON)
                .body("{\"email\":\"tutorial@example.com\"}")
                .when()
                .post("/auth/magic/request")
                .then()
                .statusCode(204);

        var message = mailbox.findFirst(fromAddress);
        assertThat(message, notNullValue());
        assertThat(message.getSubject(), notNullValue());
        assertThat(message.getSubject(), containsString("sign-in"));
        assertThat(message.getText(), containsString("/auth/magic?token="));
    }
}
