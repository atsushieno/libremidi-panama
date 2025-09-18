
plugins {
    id ("application")
}

java {
    toolchain {
        languageVersion = JavaLanguageVersion.of(libs.versions.java.get().toInt())
    }
}
application {
    mainClass = "Driver"
}

dependencies {
    implementation(project(":libremidi-panama"))
    implementation(libs.jne)
}
repositories {
    mavenCentral()
}

