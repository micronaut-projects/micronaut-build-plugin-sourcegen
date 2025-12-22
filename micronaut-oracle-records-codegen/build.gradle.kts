plugins {
    id("io.micronaut.build.internal.build-plugin-sourcegen-module")
}

dependencies {
    // Annotations used to define the plugin/task surface
    implementation(projects.micronautBuildPluginSourcegenAnnotations)

    // Annotation processors to generate Gradle/Maven plugins from the annotated classes
    annotationProcessor(projects.micronautBuildPluginSourcegenGenerator)
    annotationProcessor(mnSourcegen.micronaut.sourcegen.generator.java)

    // Micronaut inject processors required by sourcegen
    annotationProcessor(mn.micronaut.inject)
    annotationProcessor(mn.micronaut.inject.java)

    // For nullability annotations in source (NonNull/Nullable)
    compileOnly(mn.micronaut.core)
    // For generated Gradle and Maven plugin APIs (compileOnly on this module)
    compileOnly(libs.gradle.plugins.api)
    compileOnly(libs.maven.plugin.annotations)
    compileOnly(libs.maven.plugin.api)
    compileOnly(libs.maven.core)

    // Test deps (JUnit 5 via Micronaut test catalog)
    testImplementation(mnTest.micronaut.test.junit5)
    testImplementation(mnTest.junit.jupiter.engine)
}
