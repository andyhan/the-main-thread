package org.acme.security.jpa;

import io.quarkus.qute.Template;
import io.quarkus.qute.TemplateInstance;
import jakarta.annotation.security.PermitAll;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.QueryParam;
import jakarta.ws.rs.core.MediaType;

@Path("/login")
public class LoginResource {

    private final Template login;

    public LoginResource(Template login) {
        this.login = login;
    }

    @GET
    @Produces(MediaType.TEXT_HTML)
    @PermitAll
    public TemplateInstance get(@QueryParam("error") @jakarta.ws.rs.DefaultValue("false") boolean error) {
        return login.data("error", error);
    }
}
