import org.jetbrains.kotlin.gradle.tasks.KotlinCompile

val mainClassFritakAgp = "no.nav.helse.fritakagp.AppKt"
val githubPassword: String by project

plugins {
    application
    kotlin("jvm")
    id("org.jlleitschuh.gradle.ktlint")
    id("com.github.ben-manes.versions")
    id("com.autonomousapps.dependency-analysis")
    jacoco
    id("org.sonarqube")
}

application {
    mainClass.set(mainClassFritakAgp)
}

buildscript {
    dependencies {
        classpath("org.junit.platform:junit-platform-gradle-plugin:1.2.0")
    }
}

tasks.named<KotlinCompile>("compileKotlin") {
    kotlinOptions.jvmTarget = "11"
}

tasks.named<KotlinCompile>("compileTestKotlin") {
    kotlinOptions.jvmTarget = "11"
}

java {
    sourceCompatibility = JavaVersion.VERSION_11
    targetCompatibility = JavaVersion.VERSION_11
}

repositories {
    mavenCentral()
    google()
    maven(url = "https://packages.confluent.io/maven/")
    maven(url = "https://jitpack.io") {
        content {
            excludeGroup("no.nav.helsearbeidsgiver")
        }
    }
    maven {
        credentials {
            username = "x-access-token"
            password = githubPassword
        }
        setUrl("https://maven.pkg.github.com/navikt/helse-arbeidsgiver-felles-backend")
    }
}

tasks.named<Jar>("jar") {
    archiveBaseName.set("app")
    manifest {
        attributes["Main-Class"] = mainClassFritakAgp
        attributes["Class-Path"] = configurations.runtimeClasspath.get().joinToString(separator = " ") {
            it.name
        }
    }
    doLast {
        configurations.runtimeClasspath.get().forEach {
            val file = File("$buildDir/libs/${it.name}")
            if (!file.exists())
                it.copyTo(file)
        }
    }
}

tasks.withType<Test> {
    useJUnitPlatform()
    testLogging {
        events("passed", "skipped", "failed")
        showStackTraces = true
        exceptionFormat = org.gradle.api.tasks.testing.logging.TestExceptionFormat.FULL
    }
}

tasks.named<Test>("test") {
    include("no/nav/helse/**")
    exclude("no/nav/helse/slowtests/**")
}

task<Test>("slowTests") {
    include("no/nav/helse/slowtests/**")
    outputs.upToDateWhen { false }
    group = "verification"
}

tasks.jacocoTestReport {
    dependsOn(tasks.test)
    reports {
        xml.required.set(true)
        csv.required.set(false)
        html.outputLocation.set(layout.buildDirectory.dir("jacocoHtml"))
    }
}

tasks.test {
    finalizedBy(tasks.jacocoTestReport)
}

tasks.withType<Wrapper> {
    gradleVersion = "7.3"
}

sonarqube {
    properties {
        property("sonar.projectKey", "navikt_fritakagp")
        property("sonar.organization", "navikt")
        property("sonar.host.url", "https://sonarcloud.io")
        property("sonar.sourceEncoding", "UTF-8")
    }
}

dependencies {
    val assertJVersion: String by project
    val brukernotifikasjonSchemasVersion: String by project
    val confluentVersion: String by project
    val cxfVersion: String by project
    val gcpStorageVersion: String by project
    val hikariVersion: String by project
    val jacksonVersion: String by project
    val jaxwsToolsVersion: String by project
    val jaxwsVersion: String by project
    val junitJupiterVersion: String by project
    val kafkaClient: String by project
    val koinVersion: String by project
    val ktorVersion: String by project
    val logback_contrib_version: String by project
    val logback_version: String by project
    val mockKVersion: String by project
    val mockOAuth2ServerVersion: String by project
    val prometheusVersion: String by project
    val tokenSupportVersion: String by project
    val valiktorVersion: String by project

    implementation("ch.qos.logback.contrib:logback-jackson:$logback_contrib_version")
    implementation("ch.qos.logback.contrib:logback-json-classic:$logback_contrib_version")
    implementation("ch.qos.logback:logback-classic:$logback_version")
    implementation("com.fasterxml.jackson.datatype:jackson-datatype-jdk8:$jacksonVersion")
    implementation("com.fasterxml.jackson.datatype:jackson-datatype-jsr310:$jacksonVersion")
    implementation("com.fasterxml.jackson.module:jackson-module-kotlin:2.11+")
    implementation("com.github.javafaker:javafaker:1.0.2") // flytt denne til test når generatorene ikke er nødvendige i prod-koden lenger
    implementation("com.github.navikt:brukernotifikasjon-schemas:$brukernotifikasjonSchemasVersion")
    implementation("com.github.tomakehurst:wiremock-standalone:2.25.1")
    implementation("com.google.cloud:google-cloud-storage:$gcpStorageVersion")
    implementation("com.sun.activation:javax.activation:1.2.0")
    implementation("com.sun.activation:javax.activation:1.2.0")
    implementation("com.sun.xml.ws:jaxws-tools:$jaxwsToolsVersion") {
        exclude(group = "com.sun.xml.ws", module = "policy")
    }
    implementation("com.zaxxer:HikariCP:$hikariVersion")
    implementation("de.m3y.kformat:kformat:0.7")
    implementation("io.confluent:kafka-avro-serializer:$confluentVersion")
    implementation("io.insert-koin:koin-core-jvm:$koinVersion")
    implementation("io.insert-koin:koin-core:$koinVersion")
    implementation("io.insert-koin:koin-ktor:$koinVersion")
    implementation("io.ktor:ktor-auth:$ktorVersion")
    implementation("io.ktor:ktor-client-apache:$ktorVersion")
    implementation("io.ktor:ktor-client-core-jvm:$ktorVersion")
    implementation("io.ktor:ktor-client-core:$ktorVersion")
    implementation("io.ktor:ktor-client-jackson:$ktorVersion")
    implementation("io.ktor:ktor-client-json:$ktorVersion")
    implementation("io.ktor:ktor-jackson:$ktorVersion")
    implementation("io.ktor:ktor-locations:$ktorVersion")
    implementation("io.ktor:ktor-server-netty:$ktorVersion")
    implementation("io.prometheus:simpleclient_common:$prometheusVersion")
    implementation("io.prometheus:simpleclient_hotspot:$prometheusVersion")
    implementation("javax.ws.rs:javax.ws.rs-api:2.1.1")
    implementation("javax.xml.ws:jaxws-api:$jaxwsVersion")
    implementation("net.logstash.logback:logstash-logback-encoder:6.4")
    implementation("no.nav.common:log:2.2020.10.27_15.17-901cc4cfbbe4")
    implementation("no.nav.helsearbeidsgiver:helse-arbeidsgiver-felles-backend:2022.01.18-08-47-f6aa0")
    implementation("no.nav.security:mock-oauth2-server:$mockOAuth2ServerVersion")
    implementation("no.nav.security:token-client-core:$tokenSupportVersion")
    implementation("no.nav.security:token-validation-ktor:$tokenSupportVersion")
    implementation("no.nav.tjenestespesifikasjoner:altinn-correspondence-agency-external-basic:1.2019.09.25-00.21-49b69f0625e0")
    implementation("org.apache.cxf:cxf-rt-features-logging:$cxfVersion")
    implementation("org.apache.cxf:cxf-rt-frontend-jaxws:$cxfVersion")
    implementation("org.apache.cxf:cxf-rt-transports-http:$cxfVersion")
    implementation("org.apache.cxf:cxf-rt-ws-security:$cxfVersion")
    implementation("org.apache.kafka:kafka-clients:$kafkaClient")
    implementation("org.apache.pdfbox:pdfbox:2.0.24")
    implementation("org.codehaus.janino:janino:3.0.6")
    implementation("org.flywaydb:flyway-core:7.3.0")
    implementation("org.postgresql:postgresql:42.2.16")
    implementation("org.slf4j:slf4j-api:1.7.30")
    implementation("org.valiktor:valiktor-core:$valiktorVersion")
    implementation("org.valiktor:valiktor-javatime:$valiktorVersion")

    testImplementation("io.insert-koin:koin-test:$koinVersion")
    testImplementation("io.ktor:ktor-server-test-host:$ktorVersion")
    testImplementation("io.ktor:ktor-server-tests:$ktorVersion")
    testImplementation("io.mockk:mockk:$mockKVersion")
    testImplementation("org.assertj:assertj-core:$assertJVersion")
    testImplementation("org.jetbrains.kotlinx:kotlinx-coroutines-test:1.3.2")
    testImplementation("org.junit.jupiter:junit-jupiter-api:$junitJupiterVersion")
    testImplementation("org.junit.jupiter:junit-jupiter-params:$junitJupiterVersion")

    testRuntimeOnly("org.junit.jupiter:junit-jupiter-engine:$junitJupiterVersion")
}
