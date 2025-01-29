plugins {
    id("java-gradle-plugin")
    id("io.micronaut.build.internal.build-plugin-sourcegen-testsuite")
}

repositories {
    mavenCentral()
}

dependencies {
    api(projects.testSuiteCommonJava)
    annotationProcessor(projects.testSuiteCommonJava)
    annotationProcessor(mn.micronaut.inject)
    annotationProcessor(mn.micronaut.inject.java)
    annotationProcessor(projects.micronautBuildPluginSourcegenGenerator)
    annotationProcessor(mnSourcegen.micronaut.sourcegen.generator.java)
    implementation(projects.micronautBuildPluginSourcegenAnnotations)

    testImplementation(mnTest.micronaut.test.junit5)
    testImplementation(mnTest.junit.jupiter.engine)
}

tasks.withType<Test> {
    testLogging {
        showStandardStreams = true
    }
}
