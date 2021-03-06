/*
 * Copyright Strimzi authors.
 * License: Apache License 2.0 (see the file LICENSE or http://apache.org/licenses/LICENSE-2.0.html).
 */
package io.strimzi.systemtest.utils.kafkaUtils;

import io.fabric8.kubernetes.api.model.Service;
import io.strimzi.systemtest.Constants;
import io.vertx.core.json.JsonArray;
import io.vertx.core.json.JsonObject;
import io.strimzi.operator.common.model.Labels;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import io.strimzi.systemtest.resources.KubernetesResource;

import java.util.HashMap;
import java.util.Map;

import static io.strimzi.test.k8s.KubeClusterResource.kubeClient;
import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.MatcherAssert.assertThat;

public class KafkaBridgeUtils {

    private static final Logger LOGGER = LogManager.getLogger(KafkaBridgeUtils.class);

    private KafkaBridgeUtils() {}

    public static int getBridgeNodePort(String namespace, String bridgeExternalService) {
        Service extBootstrapService = kubeClient(namespace).getClient().services()
                .inNamespace(namespace)
                .withName(bridgeExternalService)
                .get();

        return extBootstrapService.getSpec().getPorts().get(0).getNodePort();
    }

    public static Service createBridgeNodePortService(String clusterName, String namespace, String serviceName) {
        Map<String, String> map = new HashMap<>();
        map.put(Labels.STRIMZI_CLUSTER_LABEL, clusterName);
        map.put(Labels.STRIMZI_KIND_LABEL, "KafkaBridge");
        map.put(Labels.STRIMZI_NAME_LABEL, clusterName + "-bridge");

        // Create node port service for expose bridge outside Kubernetes
        return KubernetesResource.getSystemtestsServiceResource(serviceName, Constants.HTTP_BRIDGE_DEFAULT_PORT, namespace, "TCP")
                    .editSpec()
                        .withType("NodePort")
                        .withSelector(map)
                    .endSpec().build();
    }

    // ======================================= MESSAGE CHECKING ======================================================

    public static void checkSendResponse(JsonObject response, int messageCount) {
        JsonArray offsets = response.getJsonArray("offsets");
        assertThat(offsets.size(), is(messageCount));
        for (int i = 0; i < messageCount; i++) {
            JsonObject metadata = offsets.getJsonObject(i);
            assertThat(metadata.getInteger("partition"), is(0));
            assertThat(metadata.getInteger("offset"), is(i));
            LOGGER.debug("offset size: {}, partition: {}, offset size: {}", offsets.size(), metadata.getInteger("partition"), metadata.getLong("offset"));
        }
    }
}
