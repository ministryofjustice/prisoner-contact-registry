# HMPPS Prisoner  Contact  Registry API

[![CircleCI](https://circleci.com/gh/ministryofjustice/prisoner-contact-registry/tree/main.svg?style=shield)](https://app.circleci.com/pipelines/github/ministryofjustice/prisoner-contact-registry)

This is a Spring Boot application, written in Kotlin, providing prisoner contact information. Used by [Visit Someone in Prison](https://github.com/ministryofjustice/book-a-prison-visit-staff-ui).

Initially a facade over the NOMIS **Prison API** enabling access to data held in **NOMIS**

Draft API Specification  [![API docs](https://img.shields.io/badge/API_docs-view-85EA2D.svg?logo=swagger)](https://editor.swagger.io/?url=https://raw.githubusercontent.com/ministryofjustice/prisoner-contact-registry/main/prisoner-contact-registry-api-specification.yaml)

<!--- Draft Event Specification [![Event docs](https://img.shields.io/badge/Event_docs-view-85EA2D.svg)](https://playground.asyncapi.io/?url=https://raw.githubusercontent.com/ministryofjustice/prisoner-contact-registry/main/prisoner-contact-registry-event-specification.yaml) -->

## Building

To build the project (without tests):
```
./gradlew clean build -x test
```

## Testing

Run:
```
./gradlew test 
```

## Running

Create a Spring Boot run configuration with active profile of dev, to run against te development environment.

Alternatively the service can be run using docker-compose.
```
docker-compose up
```

To create a Token (local):
```
curl --location --request POST "http://localhost:8081/auth/oauth/token?grant_type=client_credentials" --header "Authorization: Basic $(echo -n {Client}:{ClientSecret} | base64)"
```

Call info endpoint:
```
$ curl 'http://localhost:8080/info' -i -X GET
```

## Swagger v3
Prisoner Contact Registry
```
http://localhost:8080/swagger-ui/index.html
```

Export Spec
```
http://localhost:8080/v3/api-docs?group=full-api
```

## App Insights
Future addition ...

## Common gradle tasks

To list project dependencies, run:

```
./gradlew dependencies
```

To check for dependency updates, run:
```
./gradlew dependencyUpdates --warning-mode all
```

To run an OWASP dependency check, run:
```
./gradlew clean dependencyCheckAnalyze --info
```

To upgrade the gradle wrapper version, run:
```
./gradlew wrapper --gradle-version=<VERSION>
```

To automatically update project dependencies, run:
```
./gradlew useLatestVersions
```

#### Ktlint Gradle Tasks

To run Ktlint check:
```
./gradlew ktlintCheck
```

To run Ktlint format:
```
./gradlew ktlintFormat
```

To apply ktlint styles to intellij
```
./gradlew ktlintApplyToIdea
```

To register pre-commit check to run Ktlint format:
```
./gradlew ktlintApplyToIdea addKtlintFormatGitPreCommitHook 
```

...or to register pre-commit check to only run Ktlint check:
```
./gradlew ktlintApplyToIdea addKtlintCheckGitPreCommitHook
```

