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

    compileOnly("org.apache.maven.plugin-tools:maven-plugin-annotations:3.9.0")
    implementation("org.apache.maven:maven-plugin-api:3.9.4")
    implementation("org.apache.maven:maven-core:3.9.4")
    testImplementation("org.apache.maven:maven-core:3.9.4")
    testImplementation("org.apache.maven.plugin-testing:maven-plugin-testing-harness:3.3.0")

    testImplementation(mnTest.micronaut.test.junit5)
    testImplementation(mnTest.junit.jupiter.engine)
}

tasks.withType<Test> {
    testLogging {
        showStandardStreams = true
    }
}
