/*
 * Copyright the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.citrusframework.testcontainers.actions;

import java.time.Duration;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.citrusframework.context.TestContext;
import org.citrusframework.testcontainers.TestContainersSettings;
import org.testcontainers.containers.GenericContainer;
import org.testcontainers.containers.Network;

import static org.citrusframework.testcontainers.TestcontainersHelper.getEnvVarName;
import static org.citrusframework.testcontainers.actions.TestcontainersActionBuilder.testcontainers;

public class StartTestcontainersAction<C extends GenericContainer<?>> extends AbstractTestcontainersAction {

    protected final String serviceName;
    protected final String containerName;
    private final C container;
    private final boolean autoRemoveResources;

    public StartTestcontainersAction(AbstractBuilder<C, ? extends StartTestcontainersAction<C>, ?> builder) {
        super("start", builder);

        this.serviceName = builder.serviceName;
        this.containerName = builder.containerName;
        this.container = builder.container;
        this.autoRemoveResources = builder.autoRemoveResources;
    }

    @Override
    public void doExecute(TestContext context) {
        container.start();

        if (containerName != null && !context.getReferenceResolver().isResolvable(containerName)) {
            context.getReferenceResolver().bind(containerName, container);
        }

        exposeConnectionSettings(container, context);

        if (autoRemoveResources) {
            context.doFinally(testcontainers()
                    .stop()
                    .container(container));
        }
    }

    /**
     * Sets the connection settings in current test context in the form of test variables.
     * @param container
     * @param context
     */
    protected void exposeConnectionSettings(C container, TestContext context) {
        if (container.getContainerId() != null) {
            String dockerContainerId = container.getContainerId().substring(0, 12);
            String dockerContainerName = container.getContainerName();

            if (dockerContainerName.startsWith("/")) {
                dockerContainerName = dockerContainerName.substring(1);
            }

            String containerType = containerName.toUpperCase().replaceAll("-", "_").replaceAll("\\.", "_");
            context.setVariable(getEnvVarName(containerType, "HOST"), container.getHost());
            context.setVariable(getEnvVarName(containerType, "CONTAINER_IP"), container.getHost());
            context.setVariable(getEnvVarName(containerType, "CONTAINER_ID"), dockerContainerId);
            context.setVariable(getEnvVarName(containerType, "CONTAINER_NAME"), dockerContainerName);
        }
    }

    protected C getContainer() {
        return container;
    }

    public static class Builder<C extends GenericContainer<?>> extends AbstractBuilder<C, StartTestcontainersAction<C>, Builder<C>> {
        @Override
        protected StartTestcontainersAction<C> doBuild() {
            return new StartTestcontainersAction<>(this);
        }
    }

    /**
     * Abstract start action builder.
     */
    public static abstract class AbstractBuilder<C extends GenericContainer<?>, T extends StartTestcontainersAction<C>, B extends AbstractBuilder<C, T, B>> extends AbstractTestcontainersAction.Builder<T, B> {

        protected String image;
        protected String containerName;
        protected String serviceName;
        private final Map<String, String> labels = new HashMap<>();
        protected final Map<String, String> env = new HashMap<>();
        private final List<String> commandLine = new ArrayList<>();
        protected C container;
        protected Network network;
        protected Duration startupTimeout = Duration.ofSeconds(TestContainersSettings.getStartupTimeout());
        private boolean autoRemoveResources = TestContainersSettings.isAutoRemoveResources();

        public B containerName(String name) {
            this.containerName = name;
            return self;
        }

        public B serviceName(String name) {
            this.serviceName = name;
            return self;
        }

        public B image(String image) {
            this.image = image;
            return self;
        }

        public B container(C container) {
            this.container = container;
            return self;
        }

        public B container(String name, C container) {
            this.containerName = name;
            this.container = container;
            return self;
        }

        public B withStartupTimeout(int timeout) {
            this.startupTimeout = Duration.ofSeconds(timeout);
            return self;
        }

        public B withStartupTimeout(Duration timeout) {
            this.startupTimeout = timeout;
            return self;
        }

        public B withNetwork() {
            network = Network.newNetwork();
            return self;
        }

        public B withNetwork(Network network) {
            this.network = network;
            return self;
        }

        public B withoutNetwork() {
            network = null;
            return self;
        }

        public B withEnv(String key, String value) {
            this.env.put(key, value);
            return self;
        }

        public B withEnv(Map<String, String> env) {
            this.env.putAll(env);
            return self;
        }

        public B withLabel(String label, String value) {
            this.labels.put(label, value);
            return self;
        }

        public B withLabels(Map<String, String> labels) {
            this.labels.putAll(labels);
            return self;
        }

        public B withCommand(String... command) {
            this.commandLine.addAll(List.of(command));
            return self;
        }

        public B autoRemove(boolean enabled) {
            this.autoRemoveResources = enabled;
            return self;
        }

        protected void prepareBuild() {
        }

        @Override
        public T build() {
            prepareBuild();

            if (container == null) {
                container = (C) new GenericContainer<>(image);

                if (network != null) {
                    container.withNetwork(network);
                    container.withNetworkAliases(containerName);
                }

                container.withStartupTimeout(startupTimeout);
            }

            container.withLabels(labels);
            container.withEnv(env);

            if (!commandLine.isEmpty()) {
                container.withCommand(commandLine.toArray(String[]::new));
            }

            return doBuild();
        }
    }
}
