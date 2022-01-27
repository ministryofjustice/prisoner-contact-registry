plugins {
  id("uk.gov.justice.hmpps.gradle-spring-boot") version "4.0.1"
  kotlin("plugin.spring") version "1.6.10"
  idea
}

configurations {
  testImplementation { exclude(group = "org.junit.vintage") }
}

dependencies {

  // Spring
  annotationProcessor("org.springframework.boot:spring-boot-configuration-processor")
  implementation("org.springframework.boot:spring-boot-starter-webflux")
  implementation("org.springframework.boot:spring-boot-starter-security")
  implementation("org.springframework.boot:spring-boot-starter-oauth2-resource-server")
  implementation("org.springframework.boot:spring-boot-starter-oauth2-client")

  // Swagger
  implementation("org.springdoc:springdoc-openapi-webmvc-core:1.6.5")
  implementation("org.springdoc:springdoc-openapi-ui:1.6.5")
  implementation("org.springdoc:springdoc-openapi-kotlin:1.6.5")
  implementation("org.springdoc:springdoc-openapi-data-rest:1.6.5")

  // AWS

  // Insights
  agentDeps("com.microsoft.azure:applicationinsights-agent:3.2.4")

  // DB

  // HMPPS Libs

  // Test
  testImplementation("io.jsonwebtoken:jjwt:0.9.1")
  testImplementation("net.javacrumbs.json-unit:json-unit-assertj:2.28.0")
  testImplementation("org.springframework.security:spring-security-test")
  testImplementation("com.github.tomakehurst:wiremock-standalone:2.27.2")
}

java {
  toolchain.languageVersion.set(JavaLanguageVersion.of(17))
}

tasks {
  withType<org.jetbrains.kotlin.gradle.tasks.KotlinCompile> {
    kotlinOptions {
      jvmTarget = "17"
    }
  }
}
