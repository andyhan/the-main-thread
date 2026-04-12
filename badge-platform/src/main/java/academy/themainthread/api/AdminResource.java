package academy.themainthread.api;

import academy.themainthread.badge.BadgeIssuanceService;
import academy.themainthread.domain.AccreditedPartner;
import academy.themainthread.domain.BadgeAssertion;
import academy.themainthread.domain.BadgeTemplate;
import academy.themainthread.domain.Earner;
import academy.themainthread.domain.PartnerBadgeTemplate;
import academy.themainthread.domain.PartnerBadgeTemplateId;
import jakarta.inject.Inject;
import jakarta.transaction.Transactional;
import jakarta.validation.Valid;
import jakarta.ws.rs.Consumes;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.POST;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.PathParam;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.QueryParam;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;
import org.eclipse.microprofile.openapi.annotations.Operation;
import org.eclipse.microprofile.openapi.annotations.tags.Tag;

import java.net.URI;
import java.util.List;
import java.util.UUID;

@Path("/admin")
@Produces(MediaType.APPLICATION_JSON)
@Consumes(MediaType.APPLICATION_JSON)
@Tag(name = "Admin", description = "Badge platform administration")
public class AdminResource {

    @Inject
    BadgeIssuanceService issuanceService;

    @POST
    @Path("/badges")
    @Transactional
    @Operation(summary = "Create a badge template")
    public Response createBadgeTemplate(@Valid AdminRequests.CreateBadgeTemplate req) {
        BadgeTemplate template = new BadgeTemplate();
        template.name = req.name();
        template.description = req.description();
        template.criteria = req.criteria();
        template.imageUrl = req.imageUrl();
        template.skills = req.skills();
        template.persist();
        return Response.created(URI.create("/admin/badges/" + template.id)).entity(template).build();
    }

    @GET
    @Path("/badges")
    @Operation(summary = "List all badge templates")
    public List<BadgeTemplate> listBadgeTemplates() {
        return BadgeTemplate.listAll();
    }

    @POST
    @Path("/earners")
    @Transactional
    @Operation(summary = "Register an earner")
    public Response createEarner(@Valid AdminRequests.CreateEarner req) {
        if (Earner.findByEmail(req.email()) != null) {
            return Response.status(Response.Status.CONFLICT)
                    .entity(new ErrorResponse("Earner already exists with email: " + req.email()))
                    .build();
        }
        Earner earner = new Earner();
        earner.email = req.email();
        earner.name = req.name();
        earner.persist();
        return Response.created(URI.create("/admin/earners/" + earner.id)).entity(earner).build();
    }

    @POST
    @Path("/issue")
    @Transactional
    @Operation(summary = "Manually issue a badge to an earner")
    public Response issueBadge(@Valid AdminRequests.IssueBadge req) {
        Earner earner = Earner.findById(req.earnerId());
        if (earner == null) {
            return Response.status(Response.Status.NOT_FOUND)
                    .entity(new ErrorResponse("Earner not found"))
                    .build();
        }

        BadgeTemplate template = BadgeTemplate.findById(req.templateId());
        if (template == null) {
            return Response.status(Response.Status.NOT_FOUND)
                    .entity(new ErrorResponse("Template not found"))
                    .build();
        }

        BadgeAssertion assertion = issuanceService.issueWithDefaultExpiry(earner, template);
        return Response.created(URI.create("/assertions/" + assertion.id)).entity(assertion).build();
    }

    @GET
    @Path("/assertions")
    @Operation(summary = "List all issued assertions")
    public List<BadgeAssertion> listAssertions(
            @QueryParam("earnerId") UUID earnerId, @QueryParam("templateId") UUID templateId) {
        if (earnerId != null) {
            return BadgeAssertion.find(
                            "SELECT a FROM BadgeAssertion a JOIN FETCH a.earner JOIN FETCH a.template WHERE a.earner.id = ?1 ORDER BY a.issuedOn DESC",
                            earnerId)
                    .list();
        }
        if (templateId != null) {
            return BadgeAssertion.find(
                            "SELECT a FROM BadgeAssertion a JOIN FETCH a.earner JOIN FETCH a.template WHERE a.template.id = ?1 ORDER BY a.issuedOn DESC",
                            templateId)
                    .list();
        }
        return BadgeAssertion.find(
                        "SELECT a FROM BadgeAssertion a JOIN FETCH a.earner JOIN FETCH a.template ORDER BY a.issuedOn DESC")
                .list();
    }

    @POST
    @Path("/assertions/{id}/revoke")
    @Transactional
    @Operation(summary = "Revoke an assertion")
    public Response revoke(@PathParam("id") UUID id, @QueryParam("reason") String reason) {
        issuanceService.revoke(id, reason);
        return Response.ok().build();
    }

    @POST
    @Path("/partners")
    @Transactional
    @Operation(summary = "Register an accredited partner")
    public Response registerPartner(@Valid AdminRequests.RegisterPartner req) {
        AccreditedPartner partner = new AccreditedPartner();
        partner.name = req.name();
        partner.webhookSecret = req.webhookSecret();
        partner.persist();
        return Response.created(URI.create("/admin/partners/" + partner.id)).entity(partner).build();
    }

    @POST
    @Path("/partners/{id}/courses")
    @Transactional
    @Operation(summary = "Map a partner course to a badge template")
    public Response mapCourse(@PathParam("id") UUID partnerId, @Valid AdminRequests.MapPartnerCourse req) {
        AccreditedPartner partner = AccreditedPartner.findById(partnerId);
        if (partner == null) {
            return Response.status(Response.Status.NOT_FOUND)
                    .entity(new ErrorResponse("Partner not found"))
                    .build();
        }

        BadgeTemplate template = BadgeTemplate.findById(req.templateId());
        if (template == null) {
            return Response.status(Response.Status.NOT_FOUND)
                    .entity(new ErrorResponse("Template not found"))
                    .build();
        }

        PartnerBadgeTemplateId pkId = new PartnerBadgeTemplateId();
        pkId.partnerId = partnerId;
        pkId.courseId = req.courseId();

        PartnerBadgeTemplate mapping = new PartnerBadgeTemplate();
        mapping.id = pkId;
        mapping.partner = partner;
        mapping.template = template;
        mapping.persist();

        return Response.ok(mapping).build();
    }

    public record ErrorResponse(String message) {}
}