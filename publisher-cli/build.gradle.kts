plugins {
    java
    application
    id("com.gradleup.shadow")
}

group = "org.example"
version = "1.0-SNAPSHOT"

repositories {
    mavenCentral()
}

application {
    mainClass.set("app.PublisherApp") // Match your actual Java main class package
}

dependencies {
    implementation(project(":repository-api"))
    implementation(project(":repository-core"))
    runtimeOnly(project(":storage-local"))

    testImplementation(platform("org.junit:junit-bom:6.0.0"))
    testImplementation("org.junit.jupiter:junit-jupiter")
    testRuntimeOnly("org.junit.platform:junit-platform-launcher")
}

tasks.test {
    useJUnitPlatform()
}

tasks.shadowJar {
    archiveFileName.set("publisher-cli.jar")
    mergeServiceFiles()
}