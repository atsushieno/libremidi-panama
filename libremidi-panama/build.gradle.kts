plugins {
    id ("java-library")
    //alias(libs.plugins.jextract)
    alias(libs.plugins.maven.publish)
    id ("maven-publish")
    id ("signing")
}

java {
    toolchain {
        languageVersion = JavaLanguageVersion.of(22)
    }
}

/*
// io.github.krakowski.jextract
  external/jextract/build/jextract/bin/jextract
    external/libremidi/include/libremidi/libremidi-c.h
    -I external/libremidi/include
    -t dev.atsushieno.panama.libremidi
    --output libremidi-panama/src/main/java

   built jextract on OSX with:
     ./gradlew build
        -Pllvm_home=/Library/Developer/CommandLineTools/usr
        -Pjdk_home=`/usr/libexec/java_home`
*/
/*
jextract {
    header("${project.projectDir}/../external/libremidi/include/libremidi/libremidi-c.h") {
        // The library name
        libraries = [ 'libremidi' ]
    
        // The package under which all source files will be generated
        targetPackage = 'dev.atsushieno.panama.libremidi'
        
        // The generated class name
        className = 'LibreMidi'
    }
}
*/

dependencies {
    implementation(libs.jne)
    testImplementation(platform(libs.junit.bom))
    testImplementation(libs.junit.jupiter)
}

tasks.jar {
    manifest {
        attributes(
            "Class-Path" to configurations.runtimeClasspath.get().joinToString(" ") { it.name },
            "Implementation-Title" to "libremidi Panama binding",
            "Implementation-Vendor" to "atsushieno",
            "Implementation-Version" to project.version,
            "Specification-Title" to "libremidi Panama binding",
            "Specification-Vendor" to "atsushieno",
            "Specification-Version" to project.version
        )
    }
}

tasks.javadoc {
    isFailOnError = false
    //from javadoc.destinationDir
}

val javadocJar = tasks.register<Jar>("javadocJar") {
    dependsOn(tasks.javadoc)
    archiveClassifier.set("javadoc")
    from(tasks.javadoc.get().destinationDir)
}

val sourcesJar = tasks.register<Jar>("sourcesJar") {
    dependsOn(tasks.classes)
    archiveClassifier.set("sources")
    from(sourceSets.main.get().allSource)
}

afterEvaluate {
    tasks.findByName("generateMetadataFileForMavenJavaPublication")?.dependsOn(javadocJar, sourcesJar)
}

artifacts {
    archives(javadocJar)
    archives(sourcesJar)
}

val gitProjectName = "libremidi-panama"
val packageName = project.name
val packageDescription = "libremidi Panama binding with prebuilt binaries"
// my common settings
val packageUrl = "https://github.com/atsushieno/$gitProjectName"
val licenseName = "MIT"
val licenseUrl = "https://github.com/atsushieno/$gitProjectName/blob/main/LICENSE"
val devId = "atsushieno"
val devName = "Atsushi Eno"
val devEmail = "atsushieno@gmail.com"

// Common copy-pasted
mavenPublishing {
    publishToMavenCentral(com.vanniktech.maven.publish.SonatypeHost.CENTRAL_PORTAL)
    if (project.hasProperty("mavenCentralUsername") || System.getenv("ORG_GRADLE_PROJECT_mavenCentralUsername") != null)
        signAllPublications()
    coordinates(group.toString(), project.name, version.toString())
    pom {
        name.set(packageName)
        description.set(packageDescription)
        url.set(packageUrl)
        scm { url.set(packageUrl) }
        licenses { license { name.set(licenseName); url.set(licenseUrl) } }
        developers { developer { id.set(devId); name.set(devName); email.set(devEmail) } }
    }
}
