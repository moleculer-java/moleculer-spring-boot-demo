# CLAUDE.md

This file provides guidance to Claude Code (claude.ai/code) when working with code in this repository.

## What this is

A demo web application showing how to run the [Moleculer-Java](https://moleculer-java.github.io/site/)
microservices framework inside Spring Boot. The same code runs in two interchangeable modes:

- **Standalone** — Netty HTTP server, launched via `MoleculerRunner` (high-performance, used by the Windows Installer build).
- **Servlet / WAR** — deployed to a Jakarta EE 10 container (Tomcat 10+, Jetty 12, WildFly, WebLogic 14.1.2+, etc.) via `MoleculerServlet`, configured in `src/main/webapp/WEB-INF/web.xml` (`jakartaee` 6.0 namespace).

There are no automated tests (`src/test` is empty; a JUnit 5 dependency is declared but unused). Verification is manual: run the app and hit the example URLs.

## Build & run

This is a **Maven** project (one `pom.xml`) targeting **Java 17** with `javac` (`<maven.compiler.release>17</maven.compiler.release>`). Build JDK 17+ (JDK 25 in use); minimum runtime: **JDK 17** (Spring Boot 3.5 requires Java 17+). It runs on **Spring Boot 3.5** (jakarta) and the `2.0.0` Moleculer/datatree stack. Version `2.0.0` (Maven coordinate `2.0.0-SNAPSHOT` while the workspace is in active development); **not** published to Maven Central (it's a demo).

```bash
mvn clean verify              # compile + build WAR -> target/moleculer-demo.war (no tests)
mvn clean package             # same; WAR for Jakarta EE servers
mvn -Pinstaller package       # ALSO build the Windows Installer (Windows-only, opt-in):
                              #   maven-dependency-plugin -> target/lib (runtime JARs)
                              #   maven-jar-plugin        -> target/lib/moleculer-demo.jar (app)
                              #   jlink                   -> target/jre (minimal JDK 21+ runtime)
                              #   ISCC.exe                -> installer/dist/moleculer_setup_2.0.0.exe
```

The core `mvn verify` never activates the `installer` profile, so it does not require `jlink` output or the bundled `installer/setup/ISCC.exe`.

**Run from source (standalone / development mode):**

- Main class: `services.moleculer.config.MoleculerRunner`
- Program arguments: `my.application.MoleculerApplication`
- VM options: `-Dlogging.config="classpath:logging-development.properties" -Djava.net.preferIPv4Stack=true -Dspring.profiles.active=development`

A ready-made VS Code launch configuration lives in `.vscode/launch.json`. The app serves examples at `http://localhost:3000/` and opens an interactive REPL on stdin (type `help`).

## Architecture

**Everything is wired in one place: `my.application.MoleculerApplication.getServiceBroker()`.** This single `@Bean` builds the `ServiceBroker`, picks the JSON adapter (Jackson), creates the `ApiGateway`, and defines all HTTP routes, middlewares, REST aliases, the template engine, and the JMX watchers. To change routing, middleware order, transporters, or server-side behavior, edit this method — start here before reading individual services.

**Services auto-register from the `my.services` package.** `@ComponentScan("my.services")` + the `SpringRegistrator` bean turn every Spring `@Controller` that extends `services.moleculer.service.Service` into a distributed Moleculer service at boot. A service's Moleculer name defaults to the decapitalized class name (`TableService` → `tableService`) unless overridden with `@Name(...)`.

**Service anatomy** (see `Greeter.java`, `ChatService.java`):
- **Actions** are *fields* of type `Action`, assigned a lambda `ctx -> {...}`, optionally named with `@Name`. Input is `ctx.params`; return a `Tree`, String, or `null` (→ HTTP 200).
- **Event listeners** are fields of type `Listener` annotated `@Subscribe("eventName")`.
- Lifecycle hooks: override `started(broker)` / `stopped()` (e.g. `ScheduledService` schedules a task via `broker.getConfig().getScheduler()`).

**`Tree` (datatree) is the universal data type** for both request params and responses — a JSON-like hierarchical structure used everywhere instead of POJOs/DTOs. See https://berkesa.github.io/datatree/.

**Two ways to expose an action over HTTP:**
1. Centrally in `getServiceBroker()` via `restRoute.addAlias("GET", "api/path/:param", "service.action")`.
2. On the action field via `@HttpAlias(method = "...", path = "...")` (see `ChatService`).

**HTML rendering convention:** an action returns a `Tree` model and sets `model.getMeta().put("$template", "name")`. The `ApiGateway` then renders it through the configured template engine (`DataTreeEngine`) using templates under `src/main/resources/www/`. Localized strings come from `src/main/resources/languages/messages-*.yml`.

**WebSocket push:** broadcast the framework event `websocket.send` with a `Tree` packet `{ "path": "ws/...", "data": {...} }`. Browsers subscribe to the logical `path`. Used by `ChatService` and `ScheduledService`.

**REPL commands** live in `my.commands`, extend `services.moleculer.repl.Command`, and are registered on the `LocalRepl`/`RemoteRepl` beans — which only exist under the `development` profile.

## Profiles & configuration

- **`development`** vs **`production`** Spring profiles drive behavior: development enables the REPL consoles, template/static-file reloading, request logging, and gateway debug; production minifies HTML templates and disables the consoles.
- `src/main/resources/application.yml` holds runtime config (`netty.port: 3000`, `repl.port`). Logging is selected by the `-Dlogging.config` system property pointing at `logging-development.properties` or `logging-production.properties`.
- The WAR runtime overrides settings in `web.xml`: it forces the `production` profile, sets `netty.enabled=false` (the container provides the HTTP server instead of Netty), and delegates logging to the container.

## Gotchas

- **Edit resources under `src/main/resources/`, never `target/classes/`.** Maven copies `src/main/resources/` (`application.yml`, `languages/`, `www/`, etc.) into `target/classes` on build; edits there are overwritten and won't take effect.
- The WAR build keeps `application.yml` but **excludes** `logging-development.properties` / `logging-production.properties` (see `maven-war-plugin` `<packagingExcludes>`); the container supplies logging. The standalone installer ships all three under `{app}\cfg` instead.
- Transporters (Redis, Kafka, NATS, AMQP, TCP) are not bundled. To connect this node to other Java/Node.js Moleculer nodes, add the transporter dependency in `pom.xml` and uncomment/set `cfg.setTransporter(...)` in `getServiceBroker()`.
