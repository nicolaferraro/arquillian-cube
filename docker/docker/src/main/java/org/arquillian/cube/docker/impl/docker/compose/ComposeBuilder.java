package org.arquillian.cube.docker.impl.docker.compose;

import static org.arquillian.cube.docker.impl.util.YamlUtil.asListOfString;
import static org.arquillian.cube.docker.impl.util.YamlUtil.asMap;

import java.nio.file.Path;
import java.util.Arrays;
import java.util.Collection;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

import org.arquillian.cube.docker.impl.client.config.CubeContainer;
import org.arquillian.cube.docker.impl.client.config.DockerCompositions;
import org.arquillian.cube.docker.impl.client.config.Network;

public class ComposeBuilder {

    private DockerCompositions configuration;

    private static final String SERVICES = "services";
    private static final String NETWORKS = "networks";

    private static final String NETWORK_NAME_SUFFIX = "_default";

    private Path dockerComposeRootLocation;

    ComposeBuilder(Path dockerComposeRootLocation) {
        this(dockerComposeRootLocation, new DockerCompositions());
    }

    private ComposeBuilder(Path dockerComposeRootLocation,
                           DockerCompositions configuration) {
        this.dockerComposeRootLocation = dockerComposeRootLocation;
        this.configuration = configuration;
    }

    @SuppressWarnings("unchecked")
    public DockerCompositions build(Map<String, Object> dockerComposeContainerDefinition) {
        if (dockerComposeContainerDefinition.containsKey(SERVICES)) {
            Map<String, Object> servicesDefinition = asMap(
                    dockerComposeContainerDefinition, SERVICES);
            Set<String> keys = servicesDefinition.keySet();
            for (String key : keys) {
                ContainerBuilder services = new ContainerBuilder(this.dockerComposeRootLocation);
                CubeContainer cubeContainer = services.build(asMap(servicesDefinition, key), DockerComposeConverter.DOCKER_COMPOSE_VERSION_2_VALUE);
                if (!dockerComposeContainerDefinition.containsKey(NETWORKS)) {
                    String networkName = getDefaultNetworkName();
                    cubeContainer.setNetworks(Arrays.asList(networkName));
                    cubeContainer.setNetworkMode(networkName);
                } else {
                    final Map<String, Object> serviceDefinition = asMap(servicesDefinition, key);
                    if (serviceDefinition.containsKey(NETWORKS)) {
                        final Collection<String> serviceNetworks = asListOfString(serviceDefinition, NETWORKS);
                        if (!serviceNetworks.isEmpty()) {
                            cubeContainer.setNetworks(new HashSet<>(serviceNetworks));
                            cubeContainer.setNetworkMode(serviceNetworks.iterator().next());
                        } else {
                            throw new IllegalArgumentException("Networks not mentioned under services networks section.");
                        }
                    }
                }
                this.configuration.add(key, cubeContainer);
            }
        }
        if (dockerComposeContainerDefinition.containsKey(NETWORKS)) {
            Map<String, Object> networksDefinition = asMap(
                    dockerComposeContainerDefinition, NETWORKS);
            Set<String> keys = networksDefinition.keySet();
            for (String key : keys) {
                NetworkBuilder networks = new NetworkBuilder();
                Network network = networks.build(asMap(networksDefinition, key));
                this.configuration.add(key, network);
            }
        } else {
            String networkName = getDefaultNetworkName();
            NetworkBuilder networkBuilder = new NetworkBuilder();
            Network network = networkBuilder.withDefaultDriver().build();
            this.configuration.add(networkName, network);
        }
        return this.configuration;
    }

    private String getDefaultNetworkName() {
        return this.dockerComposeRootLocation.toFile()
                .getAbsoluteFile()
                .getParentFile()
                .getName() + NETWORK_NAME_SUFFIX;
    }

}