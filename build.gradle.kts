plugins {
  id("uk.gov.justice.hmpps.gradle-spring-boot") version "3.3.13"
  kotlin("plugin.spring") version "1.5.31"
}

configurations {
  testImplementation { exclude(group = "org.junit.vintage") }
}

dependencies {
  agentDeps("com.microsoft.azure:applicationinsights-agent:3.2.0-BETA.4")
  implementation("org.springframework.boot:spring-boot-starter-webflux")
  annotationProcessor("org.springframework.boot:spring-boot-configuration-processor")

  implementation("org.springdoc:springdoc-openapi-webmvc-core:1.5.10")
  implementation("org.springdoc:springdoc-openapi-ui:1.5.10")
  implementation("org.springdoc:springdoc-openapi-kotlin:1.5.10")
  implementation("org.springdoc:springdoc-openapi-data-rest:1.5.10")
}

java {
  toolchain.languageVersion.set(JavaLanguageVersion.of(16))
}

tasks {
  withType<org.jetbrains.kotlin.gradle.tasks.KotlinCompile> {
    kotlinOptions {
      jvmTarget = "16"
    }
  }
}
