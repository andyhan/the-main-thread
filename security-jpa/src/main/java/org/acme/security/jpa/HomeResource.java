package org.acme.security.jpa;

import io.quarkus.qute.Template;
import io.quarkus.qute.TemplateInstance;
import jakarta.inject.Inject;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.core.MediaType;

@Path("/")
public class HomeResource {

    @Inject
    CurrentUserService currentUserService;

    private final Template index;

    public HomeResource(Template index) {
        this.index = index;
    }

    @GET
    @Produces(MediaType.TEXT_HTML)
    public TemplateInstance get() {
        User user = currentUserService.getCurrentUser();
        String displayName = user != null ? user.getDisplayNameOrUsername() : "Guest";
        return index.data("username", displayName);
    }
}