plugins {
    id("io.micronaut.build.internal.build-plugin-sourcegen-testsuite")
}

dependencies {
    annotationProcessor(mn.micronaut.inject)
    annotationProcessor(mn.micronaut.inject.java)
    annotationProcessor(projects.micronautBuildPluginSourcegenGenerator)

    implementation(projects.micronautBuildPluginSourcegenAnnotations)
}
