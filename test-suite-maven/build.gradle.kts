plugins {
    id("maven-publish")
    id("io.micronaut.build.internal.build-plugin-sourcegen-testsuite")
}

repositories {
    mavenCentral()
}

dependencies {
    api(projects.testSuiteCommon)
    annotationProcessor(projects.testSuiteCommon)
    annotationProcessor(mn.micronaut.inject)
    annotationProcessor(mn.micronaut.inject.java)
    annotationProcessor(projects.micronautBuildPluginSourcegenGenerator)
    annotationProcessor(mnSourcegen.micronaut.sourcegen.generator.java)
    implementation(projects.micronautBuildPluginSourcegenAnnotations)

    compileOnly(libs.maven.plugin.annotations)
    implementation(libs.maven.plugin.api)
    implementation(libs.maven.core)
    testImplementation(libs.maven.plugin.testing.harness)

    testImplementation(mnTest.micronaut.test.junit5)
    testImplementation(mnTest.junit.jupiter.engine)
}

tasks.withType<Test> {
    testLogging {
        showStandardStreams = true
    }
}
