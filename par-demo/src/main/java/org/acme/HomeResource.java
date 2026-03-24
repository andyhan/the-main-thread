package org.acme;

import jakarta.ws.rs.GET;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.core.MediaType;

@Path("/")
public class HomeResource {

  @GET
  @Produces(MediaType.TEXT_HTML)
  public String home() {
    return """
        <html>
          <body>
            <h1>PAR demo</h1>
            <p>This application protects the account page with Quarkus OIDC and Pushed Authorization Requests.</p>
            <p><a href="/account">Open the protected account page</a></p>
          </body>
        </html>
        """;
  }
}
