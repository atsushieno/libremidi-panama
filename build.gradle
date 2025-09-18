plugins {
    alias(libs.plugins.jextract) apply false
    alias(libs.plugins.maven.publish) apply false
}

allprojects {
    group = "dev.atsushieno"
    version = "0.2.7"

    repositories {
        mavenLocal()
        mavenCentral()
    }
}

apply { from("${rootDir}/publish-root.gradle") }
