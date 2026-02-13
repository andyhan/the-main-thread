package com.example.reviews.flow;

import static io.serverlessworkflow.fluent.func.dsl.FuncDSL.emitJson;
import static io.serverlessworkflow.fluent.func.dsl.FuncDSL.function;
import static io.serverlessworkflow.fluent.func.dsl.FuncDSL.listen;
import static io.serverlessworkflow.fluent.func.dsl.FuncDSL.set;
import static io.serverlessworkflow.fluent.func.dsl.FuncDSL.switchWhenOrElse;
import static io.serverlessworkflow.fluent.func.dsl.FuncDSL.toOne;
import static io.serverlessworkflow.fluent.func.dsl.FuncDSL.withUniqueId;

import java.util.Collection;
import java.util.HashMap;
import java.util.Map;

import com.example.reviews.ai.ReviewSentimentAgent;
import com.example.reviews.model.ApprovalDecision;
import com.example.reviews.model.ProcessedReview;
import com.example.reviews.state.ApprovalCorrelationStore;

import io.quarkiverse.flow.Flow;
import io.serverlessworkflow.api.types.Workflow;
import io.serverlessworkflow.fluent.func.FuncWorkflowBuilder;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;

@ApplicationScoped
public class ReviewTriageWorkflow extends Flow {

    @Inject
    ReviewSentimentAgent agent;

    @Inject
    ApprovalCorrelationStore correlationStore;

    @Override
    public Workflow descriptor() {
        return FuncWorkflowBuilder.workflow("review-triage")
                .tasks(
                        // 1) Initial state: capture review from flow.instance(review).start() input
                        set("{ review: . }"),

                        // 2) Agent call: classify sentiment and derive action, preserving review in
                        // state
                        withUniqueId("classifySentiment", this::classifySentimentWithReview, Map.class),

                        // Merge agent output into workflow doc (keep .review)
                        set("{ review: .review, sentiment: .sentiment, action: .action }"),

                        // 3) Branch: if action is needs-approval go to approvalRequest; else go to done
                        switchWhenOrElse(
                                ".action == \"needs-approval\"",
                                "approvalRequest",
                                "done"),

                        // 4) Approval path: build approval payload, stash state, emit
                        // approval.requested, then wait for approval.done
                        function("approvalRequest", ReviewTriageWorkflow::mergeApprovalRequest, Map.class),
                        function("stashState", this::stashStateForApproval, Map.class),

                        emitJson("emitApprovalRequest", "com.example.review.approval.requested", Map.class)
                                .inputFrom(".approvalRequest"),

                        listen("waitApproval", toOne("com.example.review.approval.done"))
                                .outputAs((Collection<Object> c) -> c.iterator().next()),

                        // Pass through approval decision and restore stashed state (review, sentiment,
                        // action) plus approved/approvedBy
                        function("applyDecision", (ApprovalDecision d) -> d, ApprovalDecision.class),
                        function("restoreState", this::restoreStateAfterApproval, ApprovalDecision.class),

                        set("{ review: .review, sentiment: .sentiment, action: .action, approved: .approved, approvedBy: .approvedBy }"),

                        // 5) Final path (join point): pass-through so the following set() sees full
                        // state
                        function("done", (Object ctx) -> ctx, Object.class),

                        // Pass-through so the set() below sees full state (review, sentiment, action,
                        // approved, approvedBy)
                        function("buildProcessed", (Object ctx) -> ctx, Object.class),

                        // Build processed payload (finalAction from approved/action) and emit
                        // review.processed
                        set("""
                                {
                                  processed: {
                                    reviewId: .review.reviewId,
                                    productId: .review.productId,
                                    rating: .review.rating,
                                    sentiment: .sentiment,
                                    action: .action,
                                    finalAction: (
                                      if .action == "needs-approval"
                                      then (if .approved == true then "publish-response" else "escalate-further" end)
                                      else .action
                                      end
                                    ),
                                    approvedBy: (if .action == "needs-approval" then .approvedBy else "system" end)
                                  }
                                }
                                """),

                        emitJson("emitProcessed", "com.example.review.processed", ProcessedReview.class)
                                .inputFrom(".processed"))
                .build();
    }

    private static String actionFor(String sentiment) {
        if (sentiment == null)
            return "log-only";
        return switch (sentiment) {
            case "very-positive" -> "feature-on-website";
            case "positive" -> "thank-customer";
            case "neutral" -> "log-only";
            case "negative", "very-negative" -> "needs-approval";
            default -> "log-only";
        };
    }

    private Map<String, Object> classifySentimentWithReview(String memoryId, Map<String, Object> state) {
        Object reviewObj = state.get("review");
        String text = null;
        if (reviewObj instanceof Map<?, ?> reviewMap) {
            Object t = reviewMap.get("text");
            text = t != null ? t.toString() : null;
        }
        String sentiment = agent.classify(memoryId, text != null ? text : "");
        Map<String, Object> out = new HashMap<>(state);
        out.put("sentiment", sentiment);
        out.put("action", actionFor(sentiment));
        return out;
    }

    private static Map<String, Object> mergeApprovalRequest(Map<String, Object> state) {
        Map<String, Object> out = new HashMap<>(state);
        Object reviewObj = state.get("review");
        Object reviewId = null;
        if (reviewObj instanceof Map<?, ?> reviewMap) {
            reviewId = reviewMap.get("reviewId");
        }
        Map<String, Object> approvalRequest = Map.of(
                "reviewId", reviewId != null ? reviewId : "",
                "sentiment", String.valueOf(state.getOrDefault("sentiment", "")),
                "action", String.valueOf(state.getOrDefault("action", "")));
        out.put("approvalRequest", approvalRequest);
        return out;
    }

    private Map<String, Object> stashStateForApproval(Map<String, Object> state) {
        Object reviewObj = state.get("review");
        if (reviewObj instanceof Map<?, ?> reviewMap) {
            Object reviewId = reviewMap.get("reviewId");
            if (reviewId != null) {
                correlationStore.putState(reviewId.toString(), new HashMap<>(state));
            }
        }
        return state;
    }

    private Map<String, Object> restoreStateAfterApproval(ApprovalDecision decision) {
        Map<String, Object> stashed = correlationStore.getState(decision.reviewId());
        if (stashed == null) {
            return Map.of(
                    "review", Map.of("reviewId", decision.reviewId(), "productId", "", "rating", 0, "text", ""),
                    "sentiment", "", "action", "needs-approval",
                    "approved", decision.approved(), "approvedBy", decision.approver());
        }
        Map<String, Object> out = new HashMap<>(stashed);
        out.put("approved", decision.approved());
        out.put("approvedBy", decision.approver());
        return out;
    }
}