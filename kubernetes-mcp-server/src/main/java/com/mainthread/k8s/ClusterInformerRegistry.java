package com.mainthread.k8s;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.concurrent.TimeUnit;

import io.fabric8.kubernetes.api.model.Pod;
import io.fabric8.kubernetes.api.model.apps.Deployment;
import io.fabric8.kubernetes.client.Config;
import io.fabric8.kubernetes.client.KubernetesClient;
import io.fabric8.kubernetes.client.KubernetesClientBuilder;
import io.fabric8.kubernetes.client.informers.SharedIndexInformer;
import io.fabric8.kubernetes.client.informers.SharedInformerFactory;
import jakarta.annotation.PostConstruct;
import jakarta.annotation.PreDestroy;
import jakarta.enterprise.context.ApplicationScoped;
import org.eclipse.microprofile.config.inject.ConfigProperty;
import org.jboss.logging.Logger;

@ApplicationScoped
public class ClusterInformerRegistry {

    private static final Logger log = Logger.getLogger(ClusterInformerRegistry.class);

    @ConfigProperty(name = "quarkus.kubernetes.namespace", defaultValue = "mcp-demo")
    String watchNamespace;

    private KubernetesClient client;
    private SharedInformerFactory factory;

    private SharedIndexInformer<Pod> podInformer;
    private SharedIndexInformer<Deployment> deploymentInformer;

    @PostConstruct
    void start() {
        client = createKubernetesClient();
        factory = client.informers();

        // Namespace-scoped informers so a namespaced Role (list/watch in mcp-demo) is
        // enough.
        // inNamespace() is deprecated in Fabric8 7.x but remains the supported way to
        // get namespaced informers in this API.
        long resyncMs = TimeUnit.MINUTES.toMillis(5);
        @SuppressWarnings("deprecation")
        var namespacedFactory = factory.inNamespace(watchNamespace);
        podInformer = namespacedFactory.sharedIndexInformerFor(Pod.class, resyncMs);
        deploymentInformer = namespacedFactory.sharedIndexInformerFor(Deployment.class, resyncMs);

        factory.startAllRegisteredInformers();
        log.infof("Started pod and deployment informers for namespace %s", watchNamespace);
    }

    /**
     * Builds the Kubernetes client. When running locally, uses kubeconfig
     * (KUBECONFIG or ~/.kube/config)
     * so the API server is reached via your cluster's URL (e.g. minikube, kind),
     * not the in-cluster
     * host "kubernetes.default.svc". When no kubeconfig file exists (e.g.
     * in-cluster), falls back to
     * default auto-config (service account).
     */
    private static KubernetesClient createKubernetesClient() {
        Path kubeconfigPath = resolveKubeconfigPath();
        if (kubeconfigPath != null && Files.isRegularFile(kubeconfigPath)) {
            try {
                Config config = Config.fromKubeconfig(kubeconfigPath.toFile());
                log.infof("Using kubeconfig from %s for Kubernetes API connection", kubeconfigPath);
                return new KubernetesClientBuilder().withConfig(config).build();
            } catch (Exception e) {
                log.warnf(e, "Failed to load kubeconfig from %s, falling back to default config", kubeconfigPath);
            }
        }
        return new KubernetesClientBuilder().build();
    }

    private static Path resolveKubeconfigPath() {
        String fromEnv = System.getenv("KUBECONFIG");
        if (fromEnv != null && !fromEnv.isBlank()) {
            return Path.of(fromEnv.trim());
        }
        String home = System.getProperty("user.home");
        if (home != null) {
            return Path.of(home, ".kube", "config");
        }
        return null;
    }

    @PreDestroy
    void stop() {
        if (factory != null) {
            factory.stopAllRegisteredInformers();
        }
        if (client != null) {
            client.close();
        }
    }

    public SharedIndexInformer<Pod> pods() {
        return podInformer;
    }

    public SharedIndexInformer<Deployment> deployments() {
        return deploymentInformer;
    }

    public boolean allInformersWatching() {
        return podInformer != null
                && deploymentInformer != null
                && podInformer.isWatching()
                && deploymentInformer.isWatching();
    }

    public boolean allInformersSynced() {
        return podInformer != null
                && deploymentInformer != null
                && podInformer.hasSynced()
                && deploymentInformer.hasSynced();
    }
}