plugins {
    id("io.micronaut.build.internal.build-plugin-sourcegen-module")
}

micronautBuild {
    descriptor {
        parentModuleId = "io.micronaut.build.plugin.sourcegen:micronaut-build-plugin-sourcegen-annotations"
    }
}
