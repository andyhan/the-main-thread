package academy.themainthread.api;

import academy.themainthread.badge.RecipientIdentity;
import academy.themainthread.domain.BadgeAssertion;
import academy.themainthread.domain.BadgeTemplate;
import academy.themainthread.domain.Earner;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.quarkus.qute.Template;
import io.quarkus.qute.TemplateInstance;
import jakarta.inject.Inject;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.NotFoundException;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.PathParam;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;
import org.eclipse.microprofile.config.inject.ConfigProperty;

import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

@Path("/")
public class PublicResource {

    @Inject
    Template assertion;

    @Inject
    Template badge;

    @Inject
    Template earner;

    @Inject
    ObjectMapper objectMapper;

    @ConfigProperty(name = "academy.base-url")
    String baseUrl;

    @GET
    @Path("/assertions/{id}")
    @Produces(MediaType.TEXT_HTML)
    public TemplateInstance getAssertionHtml(@PathParam("id") UUID id) {
        BadgeAssertion a = BadgeAssertion.findByIdWithDetails(id);
        if (a == null) {
            throw new NotFoundException();
        }
        return assertion.data("assertion", a).data("baseUrl", baseUrl);
    }

    @GET
    @Path("/assertions/{id}")
    @Produces(MediaType.APPLICATION_JSON)
    public Response getAssertionJson(@PathParam("id") UUID id) {
        BadgeAssertion a = BadgeAssertion.findByIdWithDetails(id);
        if (a == null) {
            return Response.status(Response.Status.NOT_FOUND).build();
        }
        return Response.ok(buildAssertionJson(a)).build();
    }

    @GET
    @Path("/badges/{id}")
    @Produces(MediaType.TEXT_HTML)
    public TemplateInstance getBadgeHtml(@PathParam("id") UUID id) {
        BadgeTemplate t = BadgeTemplate.findById(id);
        if (t == null) {
            throw new NotFoundException();
        }
        List<BadgeAssertion> recentEarners = BadgeAssertion.find(
                        "SELECT a FROM BadgeAssertion a JOIN FETCH a.earner WHERE a.template.id = ?1 AND a.revoked = false ORDER BY a.issuedOn DESC",
                        id)
                .page(0, 10)
                .list();
        return badge.data("template", t).data("recentEarners", recentEarners).data("baseUrl", baseUrl);
    }

    @GET
    @Path("/badges/{id}")
    @Produces(MediaType.APPLICATION_JSON)
    public Response getBadgeJson(@PathParam("id") UUID id) {
        BadgeTemplate t = BadgeTemplate.findById(id);
        if (t == null) {
            return Response.status(Response.Status.NOT_FOUND).build();
        }
        return Response.ok(buildBadgeJson(t)).build();
    }

    @GET
    @Path("/earners/{id}")
    @Produces(MediaType.TEXT_HTML)
    public TemplateInstance getEarnerHtml(@PathParam("id") UUID id) {
        Earner e = Earner.findById(id);
        if (e == null) {
            throw new NotFoundException();
        }
        List<BadgeAssertion> assertions = BadgeAssertion.find(
                        "SELECT a FROM BadgeAssertion a JOIN FETCH a.template WHERE a.earner.id = ?1 AND a.revoked = false ORDER BY a.issuedOn DESC",
                        id)
                .list();
        return earner.data("earner", e).data("assertions", assertions).data("baseUrl", baseUrl);
    }

    @GET
    @Path("/keys/1")
    @Produces(MediaType.APPLICATION_JSON)
    public Response getPublicKey() throws IOException {
        String pem;
        try (InputStream in = Thread.currentThread()
                .getContextClassLoader()
                .getResourceAsStream("META-INF/resources/public.pem")) {
            if (in == null) {
                return Response.status(Response.Status.NOT_FOUND)
                        .entity(Map.of("error", "public key not configured"))
                        .build();
            }
            pem = new String(in.readAllBytes(), StandardCharsets.UTF_8)
                    .replace("\r\n", "\n")
                    .trim();
        }
        Map<String, Object> body = new LinkedHashMap<>();
        body.put("type", "CryptographicKey");
        body.put("id", baseUrl + "/keys/1");
        body.put("owner", baseUrl);
        body.put("publicKeyPem", pem);
        return Response.ok(objectMapper.writeValueAsString(body)).type(MediaType.APPLICATION_JSON).build();
    }

    private Map<String, Object> buildAssertionJson(BadgeAssertion a) {
        String identity = RecipientIdentity.openBadgeIdentity(a.earner.email, a.salt);
        Map<String, Object> recipient = new LinkedHashMap<>();
        recipient.put("type", "email");
        recipient.put("hashed", true);
        recipient.put("salt", a.salt);
        recipient.put("identity", identity);

        Map<String, Object> verification = Map.of(
                "type", "signed",
                "creator", baseUrl + "/keys/1");

        Map<String, Object> doc = new LinkedHashMap<>();
        doc.put("@context", "https://w3id.org/openbadges/v2");
        doc.put("type", "Assertion");
        doc.put("id", baseUrl + "/assertions/" + a.id);
        doc.put("badge", baseUrl + "/badges/" + a.template.id);
        doc.put("issuedOn", a.issuedOn.toString());
        doc.put("recipient", recipient);
        doc.put("verification", verification);
        return doc;
    }

    private Map<String, Object> buildBadgeJson(BadgeTemplate t) {
        Map<String, Object> criteria = Map.of("narrative", t.criteria);
        Map<String, Object> doc = new LinkedHashMap<>();
        doc.put("@context", "https://w3id.org/openbadges/v2");
        doc.put("type", "BadgeClass");
        doc.put("id", baseUrl + "/badges/" + t.id);
        doc.put("name", t.name);
        doc.put("description", t.description);
        doc.put("image", t.imageUrl);
        doc.put("criteria", criteria);
        doc.put("issuer", baseUrl);
        return doc;
    }
}