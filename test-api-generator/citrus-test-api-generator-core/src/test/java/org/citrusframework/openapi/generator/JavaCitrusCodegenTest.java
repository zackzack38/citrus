package org.citrusframework.openapi.generator;

import static org.assertj.core.api.Assertions.assertThat;
import static org.citrusframework.openapi.generator.CitrusJavaCodegen.CODEGEN_NAME;

import java.io.File;
import java.io.IOException;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import org.junit.jupiter.api.Test;
import org.openapitools.codegen.ClientOptInput;
import org.openapitools.codegen.CodegenConfigLoader;
import org.openapitools.codegen.DefaultGenerator;
import org.openapitools.codegen.config.CodegenConfigurator;

/**
 * This test validates the code generation process.
 * <p>
 * It may also serve as an entry point for debugging the code generation process. When executed in debug mode, it allows you to
 * step through the generation process and inspect the resulting output in the specified output directory.
 * <p>
 * To debug the code generator:
 * <ol>
 *   <li>Set a breakpoint in the {@code postProcessOperationsWithModels()} method of {@code JavaCitrusCodegen.java}.</li>
 *   <li>In your IDE, launch this test by right-clicking and selecting Debug As > JUnit Test.</li>
 * </ol>
 */

class JavaCitrusCodegenTest {

    /**
     * Get the absolute path to the test resources directory.
     *
     * @param pathToFileInTestResources The file within {@code src/test/resources} to look for
     * @return the absolute path to the file
     */
    static String getAbsoluteTestResourcePath(String pathToFileInTestResources) {
        URL resourceUrl = JavaCitrusCodegenTest.class.getClassLoader().getResource(pathToFileInTestResources);
        assert resourceUrl != null;
        File inputSpecFile = new File(resourceUrl.getFile());
        return inputSpecFile.getAbsolutePath();
    }

    /**
     * Get the absolute path to the project's target directory.
     *
     * @param pathToFileInTargetDirectory The file within {@code target} to look for
     * @return the absolute path to the file
     */
    static String getAbsoluteTargetDirectoryPath(String pathToFileInTargetDirectory) {
        String projectBaseDir = System.getProperty("user.dir"); // Base directory of the project
        File outputDirFile = new File(projectBaseDir, "target/" + pathToFileInTargetDirectory);
        return outputDirFile.getAbsolutePath();
    }

    @Test
    void retrieveGeneratorBsSpi() {
        CitrusJavaCodegen codegen = (CitrusJavaCodegen) CodegenConfigLoader.forName("java-citrus");
        assertThat(codegen).isNotNull();
    }

    @Test
    void arePredefinedValuesNotEmptyTest() {
        CitrusJavaCodegen codegen = new CitrusJavaCodegen();

        assertThat(codegen.getName()).isEqualTo(CODEGEN_NAME);
        assertThat(codegen.getHelp()).isNotEmpty();
        assertThat(codegen.getHttpClient()).isNotEmpty();
        assertThat(codegen.getOpenapiSchema()).isNotEmpty();
        assertThat(codegen.getApiPrefix()).isNotEmpty();
        assertThat(codegen.getHttpPathPrefix()).isNotEmpty();
        assertThat(codegen.getTargetXmlnsNamespace()).isNull();
        assertThat(codegen.getGeneratedSchemaFolder()).isNotEmpty();
    }

    @Test
    void areAdditionalPropertiesProcessedTest() {
        final String httpClient = "myTestEndpoint";
        final String openapiSchema = "testSchema";
        final String prefix = "testPrefix";
        final String httpPathPrefix = "test/path";
        final String targetXmlnsNamespace = "http://www.citrusframework.org/schema/test/extension";
        final String generatedSchemaFolder = "generatedResourceFolder";

        Map<String, Object> properties = new HashMap<>();
        properties.put(CitrusJavaCodegen.API_ENDPOINT, httpClient);
        properties.put(CitrusJavaCodegen.GENERATED_SCHEMA_FOLDER, generatedSchemaFolder);
        properties.put(CitrusJavaCodegen.HTTP_PATH_PREFIX, httpPathPrefix);
        properties.put(CitrusJavaCodegen.OPENAPI_SCHEMA, openapiSchema);
        properties.put(CitrusJavaCodegen.PREFIX, prefix);
        properties.put(CitrusJavaCodegen.TARGET_XMLNS_NAMESPACE, targetXmlnsNamespace);

        CitrusJavaCodegen codegen = new CitrusJavaCodegen();
        codegen.additionalProperties().putAll(properties);
        codegen.processOpts();

        assertThat(codegen.getApiPrefix()).isEqualTo(prefix);
        assertThat(codegen.getGeneratedSchemaFolder()).isEqualTo(generatedSchemaFolder);
        assertThat(codegen.getHttpClient()).isEqualTo(httpClient);
        assertThat(codegen.getHttpPathPrefix()).isEqualTo(httpPathPrefix);
        assertThat(codegen.getOpenapiSchema()).isEqualTo(openapiSchema);
        assertThat(codegen.getTargetXmlnsNamespace()).isEqualTo(targetXmlnsNamespace);
    }

    @Test
    void areReservedWordsEscapedTest() throws IOException {
        String absoluteInputSpecPath = getAbsoluteTestResourcePath("apis/petstore_reservedWords.yaml");
        String absoluteOutputDirPath = getAbsoluteTargetDirectoryPath("JavaCitrusCodegenTest/petstore_escapedWords");

        final CodegenConfigurator configurator = new CodegenConfigurator()
            .setGeneratorName(CODEGEN_NAME)
            .setInputSpec(absoluteInputSpecPath)
            .setOutputDir(absoluteOutputDirPath);

        final ClientOptInput clientOptInput = configurator.toClientOptInput();
        DefaultGenerator generator = new DefaultGenerator();
        List<File> outputFiles = generator.opts(clientOptInput).generate();

        Optional<File> file = outputFiles.stream()
            .filter(x -> "PetApi.java".equals(x.getName()))
            .findFirst();

        assertThat(file).isPresent();

        List<String> lines = Files.readAllLines(file.get().toPath(), StandardCharsets.UTF_8);

        // "name" is a reserved word, so it should be escaped with an underline for the second parameter
        assertThat(
            lines.stream()
                .filter(x -> x.contains("\"name\", this._name"))
                .count())
            .isEqualTo(1L);
    }

    @Test
    void arePathParamsFieldsPresent() throws IOException {
        String absoluteInputSpecPath = getAbsoluteTestResourcePath("apis/petstore.yaml");
        String absoluteOutputDirPath = getAbsoluteTargetDirectoryPath("JavaCitrusCodegenTest/petstore");

        final CodegenConfigurator configurator = new CodegenConfigurator()
            .setGeneratorName(CODEGEN_NAME)
            .setInputSpec(absoluteInputSpecPath)
            .setOutputDir(absoluteOutputDirPath);

        final ClientOptInput clientOptInput = configurator.toClientOptInput();
        DefaultGenerator generator = new DefaultGenerator();
        List<File> outputFiles = generator.opts(clientOptInput).generate();

        Optional<File> file = outputFiles.stream()
            .filter(x -> "PetApi.java".equals(x.getName()))
            .findFirst();

        assertThat(file).isPresent();

        List<String> lines = Files.readAllLines(file.get().toPath(), StandardCharsets.UTF_8);

        // "name" is a reserved word, so it should be escaped with an underline for the second parameter
        assertThat(
            lines.stream()
                .filter(x -> x.contains("private String petId;"))
                .count())
            .isEqualTo(4L);
        assertThat(
            lines.stream()
                .filter(x -> x.contains(
                    "endpoint = endpoint.replace(\"{\" + \"petId\" + \"}\", petId);"))
                .count())
            .isEqualTo(4L);
    }

    @Test
    void areBasicAuthFieldsPresent() throws IOException {
        String absoluteInputSpecPath = getAbsoluteTestResourcePath("apis/petstore.yaml");
        String absoluteOutputDirPath = getAbsoluteTargetDirectoryPath("JavaCitrusCodegenTest/petstore");

        final CodegenConfigurator configurator = new CodegenConfigurator()
            .setGeneratorName(CODEGEN_NAME)
            .setInputSpec(absoluteInputSpecPath)
            .setOutputDir(absoluteOutputDirPath);

        final ClientOptInput clientOptInput = configurator.toClientOptInput();
        DefaultGenerator generator = new DefaultGenerator();
        List<File> outputFiles = generator.opts(clientOptInput).generate();

        Optional<File> file = outputFiles.stream()
            .filter(x -> "PetApi.java".equals(x.getName()))
            .findFirst();

        assertThat(file).isPresent();

        List<String> lines = Files.readAllLines(file.get().toPath(), StandardCharsets.UTF_8);

        // "name" is a reserved word, so it should be escaped with an underline for the second parameter
        assertThat(
            lines.stream()
                .filter(x -> x.contains("@Value(\"${\" + \"apiEndpoint.basic.username:#{null}}\")"))
                .count())
            .isEqualTo(1L);
        assertThat(
            lines.stream()
                .filter(x -> x.contains("private String basicUsername;"))
                .count())
            .isEqualTo(1L);
        assertThat(
            lines.stream()
                .filter(x ->
                    x.contains(
                        "messageBuilderSupport.header(\"Authorization\", \"Basic \" + Base64.getEncoder().encodeToString((context.replaceDynamicContentInString(basicUsername)+\":\"+context.replaceDynamicContentInString(basicPassword)).getBytes()));"
                    )
                )
                .count())
            .isEqualTo(1L);
    }
}
