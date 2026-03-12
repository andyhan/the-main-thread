package com.mainthread.k8s;

import java.util.List;
import java.util.stream.Collectors;

import io.fabric8.kubernetes.api.model.Pod;
import io.fabric8.kubernetes.api.model.apps.Deployment;
import io.quarkiverse.mcp.server.Tool;
import io.quarkiverse.mcp.server.ToolArg;
import io.quarkiverse.mcp.server.ToolResponse;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;

@ApplicationScoped
public class KubernetesTools {

    @Inject
    ClusterInformerRegistry registry;

    @Tool(description = """
            List all pods in a namespace. Returns pod names, phases, and the node assignment.
            Use this when you need a quick workload view before drilling into one specific pod.
            """)
    public ToolResponse listPods(
            @ToolArg(description = "The Kubernetes namespace to query") String namespace) {

        if (!registry.pods().hasSynced()) {
            return ToolResponse.error(
                    "The pod informer has not completed its initial sync yet. Retry in a few seconds.");
        }

        if (!registry.pods().isWatching()) {
            return ToolResponse.error(
                    "The pod informer is not currently watching live cluster state. Retry in a few seconds instead of acting on cached data.");
        }

        List<Pod> pods = registry.pods().getStore().list().stream()
                .filter(p -> namespace.equals(p.getMetadata().getNamespace()))
                .toList();

        if (pods.isEmpty()) {
            return ToolResponse.success("No pods found in namespace '" + namespace + "'.");
        }

        String content = pods.stream()
                .map(p -> String.format(
                        "%s  phase=%s  node=%s",
                        p.getMetadata().getName(),
                        p.getStatus() != null && p.getStatus().getPhase() != null ? p.getStatus().getPhase()
                                : "<unknown>",
                        p.getSpec() != null && p.getSpec().getNodeName() != null ? p.getSpec().getNodeName()
                                : "<unscheduled>"))
                .collect(Collectors.joining("\n"));

        return ToolResponse.success("Pods in namespace '" + namespace + "':\n" + content);
    }

    @Tool(description = """
            List all deployments in a namespace with desired, ready, and available replica counts.
            Use this for a quick namespace-level rollout view.
            """)
    public ToolResponse listDeployments(
            @ToolArg(description = "The Kubernetes namespace to query") String namespace) {

        if (!registry.deployments().hasSynced()) {
            return ToolResponse.error(
                    "The deployment informer has not completed its initial sync yet. Retry in a few seconds.");
        }

        if (!registry.deployments().isWatching()) {
            return ToolResponse.error(
                    "The deployment informer is not currently watching live cluster state. Retry shortly.");
        }

        List<Deployment> deployments = registry.deployments().getStore().list().stream()
                .filter(d -> namespace.equals(d.getMetadata().getNamespace()))
                .toList();

        if (deployments.isEmpty()) {
            return ToolResponse.success("No deployments found in namespace '" + namespace + "'.");
        }

        String content = deployments.stream()
                .map(d -> String.format(
                        "%s  desired=%d  ready=%d  available=%d",
                        d.getMetadata().getName(),
                        d.getSpec() != null && d.getSpec().getReplicas() != null ? d.getSpec().getReplicas() : 0,
                        d.getStatus() != null && d.getStatus().getReadyReplicas() != null
                                ? d.getStatus().getReadyReplicas()
                                : 0,
                        d.getStatus() != null && d.getStatus().getAvailableReplicas() != null
                                ? d.getStatus().getAvailableReplicas()
                                : 0))
                .collect(Collectors.joining("\n"));

        return ToolResponse.success("Deployments in namespace '" + namespace + "':\n" + content);
    }

    @Tool(description = """
            Get the status of a specific deployment. Returns desired, ready, available, and updated replica counts.
            Use this when you need to know whether a rollout has finished or is degraded.
            """)
    public ToolResponse getDeploymentStatus(
            @ToolArg(description = "The Kubernetes namespace") String namespace,
            @ToolArg(description = "The deployment name") String name) {

        if (!registry.deployments().hasSynced()) {
            return ToolResponse.error(
                    "The deployment informer has not completed its initial sync yet. Retry in a few seconds.");
        }

        if (!registry.deployments().isWatching()) {
            return ToolResponse.error(
                    "The deployment informer is not currently watching live cluster state. Retry in a few seconds rather than acting on cached data.");
        }

        Deployment deployment = registry.deployments().getStore().list().stream()
                .filter(d -> namespace.equals(d.getMetadata().getNamespace())
                        && name.equals(d.getMetadata().getName()))
                .findFirst()
                .orElse(null);

        if (deployment == null) {
            return ToolResponse.error(
                    "Deployment '" + name + "' was not found in namespace '" + namespace + "'.");
        }

        int desired = deployment.getSpec() != null && deployment.getSpec().getReplicas() != null
                ? deployment.getSpec().getReplicas()
                : 0;
        int ready = deployment.getStatus() != null && deployment.getStatus().getReadyReplicas() != null
                ? deployment.getStatus().getReadyReplicas()
                : 0;
        int available = deployment.getStatus() != null && deployment.getStatus().getAvailableReplicas() != null
                ? deployment.getStatus().getAvailableReplicas()
                : 0;
        int updated = deployment.getStatus() != null && deployment.getStatus().getUpdatedReplicas() != null
                ? deployment.getStatus().getUpdatedReplicas()
                : 0;

        StringBuilder response = new StringBuilder();
        response.append("Deployment: ").append(namespace).append("/").append(name).append("\n");
        response.append("Desired replicas: ").append(desired).append("\n");
        response.append("Ready replicas: ").append(ready).append("\n");
        response.append("Available replicas: ").append(available).append("\n");
        response.append("Updated replicas: ").append(updated);

        if (deployment.getStatus() != null && deployment.getStatus().getConditions() != null) {
            String conditions = deployment.getStatus().getConditions().stream()
                    .map(c -> String.format(
                            "%s status=%s reason=%s message=%s",
                            c.getType(),
                            c.getStatus(),
                            c.getReason() != null ? c.getReason() : "<none>",
                            c.getMessage() != null ? c.getMessage() : "<none>"))
                    .collect(Collectors.joining("\n"));

            if (!conditions.isBlank()) {
                response.append("\nConditions:\n").append(conditions);
            }
        }

        return ToolResponse.success(response.toString());
    }
}