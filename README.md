# HMPPS Prisoner Contact Registry API

[![CircleCI](https://circleci.com/gh/ministryofjustice/prisoner-contact-registry/tree/main.svg?style=shield)](https://app.circleci.com/pipelines/github/ministryofjustice/prisoner-contact-registry)

This is a Spring Boot application, written in Kotlin, providing prisoner contact information. Used by [Visits UI](https://github.com/ministryofjustice/book-a-prison-visit-staff-ui).

Initially a facade over the NOMIS **Prison API** enabling access to data held in **NOMIS**

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

Testing coverage report

Run:
```
./gradlew koverHtmlReport
```
Then view output file for coverage report.


## Running

This service connects to a development environment for downstream APIs. 

Create a Spring Boot run configuration with active profile of 'dev', to run against te development environment.

Ports

| Service                   | Port |  
|---------------------------|------|
| prisoner-contact-registry | 8082 |

Alternatively the service can be run using docker-compose which will allow you to connect to a local version of prison-api.
edit the application-dev.yml file by changing the `prison.api.url` to `http://localhost:8091`. Then run:
```
docker-compose up
```

Ports

| Service                   | Port |  
|---------------------------|------|
| prisoner-contact-registry | 8082 |
| prison-api                | 8091 |


To create a Token via curl (local):
```
curl --location --request POST "https://sign-in-dev.hmpps.service.justice.gov.uk/auth/oauth/token?grant_type=client_credentials" --header "Authorization: Basic $(echo -n {Client}:{ClientSecret} | base64)"
```

or via postman collection using the following authorisation urls:
```
Grant type: Client Credentials
Access Token URL: https://sign-in-dev.hmpps.service.justice.gov.uk/auth/oauth/token
Client ID: <get from kubernetes secrets for dev namespace>
Client Secret: <get from kubernetes secrets for dev namespace>
Client Authentication: "Send as Basic Auth Header"
```

Call info endpoint:
```
$ curl 'http://localhost:8082/info' -i -X GET
```

## Swagger v3
Prisoner Contact Registry
```
http://localhost:8082/swagger-ui/index.html
```

Export Spec
```
http://localhost:8082/v3/api-docs?group=full-api
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

#### Build checks

To run the CircleCI trivy scan locally download and install trivy, build the prisoner-contact-registry docker image and run:
```
trivy image prisoner-contact-registry_prisoner-contact-registry
```
