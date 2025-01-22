plugins {
    id("io.micronaut.build.internal.build-plugin-sourcegen-module")
}

dependencies {
    api(mnSourcegen.micronaut.sourcegen.model)
    implementation(mnSourcegen.micronaut.sourcegen.generator)
    api(mn.micronaut.core.processor)
    implementation(projects.micronautBuildPluginSourcegenAnnotations)

    testImplementation(mnSourcegen.micronaut.sourcegen.annotations)
    testImplementation(mn.micronaut.inject.java.test)
    testImplementation(mnSourcegen.micronaut.sourcegen.generator.java)

    testImplementation(libs.gradle.plugins.api) {
        exclude( "org.codehaus.groovy", "groovy")
        exclude( "org.codehaus.groovy", "groovy-all")
    }
    testImplementation(libs.maven.plugin.annotations)
    testImplementation(libs.maven.plugin.api)
}

