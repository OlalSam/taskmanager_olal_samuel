# Task Manager (Spring Boot) — README

> **Project:** Task Manager with bi-directional Google Calendar synchronization

This is a Spring Boot application that manages tasks, tags and synchronizes them **bidirectionally** with Google Calendar using asynchronous processing and application events. The application runs as a **resource server** and uses **Keycloak** for authentication/authorization. The database and Keycloak are provided in `docker-compose.yml` for easy local developmt


---

## Table of contents

* [Features](#features)
* [Tech stack](#tech-stack)
* [Prerequisites](#prerequisites)
* [Quick install (Docker Compose)](#quick-install-docker-compose)
* [Local development (without Docker)](#local-development-without-docker)
* [Configuration / Environment variables](#configuration--environment-variables)
* [Google Calendar setup](#google-calendar-setup)
* [Keycloak setup (outline)](#keycloak-setup-outline)
* [Build & run](#build--run)
* [Testing](#testing)

---

## Features

* Create, update, delete tasks (CRUD)
* Bi-directional sync with Google Calendar events (changes in app → Google Calendar and vice versa)
* Asynchronous processing for long-running sync operations and event handling
* OAuth2 / OpenID Connect resource server (Keycloak management)
* Docker Compose setup for local Postgres and Keycloak

---

## Tech stack

* Java + Spring Boot (Web, Data JPA, Security)
* Keycloak (authentication & authorization)
* Google Calendar API (OAuth2)
* PostgreSQL (database)
* Docker / Docker Compose for local services
* Maven wrapper (`mvnw`) to build and run

---

## Prerequisites

* Java 21+ (or the version specified in `pom.xml`)
* Maven (or use the included `./mvnw` wrapper)
* Docker & Docker Compose (for running Keycloak + Postgres locally)
* A Google Cloud Project with Calendar API enabled and OAuth 2.0 credentials
* Access to the Keycloak admin console (configured via `docker-compose.yml`)

---

## Quick install (Docker Compose)

This will start the database and Keycloak locally using `docker-compose`.

1. Clone the repo

```bash
git clone <repo-url>
cd <repo-directory>
```

2. Create a `.env` file (optional) to hold service credentials (example below in the Configuration section).

3. Start local services (Keycloak & Postgres)

```bash
docker-compose up -d
```

4. Use the Keycloak admin console to create a realm and client for this app.

5. Configure Google OAuth credentials (see “Google Calendar setup” below) and place the credentials file where the app expects it (or set the env var pointing to it).

6. Build and run the application (see [Build & run](#build--run)).

---

## Local development (without Docker)

If you prefer to run Keycloak and Postgres elsewhere (cloud or local installs), update the configuration environment variables accordingly.

* Ensure `spring.datasource.*` points to your Postgres instance
* Ensure Keycloak URIs are reachable from your app
* Ensure Google credential JSON or client secrets are available to the app

---

## Configuration / Environment variables

The app reads configuration from `application.yml` / `application.properties` and environment variables. Typical environment variables used include (examples — update to your exact property names):
```

> Put any sensitive values (client secrets, DB passwords, credential JSON) outside Git, e.g. in a local `.env` file excluded by `.gitignore`, or use a secrets manager.

---

## Google Calendar setup

1. Create a Google Cloud Project ([https://console.cloud.google.com](https://console.cloud.google.com)).
2. Enable the Google Calendar API for the project.
3. Create OAuth 2.0 credentials (OAuth client ID) for a web application and set authorized redirect URIs (example: `http://localhost:8080/oauth2/callback`).
4. Download the credentials JSON and set `GOOGLE_CREDENTIALS_FILE` to its path or paste the values into env vars used by the app.
5. Ensure the consent screen is configured (at least for testing, you can set to "External" and add test users).

 
---

## Keycloak setup (outline)

If you are using the provided `docker-compose.yml`, Keycloak will be available locally (default port 8080 or configured port in compose). Typical steps:

1. Open Keycloak admin console ([http://localhost:8080/](http://localhost:8080/) or port defined in compose)
2. Create a realm (e.g., `sare_africa`) or import the provided realm JSON if repository includes it.
3. Create a client (e.g., sare_africa_api`) and set `Access Type` to `confidential` or `public` depending on your flow. Configure valid redirect URIs matching the application.
4. Create roles (e.g., `USER`) and test users.
5. Update application configuration (`KEYCLOAK_ISSUER_URI`, `KEYCLOAK_CLIENT_ID`, `KEYCLOAK_CLIENT_SECRET`) accordingly.

---

## Build & run

### Using Maven wrapper:

```bash
# build
./mvnw clean package -DskipTests

# run (uses application.properties/yml + env var overrides)
./mvnw spring-boot:run
```

### Using packaged jar

```bash
./mvnw clean package -DskipTests
java -jar target/*.jar
```

> The app will connect to the configured Postgres and Keycloak instances. Ensure they are up and reachable before starting.

---

## Running tests

```bash
./mvnw test
```

> 

---

## Troubleshooting

* **Keycloak 401 / token validation errors:** Ensure `KEYCLOAK_ISSUER_URI` is correct and the realm public key / JWKS endpoint is reachable by the app. Check client configuration (redirect URIs) and that the client ID/secret match.
* **Google OAuth redirect / consent errors:** Ensure the OAuth consent screen is configured in Google Cloud and the redirect URIs match those registered in the Google Cloud Console.
* **Database connection refused:** Verify `spring.datasource.url`, that Postgres container is `up` (`docker ps`), and that ports match.
* **Sync issues / duplicates:** Check async worker logs and event handlers. Events should be idempotent where possible; consider de-duplication keys when persisting synced events.
* **Sensitive file accidentally committed:** See the Security section below.

---
 
