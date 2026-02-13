package com.example.reviews.resource;

import java.util.Map;

import com.example.reviews.events.CloudEventEnvelope;
import com.example.reviews.events.FlowInProducer;
import com.example.reviews.flow.ReviewTriageWorkflow;
import com.example.reviews.model.ApprovalDecision;
import com.example.reviews.model.Review;
import com.example.reviews.state.ApprovalCorrelationStore;

import io.serverlessworkflow.impl.WorkflowInstance;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import jakarta.ws.rs.Consumes;
import jakarta.ws.rs.POST;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.PathParam;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;

@Path("/reviews")
@ApplicationScoped
@Consumes(MediaType.APPLICATION_JSON)
@Produces(MediaType.APPLICATION_JSON)
public class ReviewResource {

    @Inject
    ReviewTriageWorkflow workflow;

    @Inject
    FlowInProducer flowIn;

    @Inject
    ApprovalCorrelationStore store;

    @POST
    public Response submit(Review review) {
        WorkflowInstance instance = workflow.instance(review);
        store.put(review.reviewId(), instance.id());
        instance.start();

        return Response.accepted(Map.of(
                "status", "submitted",
                "reviewId", review.reviewId())).build();
    }

    @POST
    @Path("/{reviewId}/approval")
    public Response approve(@PathParam("reviewId") String reviewId, ApprovalDecision decision) {
        String instanceId = store.getInstanceId(reviewId);
        if (instanceId == null) {
            return Response.status(409).entity(Map.of(
                    "error", "No pending approval found for reviewId",
                    "reviewId", reviewId)).build();
        }

        // Send approval decision back to the waiting workflow instance.
        // Use lowercase key to satisfy CloudEvents extension name spec ([a-z0-9]).
        CloudEventEnvelope evt = flowIn.newEvent("com.example.review.approval.done", decision);
        flowIn.sendWithExtensions(evt, Map.of("xflowinstanceid", instanceId));

        return Response.ok(Map.of(
                "status", "approval-sent",
                "reviewId", reviewId,
                "instanceId", instanceId)).build();
    }
}
