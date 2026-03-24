package org.acme;

import org.eclipse.microprofile.jwt.Claims;
import org.eclipse.microprofile.jwt.JsonWebToken;

import io.quarkus.oidc.IdToken;
import io.quarkus.oidc.RefreshToken;
import io.quarkus.security.Authenticated;
import jakarta.inject.Inject;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.core.MediaType;

@Path("/account")
public class AccountResource {

  @Inject
  @IdToken
  JsonWebToken idToken;

  @Inject
  JsonWebToken accessToken;

  @Inject
  RefreshToken refreshToken;

  @GET
  @Authenticated
  @Produces(MediaType.TEXT_HTML)
  public String account() {
    Object givenName = idToken.getClaim(Claims.given_name.name());
    String displayName = givenName != null ? givenName.toString() : idToken.getName();

    return """
        <html>
          <body>
            <h1>Hello, %s</h1>
            <p>You authenticated through Quarkus OIDC with PAR enabled.</p>
            <p><a href="/account/tokens">Inspect tokens</a></p>
            <p><a href="/logout">Logout</a></p>
          </body>
        </html>
        """.formatted(displayName);
  }

  @GET
  @Path("/tokens")
  @Authenticated
  @Produces(MediaType.APPLICATION_JSON)
  public TokenInfo tokens() {
    return new TokenInfo(
        idToken.getName(),
        idToken.getSubject(),
        accessToken.getExpirationTime(),
        refreshToken.getToken() != null);
  }

  public record TokenInfo(
      String principalName,
      String subject,
      long accessTokenExpirationTime,
      boolean hasRefreshToken) {
  }
}
