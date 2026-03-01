package org.acme.security.jpa;

import io.quarkus.qute.Template;
import io.quarkus.qute.TemplateInstance;
import io.quarkus.security.ForbiddenException;
import jakarta.ws.rs.core.Context;
import jakarta.ws.rs.core.HttpHeaders;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;
import jakarta.ws.rs.ext.ExceptionMapper;
import jakarta.ws.rs.ext.Provider;

@Provider
public class ForbiddenExceptionMapper implements ExceptionMapper<ForbiddenException> {

    private final Template forbidden;

    @Context
    HttpHeaders headers;

    public ForbiddenExceptionMapper(Template forbidden) {
        this.forbidden = forbidden;
    }

    @Override
    public Response toResponse(ForbiddenException exception) {
        boolean wantsHtml = headers.getAcceptableMediaTypes().stream()
                .anyMatch(mt -> mt.isCompatible(MediaType.TEXT_HTML_TYPE));

        if (wantsHtml) {
            TemplateInstance instance = forbidden.instance();
            String html = instance.render();
            return Response.status(Response.Status.FORBIDDEN)
                    .entity(html)
                    .type(MediaType.TEXT_HTML_TYPE)
                    .build();
        }

        return Response.status(Response.Status.FORBIDDEN)
                .entity("{\"error\":\"Forbidden\",\"message\":\"You don't have permission to access this resource.\"}")
                .type(MediaType.APPLICATION_JSON_TYPE)
                .build();
    }
}
