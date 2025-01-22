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

    testImplementation("dev.gradleplugins:gradle-api:8.11.1") {
        exclude( "org.codehaus.groovy", "groovy")
        exclude( "org.codehaus.groovy", "groovy-all")
    }
    testImplementation("org.apache.maven.plugin-tools:maven-plugin-annotations:3.9.0")
    testImplementation("org.apache.maven:maven-plugin-api:3.9.4")
}

