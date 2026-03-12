package com.mainthread.k8s;

import java.util.concurrent.TimeUnit;

import org.eclipse.microprofile.config.inject.ConfigProperty;
import org.jboss.logging.Logger;

import io.fabric8.kubernetes.api.model.Pod;
import io.fabric8.kubernetes.api.model.apps.Deployment;
import io.fabric8.kubernetes.client.KubernetesClient;
import io.fabric8.kubernetes.client.informers.SharedIndexInformer;
import io.fabric8.kubernetes.client.informers.SharedInformerFactory;
import jakarta.annotation.PostConstruct;
import jakarta.annotation.PreDestroy;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;

@ApplicationScoped
public class ClusterInformerRegistry {

    private static final Logger log = Logger.getLogger(ClusterInformerRegistry.class);

    @Inject
    KubernetesClient client;

    @ConfigProperty(name = "quarkus.kubernetes.namespace", defaultValue = "mcp-demo")
    String watchNamespace;

    private SharedInformerFactory factory;

    private SharedIndexInformer<Pod> podInformer;
    private SharedIndexInformer<Deployment> deploymentInformer;

    @PostConstruct
    void start() {
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

    @PreDestroy
    void stop() {
        if (factory != null) {
            factory.stopAllRegisteredInformers();
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