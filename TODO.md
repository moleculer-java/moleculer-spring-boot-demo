# TODO — Modernize `moleculer-spring-boot-demo` to 2.0.0

> **You are the per-project Claude Code instance for `moleculer-spring-boot-demo`.** Self-contained file.
> Goal: Gradle/Java 8 → **Maven + JDK 21**, **Spring Boot 2.3 → 3.5** (jakarta), all six inter-project
> deps → **2.0.0**, drop **SIGAR**, modernize the **Windows installer**, legacy files removed, version
> **2.0.0**. This is a **demo web application** (a reference for how to run Moleculer-Java inside Spring
> Boot), **not a published library** — so **no Maven Central publishing** (no sources/javadoc/GPG/Central
> plugin). It packages as a **WAR** and also runs **standalone on Netty**. Packages: `my.application`,
> `my.services`, `my.commands`.
>
> ⚠ **This is a terminal consumer — it depends on almost the whole stack.** Build it **last**, after all
> nine code projects are installed as `2.0.0-SNAPSHOT` in the local `~/.m2`. It has no workspace
> dependents, so nothing ripples out of it.

## Coordinates & facts
- Maven: `com.github.berkesa:moleculer-spring-boot-demo` (group kept for consistency), `war`, license **MIT**.
- `name`: *Moleculer Java demo project for SpringBoot Framework* · `inceptionYear`: 2019
- Two interchangeable run modes from the **same code** (preserve this — it's the whole point of the demo):
  - **Standalone** — Netty HTTP server, launched via `services.moleculer.config.MoleculerRunner`
    (program arg `my.application.MoleculerApplication`). Used by the Windows Installer.
  - **Servlet / WAR** — `services.moleculer.web.servlet.MoleculerServlet` (from `moleculer-java-web`),
    wired in `src/main/webapp/WEB-INF/web.xml`, deployed to a Jakarta servlet container.
- **No automated tests** (`src/test` is empty; JUnit is declared but unused). Verification stays
  **manual**: run the app, hit the example URLs (`http://localhost:3000/`). `mvn verify` passes trivially
  (no tests to run).
- **Version → `2.0.0`** (was `1.0.0`, hard-coded in the Gradle `version` + `jar { baseName =
  'moleculer-demo' }` block + the installer). Bump it everywhere: `pom.xml`, the `.iss` `SetupVersion`,
  and the `README.md` download links / filenames (`moleculer_setup_1.0.0.exe` → `…_2.0.0.exe`).

## Inter-project dependencies (PIN to 2.0.0)
All were badly version-skewed in the old build — unify to **2.0.0**:
| Dependency | Old | New |
|---|---|---|
| `com.github.berkesa:datatree-core` | 1.0.15 | **2.0.0** |
| `com.github.berkesa:moleculer-java` | 1.2.15 | **2.0.0** |
| `com.github.berkesa:moleculer-java-web` | 1.2.12 | **2.0.0** |
| `com.github.berkesa:moleculer-java-repl` | 1.2.2 | **2.0.0** (optional console) |
| `com.github.berkesa:moleculer-java-jmx` | 1.2.1 | **2.0.0** |
| `com.github.berkesa:datatree-templates` | 1.1.4 | **2.0.0** |

> `datatree-core` is also pulled transitively by every moleculer artifact; declaring it explicitly is
> fine (keep it at 2.0.0). `moleculer-java-web` transitively brings Netty 4.2.15, the jakarta servlet
> path, and the template engines.

## Third-party dependency actions (confirm newest at execution; keep workspace lockstep)
| Dependency | Old | Action |
|---|---|---|
| `org.springframework.boot:spring-boot-starter` | 2.3.4.RELEASE | **3.5.3** ⚠ jakarta + Java 17+ (🔒 lockstep — locked by moleculer-java). Keep the two excludes (`spring-boot-starter-logging`, `spring-jcl`). ⚠ Boot 3 **rejects circular bean refs by default** — the demo's `@Bean` graph in `MoleculerApplication` is acyclic, but re-check after wiring. |
| `com.fasterxml.jackson.core:jackson-databind` | 2.10.1 | **2.19.0** via `jackson-bom` import (🔒 lockstep). The stale "2.10.x causes JBoss EAP warnings" comment no longer applies. **Simplest:** drop the explicit pin and let it come transitively at 2.19.0 from `moleculer-java`/`datatree-adapters`; or import `jackson-bom` 2.19.0 to be explicit. |
| `org.fusesource:sigar:1.6.4` (+ native DLLs) | 1.6.4 | **DROP entirely.** `moleculer-java` 2.0.0 removed `SigarMonitor`; the monitor now auto-selects **JMX → Constant**. Not used in `my.*` source. Remove the dependency, the native libs `installer/bin/sigar-amd64-winnt.dll` + `sigar-x86-winnt.dll`, and **every `-Djava.library.path=…` option** (see the installer section). |
| `com.diogonunes:JCDP:2.0.3.1` | 2.0.3.1 | **DROP** (not referenced in `my.*` source; the console color path lives in `moleculer-java-repl` 2.0.0, which already migrated to **JColor 5.5.1** and brings it transitively). If you find a direct use, replace with **`com.diogonunes:JColor:5.5.1`** (🔒 lockstep). |
| `org.slf4j:slf4j-api`, `slf4j-jdk14`, `log4j-over-slf4j`, `jcl-over-slf4j` | 1.7.30 | **2.0.18** (🔒 lockstep) |
| `junit:junit:4.12` | 4.12 | swap to `org.junit.jupiter:junit-jupiter:5.14.4` **test** scope (or drop — `src/test` is empty). Nothing to migrate. |
| Eclipse `ecj` 4.4.2 | — | **remove** → javac |
| Java | 1.8 | **21** |

## ⚠ The jakarta migration (small here — the heavy lifting is already inside `moleculer-java-web` 2.0.0)
The demo's own `my.*` source has **no jakarta-affected `javax` imports** — the only `javax.*` it uses is
**`javax.imageio.ImageIO`** (`FileUpload.java`, `ServerSideImage.java`), which is a **JDK** class and
**stays `javax`** (do **not** rewrite it). The servlet/websocket classes the demo references
(`MoleculerServlet`, `EndpointDeployer`) are already Jakarta in `moleculer-java-web` 2.0.0. So the only
jakarta change in this repo is the **deployment descriptor**:

- **`src/main/webapp/WEB-INF/web.xml`:** migrate the `javaee` namespace to `jakartaee` and bump the
  schema/version:
  - `xmlns="http://java.sun.com/xml/ns/javaee"` → `xmlns="https://jakarta.ee/xml/ns/jakartaee"`
  - `schemaLocation … web-app_3_0.xsd` → `… https://jakarta.ee/xml/ns/jakartaee/web-app_6_0.xsd`
  - `version="3.0"` → `version="6.0"`
  - The `<listener-class>` / `<servlet-class>` (`services.moleculer.web.servlet.*`) stay the same —
    they're just jakarta-backed now. Keep the `init-param`s, `<async-supported>`, `<absolute-ordering/>`.
- **Target containers become Jakarta EE 10**: Tomcat 10+, Jetty 12, WildFly 27+, GlassFish 7, Payara 6,
  WebLogic 14.1.2+, Open Liberty (jakarta). Update the README's "compatible servers" list accordingly
  (the old list named Tomcat 7–9 / Jetty 9 / WebLogic 12 / JBoss EAP 7 — all pre-jakarta).

## WAR / standalone build (Maven)
- **Packaging `war`** via `maven-war-plugin`. Reproduce the Gradle `war` exclusions: keep
  `application.yml` in the WAR but **exclude** `logging-development.properties` /
  `logging-production.properties` from the WAR (the container supplies logging; `web.xml` sets
  `-Dorg.springframework.boot.logging.LoggingSystem=none`). Use `<packagingExcludes>` or a
  `<webResources>` filter.
- The WAR forces the **`production`** profile and `netty.enabled=false` (container provides HTTP); the
  standalone path runs the **`development`/`production`** profile on Netty (`netty.port=3000`).
- Standalone run config (for IDE / `.vscode/launch.json`): main `services.moleculer.config.MoleculerRunner`,
  program args `my.application.MoleculerApplication`, VM options
  `-Dlogging.config="classpath:logging-development.properties" -Djava.net.preferIPv4Stack=true
  -Dspring.profiles.active=development` — **note `-Djava.library.path` is gone** (SIGAR removed).

## ⚠ Windows installer — FULL REWIRE (this is in scope per the orchestrator decision)
The old installer = Inno Setup (`installer/moleculer.config.iss` + `installer/setup/ISCC.exe`) +
committed **Java 8 JRE** (`installer/jre/`) + Apache Commons Daemon **procrun** (`tomcat7.exe` /
`tomcat7w.exe`) Windows-service wrapper + the SIGAR DLLs + Gradle tasks (`cleanupLibs`, `copyLibs`,
`buildInstaller`). Modernize it into a **working JDK 21 installer**:

1. **Replace the Gradle installer tasks with Maven:**
   - `copyLibs` (copied `compileClasspath` JARs into `build/libs`) → **`maven-dependency-plugin:copy-dependencies`**
     (assemble the runtime JARs into e.g. `target/lib`), plus the project WAR/JAR. `cleanupLibs` → the
     plugin's clean / `maven-clean-plugin`.
   - `buildInstaller` (Exec → `installer/setup/ISCC.exe installer/moleculer.config.iss`) →
     **`exec-maven-plugin`** (or `org.codehaus.mojo:exec-maven-plugin`) bound to a `installer` profile so
     `mvn -Pinstaller package` regenerates the setup `.exe`. Keep it **Windows-only / opt-in** (the core
     `mvn verify` must not require ISCC).
2. **`installer/moleculer.config.iss`:** update the `[Files]` source paths from `build\libs\*.jar` →
   the Maven output (`target\lib\*.jar` + the app artifact); bump `SetupVersion` `1.0.0` → **`2.0.0`**
   (output `moleculer_setup_2.0.0.exe`); **remove the SIGAR `-Djava.library.path={app}\bin` option** from
   the procrun `[Registry] … \Java … Options` multi-string.
3. **Bundle a JDK 21 runtime instead of the committed Java 8 JRE:** generate a minimal runtime with
   **`jlink`** (target the modules the app needs) into `installer/jre/` (or `target/jre/`) at build time,
   or drop in a full JDK 21. Remove the stale Java 8 `installer/jre/` from version control (regenerate it).
   Update the `.iss` `Source: installer\jre\*` accordingly.
4. **Update the Windows-service wrapper (procrun):** `tomcat7.exe`/`tomcat7w.exe` are old Apache Commons
   Daemon binaries → replace with **current Commons Daemon `prunsrv`/`prunmgr`** (Windows x64, JDK
   21-compatible). Update the `.iss` `[Registry]` procrun keys (`…\Procrun 2.0\MoleculerJava\…`), the
   `Jvm` path (`{app}\jre\bin\server\jvm.dll`), and the references in `installer/bin/service-install.bat`
   / `service-manager.bat` / `production-start.bat` / `production-stop.bat` to the new binary names.
   Keep the start class `services.moleculer.config.MoleculerRunner` and params `my.application.MoleculerApplication`.
5. **`installer/bin/development-start.bat`:** remove `"-Djava.library.path=%ROOT%\bin"` (SIGAR gone);
   keep the rest (`-Dspring.profiles.active=development`, logging config, classpath). The `-Xrunjdwp` debug
   agent flag is fine on JDK 21 (it's still `-agentlib:jdwp` compatible) but prefer the modern
   `-agentlib:jdwp=...` form.
6. **Stale build outputs:** `installer/dist/` holds a prebuilt `moleculer-demo.war` + `moleculer_setup_1.0.0.exe`
   (1.0.0) — regenerate these from the Maven build; add `installer/dist/` (and any jlink-generated
   `installer/jre/`) to `.gitignore`.

## Steps
1. **`pom.xml`** (metadata + MIT + `<maven.compiler.release>21</maven.compiler.release>`, `<packaging>war</packaging>`).
   Apply the inter-project + third-party tables; import `jackson-bom` 2.19.0 if pinning Jackson. Build
   plugins: compiler **3.15.0**, surefire **3.5.4**, **`maven-war-plugin`** (latest 3.4.x) with the
   logging-properties exclusions; the `installer` profile (dependency-plugin + exec-plugin). **No release/
   publishing profile** (demo is not published to Central).
2. **Remove ECJ → javac**; fix any javac-vs-ECJ edge cases.
3. **Bump all six inter-project deps → 2.0.0**; **Spring Boot → 3.5.3**; **drop SIGAR + JCDP**;
   **slf4j 2.0.18**; Jackson 2.19.0. Resolve compile errors from Spring Boot 3 / the 2.0.0 APIs.
4. **jakarta:** migrate `web.xml` to the `jakartaee` 6.0 namespace (above). Leave `javax.imageio` alone.
5. **Installer full rewire** (the section above).
6. **Tests:** none to migrate (`src/test` empty). Keep a `junit-jupiter` test dep for future, or drop it.
7. **Preserve demo behavior:** the single-point wiring in `MoleculerApplication.getServiceBroker()`
   (Jackson JSON, `ApiGateway`, all routes/aliases/middlewares, `DataTreeEngine` + `DefaultMessageLoader`
   i18n, the two `JmxService` `ObjectWatcher`s, dev-vs-prod profile switches); `@ComponentScan("my.services")`
   + `SpringRegistrator` auto-registering `@Controller extends Service`; `@HttpAlias`; the `websocket.send`
   push; REPL beans under the `development` profile; static content under `src/main/resources/www`.
8. **Cleanup — delete:** `build.gradle`, `settings.gradle`, `gradlew`, `gradlew.bat`, `gradle/`,
   `.gradle/`, `.classpath`, `.project`, `.settings/`, **`.idea/`** (IntelliJ), **`moleculer-demo.launch`**
   (Eclipse), and the stale `bin/` + `build/` + `log/` dirs. **Keep:** `src/`, `installer/` (rewired),
   `docs/` (README screenshots), `LICENSE`, `README.md`, `CLAUDE.md`.
9. **VSCode + `.gitignore`.** Add `.vscode/launch.json` for `services.moleculer.config.MoleculerRunner`
   (program arg `my.application.MoleculerApplication`, the dev VM options above — **no** `java.library.path`).
   `.gitignore`: `target/`, `.gradle/`, `bin/`, `build/`, `log/`, `.idea/`, `.classpath`, `.project`,
   `.settings/`, `installer/dist/`, generated `installer/jre/`.
10. **README refresh:** Gradle → Maven commands (`gradle war` → `mvn -Pinstaller package` / `mvn package`;
    `gradle buildInstaller` → the Maven installer profile), Java 8 → **21**, version `1.0.0` → **2.0.0**
    (download links + `moleculer_setup_2.0.0.exe`), the **Jakarta EE 10** server-compatibility list, remove
    every SIGAR / `-Djava.library.path` mention, and the IDE run-config VM options. The `docs/*.png`
    screenshots can stay (note in prose if they show the old Gradle flow).
11. **Update `CLAUDE.md`:** Maven build/run; Spring Boot 3 (jakarta); `web.xml` jakartaee 6.0; SIGAR
    removed (monitor → JMX fallback); the rewired installer (jlink JDK 21 + modern procrun); version 2.0.0.
12. **Build & verify:** `mvn clean verify` (compiles WAR on JDK 21, no tests). Then optionally
    `mvn -Pinstaller package` on Windows to regenerate the setup `.exe`, and a **manual smoke run**
    (standalone Netty on :3000, hit the demo URLs).

## Definition of done
- `mvn clean verify` **green on JDK 21**; WAR builds; Spring Boot **3.5.3** + `web.xml` **jakartaee 6.0**
  resolved; SIGAR dropped (no native libs / `java.library.path`); JCDP dropped.
- All six inter-project deps on **2.0.0**; slf4j 2.0.18; Jackson 2.19.0; ECJ dropped (javac); Java 21.
- **Working modernized Windows installer**: Maven-driven build (`maven-dependency-plugin` + `exec-maven-plugin`/ISCC),
  **jlink JDK 21 runtime**, **current Commons Daemon procrun**, regenerated `moleculer_setup_2.0.0.exe`.
- No Gradle/ECJ/Eclipse/IntelliJ files remain; `.vscode/` (+ `launch.json`) + `.gitignore` added.
- Version `2.0.0`; **not** published to Central (demo app); manual smoke run of both WAR + standalone modes.
