package io.micronaut.sourcegen.example.plugin.maven;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

import java.io.File;
import java.util.List;

class TestMavenPluginTest extends AbstractMavenPluginTest {

    @Test
    void generateSimpleRecord() throws Exception {
        File pom = new File("src/test/resources/test-pom.xml");

        GenerateSimpleRecordMojo mojo = (GenerateSimpleRecordMojo) findConfiguredMojo("generateSimpleRecord", pom);
        mojo.project = project;
        mojo.execute();

        File generated = file("io/micronaut/test/MyRecord.java");
        Assertions.assertTrue(generated.exists());
        Assertions.assertEquals(content(generated), """
            package io.micronaut.test;

            /**
             * Version: 1
             * A simple record
             */
            public record MyRecord(
                java.lang.Integer age,
                java.lang.String title
            ) {
            }
            """);

        Assertions.assertEquals(List.of(baseDir.getAbsolutePath()), project.getCompileSourceRoots());
    }

    @Test
    void generateSimpleResource() throws Exception {
        File pom = new File("src/test/resources/test-resource-pom.xml");

        GenerateSimpleResourceMojo mojo = (GenerateSimpleResourceMojo) findConfiguredMojo("generateSimpleResource", pom);
        mojo.project = project;
        mojo.execute();

        File generated = file("META-INF/hello.txt");
        Assertions.assertTrue(generated.exists());
        Assertions.assertEquals("Hello!", content(generated));

        Assertions.assertEquals(1, project.getResources().size());
        Assertions.assertEquals(baseDir.getAbsolutePath(), project.getResources().get(0).getTargetPath());
    }

    @Test
    void generateSimpleResourceWithRepeat() throws Exception {
        File pom = new File("src/test/resources/test-resource-repeat-pom.xml");

        GenerateSimpleResourceMojo mojo = (GenerateSimpleResourceMojo) findConfiguredMojo("generateSimpleResource", pom);
        mojo.project = project;
        mojo.execute();

        File generated1 = file("META-INF/hello.txt_1");
        Assertions.assertTrue(generated1.exists());
        Assertions.assertEquals("Hello!\n", content(generated1));

        File generated2 = file("META-INF/hello.txt_2");
        Assertions.assertTrue(generated2.exists());
        Assertions.assertEquals("Hello!\n", content(generated2));

        Assertions.assertEquals(1, project.getResources().size());
        Assertions.assertEquals(baseDir.getAbsolutePath(), project.getResources().get(0).getTargetPath());
    }

}
