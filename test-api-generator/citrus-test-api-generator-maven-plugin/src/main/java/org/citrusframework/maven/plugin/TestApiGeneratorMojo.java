package org.citrusframework.maven.plugin;

import static org.citrusframework.openapi.generator.JavaCitrusCodegen.API_ENDPOINT;
import static org.citrusframework.openapi.generator.JavaCitrusCodegen.API_TYPE;
import static org.citrusframework.openapi.generator.JavaCitrusCodegen.PREFIX;
import static org.citrusframework.openapi.generator.JavaCitrusCodegen.TARGET_XMLNS_NAMESPACE;
import static java.lang.String.format;
import static org.apache.commons.lang3.StringUtils.isBlank;

import com.google.common.annotations.VisibleForTesting;
import java.io.File;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import org.apache.maven.plugin.AbstractMojo;
import org.apache.maven.plugin.MojoExecution;
import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.plugins.annotations.Component;
import org.apache.maven.plugins.annotations.LifecyclePhase;
import org.apache.maven.plugins.annotations.Mojo;
import org.apache.maven.plugins.annotations.Parameter;
import org.apache.maven.plugins.annotations.ResolutionScope;
import org.apache.maven.project.MavenProject;
import org.openapitools.codegen.plugin.CodeGenMojo;
import org.sonatype.plexus.build.incremental.BuildContext;
import org.sonatype.plexus.build.incremental.DefaultBuildContext;

/**
 * The Citrus OpenAPI Generator Maven Plugin is designed to facilitate the integration of multiple OpenAPI specifications
 * into the Citrus testing framework by automatically generating necessary test classes and XSDs. This plugin wraps the
 * {@code CodeGenMojo} and extends its functionality to support multiple API configurations.
 * <p>
 * Features:
 * - Multiple API Configurations: Easily configure multiple OpenAPI specifications to generate test APIs with specific prefixes.
 * - Citrus Integration: Generates classes and XSDs tailored for use within the Citrus framework, streamlining the process
 *   of creating robust integration tests.
 * </p>
 *
 * @author Thorsten Schlathoelter
 *
 */
@Mojo(
    name = "create-test-api",
    defaultPhase = LifecyclePhase.GENERATE_TEST_SOURCES,
    requiresDependencyCollection = ResolutionScope.TEST,
    requiresDependencyResolution = ResolutionScope.TEST,
    threadSafe = true
)
public class TestApiGeneratorMojo extends AbstractMojo {

    public static final String DEFAULT_SOURCE_FOLDER = "generated-test-sources";
    public static final String DEFAULT_RESOURCE_FOLDER = "generated-test-resources";
    public static final String DEFAULT_BASE_PACKAGE = "org.citrusframework.automation.%PREFIX%.%VERSION%";
    public static final String DEFAULT_INVOKER_PACKAGE = DEFAULT_BASE_PACKAGE;
    public static final String DEFAULT_API_PACKAGE = DEFAULT_BASE_PACKAGE+".api";
    public static final String DEFAULT_MODEL_PACKAGE = DEFAULT_BASE_PACKAGE+".model";
    public static final String DEFAULT_SCHEMA_FOLDER_TEMPLATE = "schema/xsd/%VERSION%";
    public static final ApiType DEFAULT_API_TYPE = ApiType.REST;

    /**
     * Marker fragment in the schema name of a generated schema. Used to distinguish generated from non generated values, when manipulating
     * spring meta-data files.
     */
    public static final String CITRUS_TEST_SCHEMA = "citrus-test-schema";

    /**
     * Specifies the default target namespace template. When changing the default value, it's important to maintain the 'citrus-test-schema'
     * part, as this name serves to differentiate between generated and non-generated schemas. This differentiation aids in the creation of
     * supporting Spring files such as 'spring.handlers' and 'spring.schemas'.
     */
    public static final String DEFAULT_TARGET_NAMESPACE_TEMPLATE =
        "http://www.citrusframework.org/" + CITRUS_TEST_SCHEMA + "/%VERSION%/%PREFIX%-api";

    /**
     * The default META-INF folder. Note that it points into the main resources, not generated resources, to allow for non generated
     * schemas/handlers. See also {@link TestApiGeneratorMojo}#DEFAULT_TARGET_NAMESPACE_TEMPLATE.
     */
    public static final String DEFAULT_META_INF_FOLDER = "target/generated-test-resources/META-INF";

    @Component
    private final BuildContext buildContext = new DefaultBuildContext();

    @Parameter(defaultValue = "${project}", readonly = true)
    private MavenProject mavenProject;

    @Parameter(defaultValue = "${mojoExecution}", readonly = true)
    private MojoExecution mojoExecution;

    /**
     * sourceFolder: specifies the location to which the sources are generated. Defaults to 'generated-test-sources'.
     */
    public static final String SOURCE_FOLDER_PROPERTY = "citrus.test.api.generator.source.folder";
    @Parameter(property = SOURCE_FOLDER_PROPERTY, defaultValue = DEFAULT_SOURCE_FOLDER)
    @SuppressWarnings({"FieldCanBeLocal", "FieldMayBeFinal"})  // Maven injected
    private String sourceFolder = DEFAULT_SOURCE_FOLDER;

    /**
     * resourceFolder: specifies the location to which the resources are generated. Defaults to 'generated-test-resources'.
     */
    public static final String RESOURCE_FOLDER_PROPERTY = "citrus.test.api.generator.resource.folder";
    @Parameter(property = RESOURCE_FOLDER_PROPERTY, defaultValue = DEFAULT_RESOURCE_FOLDER)
    @SuppressWarnings({"FieldCanBeLocal", "FieldMayBeFinal"})  // Maven injected
    private String resourceFolder = DEFAULT_RESOURCE_FOLDER;

    /**
     * schemaFolder: specifies the location for the generated xsd schemas. Defaults to 'schema/xsd/%VERSION%'
     */
    public static final String API_SCHEMA_FOLDER = "citrus.test.api.generator.schema.folder";
    @Parameter(property = API_SCHEMA_FOLDER, defaultValue = DEFAULT_SCHEMA_FOLDER_TEMPLATE)
    @SuppressWarnings({"FieldCanBeLocal", "FieldMayBeFinal"})  // Maven injected
    private String schemaFolder = DEFAULT_SCHEMA_FOLDER_TEMPLATE;

    /**
     * metaInfFolder: specifies the location to which the resources are generated. Defaults to 'generated-resources'.
     */
    public static final String META_INF_FOLDER = "citrus.test.api.generator.meta.inf.folder";
    @Parameter(property = META_INF_FOLDER, defaultValue = DEFAULT_META_INF_FOLDER)
    @SuppressWarnings({"FieldCanBeLocal", "FieldMayBeFinal"})  // Maven injected
    private String metaInfFolder = DEFAULT_META_INF_FOLDER;

    /**
     * resourceFolder: specifies the location to which the resources are generated. Defaults to 'generated-resources'.
     */
    public static final String GENERATE_SPRING_INTEGRATION_FILES = "citrus.test.api.generator.generate.spring.integration.files";
    @Parameter(property = GENERATE_SPRING_INTEGRATION_FILES, defaultValue = "true")
    @SuppressWarnings({"FieldCanBeLocal", "FieldMayBeFinal"})  // Maven injected
    private boolean generateSpringIntegrationFiles = true;

    @Parameter
    private List<ApiConfig> apis;

    protected MavenProject getMavenProject() {
        return mavenProject;
    }

    protected void setMavenProject(MavenProject mavenProject) {
        this.mavenProject = mavenProject;
    }

    public List<ApiConfig> getApiConfigs() {
        return apis;
    }

    public String metaInfFolder() {
        return metaInfFolder;
    }

    @VisibleForTesting
    void setMojoExecution(MojoExecution mojoExecution) {
        this.mojoExecution = mojoExecution;
    }

    /**
     * Returns the fully qualified schema folder
     */
    public String schemaFolder(ApiConfig apiConfig) {
        return replaceDynamicVars(schemaFolder, apiConfig.getPrefix(), apiConfig.getVersion());
    }

    @Override
    public void execute() throws MojoExecutionException {

        for (int index = 0; index < apis.size(); index++) {
            ApiConfig apiConfig = apis.get(index);
            validateApiConfig(index, apiConfig);
            CodeGenMojo  codeGenMojo = configureCodeGenMojo(apiConfig);
            codeGenMojo.execute();
        }

        if (generateSpringIntegrationFiles) {
            new SpringMetaFileGenerator(this).generateSpringIntegrationMetaFiles();
        }
    }

    CodeGenMojo configureCodeGenMojo(ApiConfig apiConfig) throws MojoExecutionException {
        CodeGenMojo codeGenMojo = new CodeGenMojoWrapper()
            .resourceFolder(resourceFolder)
            .sourceFolder(sourceFolder)
            .schemaFolder(schemaFolder(apiConfig))
            .output(new File(mavenProject.getBuild().getDirectory()))
            .mojoExecution(mojoExecution)
            .project(mavenProject)
            .inputSpec(apiConfig.getSource())
            .configOptions(apiConfig.toConfigOptionsProperties());

        codeGenMojo.setPluginContext(getPluginContext());
        codeGenMojo.setBuildContext(buildContext);
        return codeGenMojo;
    }

    private void validateApiConfig(int apiIndex, ApiConfig apiConfig) throws MojoExecutionException {
        requireNonBlankParameter("prefix", apiIndex, apiConfig.getPrefix());
        requireNonBlankParameter("source", apiIndex, apiConfig.getSource());
    }

    private void requireNonBlankParameter(String name, int index, String parameterValue) throws MojoExecutionException {
        if (isBlank(parameterValue)) {
            throw new MojoExecutionException(format("Required parameter '%s' not set for api at index '%d'!", name, index));
        }
    }

    /**
     * Replace the placeholders '%PREFIX%' and '%VERSION%' in the given text.
     */
    static String replaceDynamicVars(String text, String prefix, String version) {

        if (text == null) {
            return null;
        }

        return text.replace("%PREFIX%", prefix)
            .replace(".%VERSION%", version != null ? "." + version : "")
            .replace("/%VERSION%", version != null ? "/" + version : "")
            .replace("-%VERSION%", version != null ? "-" + version : "")
            .replace("%VERSION%", version != null ? version : "");
    }

    /**
     * Replace the placeholders '%PREFIX%' and '%VERSION%' in the given text, performing a toLowerCase on the prefix.
     */
    static String replaceDynamicVarsToLowerCase(String text, String prefix, String version) {
        return replaceDynamicVars(text, prefix.toLowerCase(), version);
    }

    public enum ApiType {
        REST, SOAP
    }

    /**
     * Note that the default values are not properly set by maven processor. Therefore, the default values have been assigned additionally
     * on field level.
     */
    public static class ApiConfig {

        public static final String DEFAULT_ENDPOINT = "PREFIX_ENDPOINT";

        /**
         * prefix: specifies the prefixed used for the test api. Typically, an acronym for the application which is being tested.
         */
        public static final String API_PREFIX_PROPERTY = "citrus.test.api.generator.prefix";
        @Parameter(required = true, property = API_PREFIX_PROPERTY)
        private String prefix;

        /**
         * source: specifies the source of the test api.
         */
        public static final String API_SOURCE_PROPERTY = "citrus.test.api.generator.source";
        @Parameter(required = true, property = API_SOURCE_PROPERTY)
        private String source;

        /**
         * version: specifies the version of the api. May be null.
         */
        public static final String API_VERSION_PROPERTY = "citrus.test.api.generator.version";
        @Parameter(property = API_VERSION_PROPERTY)
        private String version;

        /**
         * endpoint: specifies the endpoint of the test api. Defaults to 'prefixEndpoint'.
         */
        public static final String API_ENDPOINT_PROPERTY = "citrus.test.api.generator.endpoint";
        @Parameter(property = API_ENDPOINT_PROPERTY, defaultValue = DEFAULT_ENDPOINT)
        private String endpoint = DEFAULT_ENDPOINT;

        /**
         * type: specifies the type of the test api. Defaults to 'REST'
         */
        public static final String API_TYPE_PROPERTY = "citrus.test.api.generator.type";
        @Parameter(property = API_TYPE_PROPERTY, defaultValue = "REST")
        private ApiType type = DEFAULT_API_TYPE;

        /**
         * useTags: specifies whether tags should be used by the generator. Defaults to 'true'. If useTags is set to true, the generator
         * will organize the generated code based on the tags defined in your API specification.
         */
        public static final String API_USE_TAGS_PROPERTY = "citrus.test.api.generator.use.tags";
        @Parameter(property = API_USE_TAGS_PROPERTY, defaultValue = "true")
        private boolean useTags = true;

        /**
         * invokerPackage: specifies the package for the test api classes. Defaults to
         * 'org.citrusframework.automation.%PREFIX%.%VERSION%'.
         */
        public static final String API_INVOKER_PACKAGE_PROPERTY = "citrus.test.api.generator.invoker.package";
        @Parameter(property = API_INVOKER_PACKAGE_PROPERTY, defaultValue = DEFAULT_INVOKER_PACKAGE)
        private String invokerPackage = DEFAULT_INVOKER_PACKAGE;

        /**
         * apiPackage: specifies the package for the test api classes. Defaults to
         * 'org.citrusframework.automation.%PREFIX%.%VERSION%.api'.
         */
        public static final String API_API_PACKAGE_PROPERTY = "citrus.test.api.generator.api.package";
        @Parameter(property = API_API_PACKAGE_PROPERTY, defaultValue = DEFAULT_API_PACKAGE)
        private String apiPackage = DEFAULT_API_PACKAGE;

        /**
         * modelPackage: specifies the package for the test api classes. Defaults to
         * 'org.citrusframework.automation.%PREFIX%.%VERSION%.model'.
         */
        public static final String API_MODEL_PACKAGE_PROPERTY = "citrus.test.api.generator.model.package";
        @Parameter(property = API_MODEL_PACKAGE_PROPERTY, defaultValue = DEFAULT_MODEL_PACKAGE)
        private String modelPackage = DEFAULT_MODEL_PACKAGE;

        /**
         * targetXmlNamespace: specifies the xml namespace to be used by the api. Defaults to
         * 'http://www.citrusframework.org/schema/%VERSION%/%PREFIX%-api'
         */
        @SuppressWarnings("JavadocLinkAsPlainText")
        public static final String API_NAMESPACE_PROPERTY = "citrus.test.api.generator.namespace";
        @Parameter(property = API_NAMESPACE_PROPERTY, defaultValue = DEFAULT_TARGET_NAMESPACE_TEMPLATE)
        private String targetXmlnsNamespace = DEFAULT_TARGET_NAMESPACE_TEMPLATE;


        public String getPrefix() {
            return prefix;
        }

        public void setPrefix(String prefix) {
            this.prefix = prefix;
        }

        public String getSource() {
            return source;
        }

        public void setSource(String source) {
            this.source = source;
        }

        public String getVersion() {
            return version;
        }

        public void setVersion(String version) {
            this.version = version;
        }

        public String qualifiedEndpoint() {
            return DEFAULT_ENDPOINT.equals(endpoint) ? getPrefix().toLowerCase() + "Endpoint" : endpoint;
        }

        public void setEndpoint(String endpoint) {
            this.endpoint = endpoint;
        }

        public ApiType getType() {
            return type;
        }

        public void setType(ApiType type) {
            this.type = type;
        }

        public void setUseTags(boolean useTags) {
            this.useTags = useTags;
        }

        public String getInvokerPackage() {
            return invokerPackage;
        }

        public void setInvokerPackage(String invokerPackage) {
            this.invokerPackage = invokerPackage;
        }

        public String getApiPackage() {
            return apiPackage;
        }

        public void setApiPackage(String apiPackage) {
            this.apiPackage = apiPackage;
        }

        public String getModelPackage() {
            return modelPackage;
        }

        public void setModelPackage(String modelPackage) {
            this.modelPackage = modelPackage;
        }

        public String getTargetXmlnsNamespace() {
            return targetXmlnsNamespace;
        }

        public void setTargetXmlnsNamespace(String targetXmlnsNamespace) {
            this.targetXmlnsNamespace = targetXmlnsNamespace;
        }

        Map<String, Object> toConfigOptionsProperties() {

            Map<String, Object> configOptionsProperties = new HashMap<>();
            configOptionsProperties.put(PREFIX, prefix);
            configOptionsProperties.put(API_ENDPOINT, qualifiedEndpoint());
            configOptionsProperties.put(API_TYPE, type.toString());
            configOptionsProperties.put(TARGET_XMLNS_NAMESPACE,
                replaceDynamicVarsToLowerCase(targetXmlnsNamespace, prefix, version));
            configOptionsProperties.put("invokerPackage",
                replaceDynamicVarsToLowerCase(invokerPackage, prefix, version));
            configOptionsProperties.put("apiPackage",
                replaceDynamicVarsToLowerCase(apiPackage, prefix, version));
            configOptionsProperties.put("modelPackage",
                replaceDynamicVarsToLowerCase(modelPackage, prefix, version));
            configOptionsProperties.put("useTags", useTags);

            return configOptionsProperties;
        }

    }


}
