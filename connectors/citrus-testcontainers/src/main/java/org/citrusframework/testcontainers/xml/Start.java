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

package org.citrusframework.testcontainers.xml;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Stream;

import jakarta.xml.bind.annotation.XmlAccessType;
import jakarta.xml.bind.annotation.XmlAccessorType;
import jakarta.xml.bind.annotation.XmlAttribute;
import jakarta.xml.bind.annotation.XmlElement;
import jakarta.xml.bind.annotation.XmlRootElement;
import jakarta.xml.bind.annotation.XmlType;
import jakarta.xml.bind.annotation.XmlValue;
import org.citrusframework.TestActor;
import org.citrusframework.spi.ReferenceResolver;
import org.citrusframework.spi.ReferenceResolverAware;
import org.citrusframework.spi.Resources;
import org.citrusframework.testcontainers.TestContainersSettings;
import org.citrusframework.testcontainers.actions.AbstractTestcontainersAction;
import org.citrusframework.testcontainers.actions.StartTestcontainersAction;
import org.citrusframework.testcontainers.aws2.LocalStackContainer;
import org.citrusframework.testcontainers.aws2.StartLocalStackAction;
import org.citrusframework.testcontainers.kafka.StartKafkaAction;
import org.citrusframework.testcontainers.mongodb.StartMongoDBAction;
import org.citrusframework.testcontainers.postgresql.StartPostgreSQLAction;
import org.citrusframework.testcontainers.redpanda.StartRedpandaAction;
import org.citrusframework.util.ObjectHelper;

@XmlRootElement(name = "start")
public class Start extends AbstractTestcontainersAction.Builder<StartTestcontainersAction<?>, Start> implements ReferenceResolverAware {

    private StartTestcontainersAction.AbstractBuilder<?, ?, ?> delegate;

    @XmlElement
    public void setContainer(Container container) {
        StartTestcontainersAction.Builder<?> builder = new StartTestcontainersAction.Builder<>();
        configureStartActionBuilder(builder, container);
        delegate = builder;
    }

    @XmlElement(name = "localstack")
    public void setLocalStack(LocalStack container) {
        StartLocalStackAction.Builder builder = new StartLocalStackAction.Builder();
        if (container.getVersion() != null) {
            builder.version(container.getVersion());
        }

        configureStartActionBuilder(builder, container);

        if (container.getServices() != null) {
            container.getServices().getServices().forEach(service -> builder.withService(LocalStackContainer.Service.valueOf(service)));
        }

        if (container.getServiceList() != null) {
            Stream.of(container.getServiceList().split(",")).forEach(service -> builder.withService(LocalStackContainer.Service.valueOf(service)));
        }

        delegate = builder;
    }

    @XmlElement(name = "mongodb")
    public void setMongoDB(MongoDB container) {
        StartMongoDBAction.Builder builder = new StartMongoDBAction.Builder();
        if (container.getVersion() != null) {
            builder.version(container.getVersion());
        }

        configureStartActionBuilder(builder, container);

        delegate = builder;
    }

    @XmlElement(name = "kafka")
    public void setKafka(Kafka container) {
        StartKafkaAction.Builder builder = new StartKafkaAction.Builder();
        if (container.getVersion() != null) {
            builder.version(container.getVersion());
        }

        configureStartActionBuilder(builder, container);

        delegate = builder;
    }

    @XmlElement(name = "redpanda")
    public void setRedpanda(Redpanda container) {
        StartRedpandaAction.Builder builder = new StartRedpandaAction.Builder();
        if (container.getVersion() != null) {
            builder.version(container.getVersion());
        }

        configureStartActionBuilder(builder, container);

        delegate = builder;
    }

    @XmlElement(name = "postgresql")
    public void setPostgreSQL(PostgreSQL container) {
        StartPostgreSQLAction.Builder builder = new StartPostgreSQLAction.Builder();
        if (container.getVersion() != null) {
            builder.version(container.getVersion());
        }

        configureStartActionBuilder(builder, container);

        if (container.getDataSourceName() != null) {
            builder.dataSourceName(container.getDataSourceName());
        }

        if (container.getDatabase() != null) {
            builder.databaseName(container.getDatabase());
        }

        if (container.getUsername() != null) {
            builder.username(container.getUsername());
        }

        if (container.getPassword() != null) {
            builder.password(container.getPassword());
        }

        if (container.getInitScript() != null) {
            if (container.getInitScript().getFile() != null) {
                builder.initScript(Resources.create(container.getInitScript().getFile()));
            }

            if (container.getInitScript().getValue() != null) {
                builder.initScript(container.getInitScript().getValue());
            }
        }

        delegate = builder;
    }

    @Override
    public Start description(String description) {
        delegate.description(description);
        return this;
    }

    @Override
    public Start actor(TestActor actor) {
        delegate.actor(actor);
        return this;
    }

    @Override
    public void setReferenceResolver(ReferenceResolver referenceResolver) {
        this.delegate.setReferenceResolver(referenceResolver);
    }

    @Override
    public StartTestcontainersAction<?> doBuild() {
        ObjectHelper.assertNotNull(delegate);
        return delegate.build();
    }

    private void configureStartActionBuilder(StartTestcontainersAction.AbstractBuilder<?, ?, ?> builder, Container container) {
        builder.containerName(container.getName());
        builder.serviceName(container.getServiceName());
        builder.image(container.getImage());

        builder.autoRemove(container.isAutoRemove());

        if (container.getStartUpTimeout() > 0) {
            builder.withStartupTimeout(container.getStartUpTimeout());
        }

        if (container.getCommand() != null) {
            builder.withCommand(container.getCommand().split(" "));
        }

        if (container.getEnvironmentVariables() != null) {
            container.getEnvironmentVariables().getVariables().forEach(variable -> {
                builder.withEnv(variable.getName(), variable.getValue());
            });
        }

        if (container.getLabels() != null) {
            container.getLabels().getLabels().forEach(label -> {
                builder.withLabel(label.getName(), label.getValue());
            });
        }
    }

    @XmlAccessorType(XmlAccessType.FIELD)
    @XmlType(name = "", propOrder = {
            "labels",
            "environmentVariables",
    })
    public static class Container {

        @XmlAttribute
        private String name;

        @XmlAttribute(name = "service-name")
        private String serviceName;

        @XmlAttribute
        private String image;

        @XmlAttribute(name = "startup-timeout")
        private int startUpTimeout;

        @XmlAttribute
        protected String command;

        @XmlAttribute(name = "auto-remove")
        protected boolean autoRemove = TestContainersSettings.isAutoRemoveResources();

        @XmlElement(name = "env")
        protected EnvironmentVariables environmentVariables;

        @XmlElement
        protected Labels labels;

        public String getName() {
            return name;
        }

        public void setName(String name) {
            this.name = name;
        }

        public String getServiceName() {
            return serviceName;
        }

        public void setServiceName(String serviceName) {
            this.serviceName = serviceName;
        }

        public String getImage() {
            return image;
        }

        public void setImage(String image) {
            this.image = image;
        }

        public String getCommand() {
            return command;
        }

        public void setCommand(String command) {
            this.command = command;
        }

        public boolean isAutoRemove() {
            return autoRemove;
        }

        public void setAutoRemove(boolean autoRemove) {
            this.autoRemove = autoRemove;
        }

        public int getStartUpTimeout() {
            return startUpTimeout;
        }

        public void setStartUpTimeout(int startUpTimeout) {
            this.startUpTimeout = startUpTimeout;
        }

        public EnvironmentVariables getEnvironmentVariables() {
            return environmentVariables;
        }

        public void setEnvironmentVariables(EnvironmentVariables environmentVariables) {
            this.environmentVariables = environmentVariables;
        }

        public Labels getLabels() {
            return labels;
        }

        public void setLabels(Labels labels) {
            this.labels = labels;
        }
    }

    @XmlAccessorType(XmlAccessType.FIELD)
    @XmlType(name = "", propOrder = {
            "services"
    })
    public static class LocalStack extends Container {

        @XmlAttribute
        protected String version;

        @XmlAttribute(name = "services")
        protected String serviceList;

        @XmlElement
        protected Services services;


        public String getVersion() {
            return version;
        }

        public void setVersion(String version) {
            this.version = version;
        }

        public String getServiceList() {
            return serviceList;
        }

        public void setServiceList(String serviceList) {
            this.serviceList = serviceList;
        }

        public Services getServices() {
            return services;
        }

        public void setServices(Services services) {
            this.services = services;
        }

        @XmlAccessorType(XmlAccessType.FIELD)
        @XmlType(name = "", propOrder = {
                "services"
        })
        public static class Services {

            @XmlElement(name = "service")
            private List<String> services;

            public List<String> getServices() {
                if (services == null) {
                    services = new ArrayList<>();
                }
                return services;
            }

            public void setServices(List<String> services) {
                this.services = services;
            }
        }
    }

    @XmlAccessorType(XmlAccessType.FIELD)
    @XmlType(name = "", propOrder = {
    })
    public static class MongoDB extends Container {

        @XmlAttribute
        protected String version;

        public String getVersion() {
            return version;
        }

        public void setVersion(String version) {
            this.version = version;
        }
    }

    @XmlAccessorType(XmlAccessType.FIELD)
    @XmlType(name = "", propOrder = {
    })
    public static class Kafka extends Container {

        @XmlAttribute
        protected String version;

        public String getVersion() {
            return version;
        }

        public void setVersion(String version) {
            this.version = version;
        }
    }

    @XmlAccessorType(XmlAccessType.FIELD)
    @XmlType(name = "", propOrder = {
    })
    public static class Redpanda extends Container {

        @XmlAttribute
        protected String version;

        public String getVersion() {
            return version;
        }

        public void setVersion(String version) {
            this.version = version;
        }
    }

    @XmlAccessorType(XmlAccessType.FIELD)
    @XmlType(name = "", propOrder = {
            "initScript"
    })
    public static class PostgreSQL extends Container {

        @XmlAttribute
        protected String version;

        @XmlAttribute(name = "datasource-name")
        protected String dataSourceName;

        @XmlAttribute
        protected String database;

        @XmlAttribute
        protected String username;

        @XmlAttribute
        protected String password;

        @XmlElement(name = "init-script")
        protected InitScript initScript;

        public String getVersion() {
            return version;
        }

        public void setVersion(String version) {
            this.version = version;
        }

        public String getDataSourceName() {
            return dataSourceName;
        }

        public void setDataSourceName(String dataSourceName) {
            this.dataSourceName = dataSourceName;
        }

        public String getDatabase() {
            return database;
        }

        public void setDatabase(String database) {
            this.database = database;
        }

        public String getUsername() {
            return username;
        }

        public void setUsername(String username) {
            this.username = username;
        }

        public String getPassword() {
            return password;
        }

        public void setPassword(String password) {
            this.password = password;
        }

        public InitScript getInitScript() {
            return initScript;
        }

        public void setInitScript(InitScript initScript) {
            this.initScript = initScript;
        }

        @XmlAccessorType(XmlAccessType.FIELD)
        @XmlType(name = "")
        public static class InitScript {

            @XmlAttribute
            protected String file;
            @XmlValue
            protected String value;

            public String getFile() {
                return file;
            }

            public void setFile(String file) {
                this.file = file;
            }

            public String getValue() {
                return value;
            }

            public void setValue(String value) {
                this.value = value;
            }

        }
    }

    @XmlAccessorType(XmlAccessType.FIELD)
    @XmlType(name = "", propOrder = {
            "variables"
    })
    public static class EnvironmentVariables {

        @XmlElement(name = "variable")
        private List<Variable> variables;

        public void setVariables(List<Variable> variables) {
            this.variables = variables;
        }

        public List<Variable> getVariables() {
            if (variables == null) {
                variables = new ArrayList<>();
            }
            return variables;
        }

        @XmlAccessorType(XmlAccessType.FIELD)
        @XmlType(name = "")
        public static class Variable {

            @XmlAttribute(name = "name", required = true)
            protected String name;
            @XmlAttribute(name = "value", required = true)
            protected String value;

            public String getName() {
                return name;
            }

            public void setName(String value) {
                this.name = value;
            }

            public String getValue() {
                return value;
            }

            public void setValue(String value) {
                this.value = value;
            }

        }
    }

    @XmlAccessorType(XmlAccessType.FIELD)
    @XmlType(name = "", propOrder = {
            "labels"
    })
    public static class Labels {

        @XmlElement(name = "label")
        private List<Label> labels;

        public void setLabels(List<Label> labels) {
            this.labels = labels;
        }

        public List<Label> getLabels() {
            if (labels == null) {
                labels = new ArrayList<>();
            }
            return labels;
        }

        @XmlAccessorType(XmlAccessType.FIELD)
        @XmlType(name = "")
        public static class Label {

            @XmlAttribute(name = "name", required = true)
            protected String name;
            @XmlAttribute(name = "value", required = true)
            protected String value;

            public String getName() {
                return name;
            }

            public void setName(String value) {
                this.name = value;
            }

            public String getValue() {
                return value;
            }

            public void setValue(String value) {
                this.value = value;
            }

        }
    }
}
