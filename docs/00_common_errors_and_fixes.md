# 🛠️ Common Errors & Fixes — URL Shortener Backend

> A practical debugging guide documenting real issues encountered during project setup, with root-cause explanations and step-by-step fixes.

---

## 📋 Table of Contents

- [Issue 1 — Maven Project Not Recognized](#-issue-1--maven-project-not-recognized)
- [Issue 2 — Database Auto-Configuration Failure](#-issue-2--database-auto-configuration-failure)
- [Issue 3 — Port Already in Use](#-issue-3--port-already-in-use)
- [Issue 4 — Whitelabel Error Page (404)](#-issue-4--whitelabel-error-page-404)
- [Issue 5 — Repository Bean Not Found](#-issue-5--repository-bean-not-found)
- [Issue 6 — Java File Outside Source Root](#-issue-6--java-file-outside-source-root)
- [Issue 7 — Invalid Maven Dependencies](#-issue-7--invalid-maven-dependencies)
- [Issue 8 — Maven Command Not Found](#-issue-8--maven-command-not-found)
- [Issue 9 — Lombok Annotation Processor Error](#-issue-9--lombok-annotation-processor-error)
- [Issue 10 — Redis Command Not Found](#-issue-10--redis-command-not-found)
- [Issue 11 — Spring Boot Version Compatibility](#-issue-11--spring-boot-version-compatibility)
- [Issue 12 — Redis Not Being Used](#-issue-12--redis-not-being-used-logical-check)
- [Final Takeaways](#-final-takeaways)

---

## ❌ Issue 1 — Maven Project Not Recognized

### 🔍 Symptom

- No **Run ▶️** button visible in IntelliJ
- Dependencies not loading
- Project behaving like a plain folder instead of a Spring Boot app

### 🧐 Root Cause

IntelliJ opened the folder but never imported it as a **Maven project**. Without this, IntelliJ has no awareness of `pom.xml`, so it cannot resolve dependencies, compile sources, or expose the run configuration.

### ✅ Fix

Right-click on `pom.xml` inside IntelliJ's Project Explorer:

```
backend/pom.xml → Right-click → Add as Maven Project
```

IntelliJ will trigger a Maven sync, download all declared dependencies, and enable the Run button.

### 💡 How Maven Works

Maven is a **build and dependency management tool**. It reads `pom.xml` to:

| Responsibility | What It Does |
|---|---|
| Dependency Management | Downloads JARs from Maven Central |
| Build Lifecycle | Compiles, tests, and packages the project |
| Plugin Execution | Runs Spring Boot, code generation, etc. |

Without importing `pom.xml`, IntelliJ treats your project as a set of raw `.java` files — no classpath, no framework support.

> **📌 Rule of Thumb:** Always verify `pom.xml` is loaded after cloning or creating a Spring Boot project. Look for the Maven tool window on the right sidebar.

---

## ❌ Issue 2 — Database Auto-Configuration Failure

### 🔍 Symptom

Application fails to start with:

```
Failed to configure a DataSource: 'url' attribute is not specified
and no embedded datasource could be configured.
```

### 🧐 Root Cause

Adding `spring-boot-starter-data-jpa` to `pom.xml` tells Spring Boot: *"this project uses a database."* Spring Boot's auto-configuration then tries to create a `DataSource` bean at startup — but since no database URL, username, or password has been configured yet, it crashes.

This is expected behaviour. Spring Boot is opinionated: if JPA is on the classpath, it assumes a DB is required.

### ✅ Fix — Option 1: Exclude Auto-Configuration (Temporary)

Use this during early development before a real database is connected:

```java
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.autoconfigure.jdbc.DataSourceAutoConfiguration;

@SpringBootApplication(exclude = { DataSourceAutoConfiguration.class })
public class UrlShortenerApplication {
    public static void main(String[] args) {
        SpringApplication.run(UrlShortenerApplication.class, args);
    }
}
```

> ⚠️ **Remember to remove this exclusion once you configure a real database.**

### ✅ Fix — Option 2: Configure a Database (Production Path)

Add database credentials to `src/main/resources/application.properties`:

```properties
# Example using PostgreSQL
spring.datasource.url=jdbc:postgresql://localhost:5432/url_shortener
spring.datasource.username=your_username
spring.datasource.password=your_password
spring.jpa.hibernate.ddl-auto=update
```

Or use H2 (in-memory) for quick local development:

```xml
<!-- pom.xml -->
<dependency>
    <groupId>com.h2database</groupId>
    <artifactId>h2</artifactId>
    <scope>runtime</scope>
</dependency>
```

```properties
# application.properties
spring.datasource.url=jdbc:h2:mem:testdb
spring.h2.console.enabled=true
```

### 💡 How Spring Boot Auto-Configuration Works

```
Dependency on classpath
        ↓
Spring Boot detects it (e.g., JPA → needs DataSource)
        ↓
Attempts to create the bean automatically
        ↓
No config found → ❌ Startup crash
```

> **📌 Rule of Thumb:** Every Spring Boot starter you add may come with auto-configuration side effects. Always check what a starter expects and configure it — or explicitly exclude it.

---

## ❌ Issue 3 — Port Already in Use

### 🔍 Symptom

Application fails to start with:

```
Web server failed to start. Port 8080 was already in use.
```

### 🧐 Root Cause

A previous instance of the application (or another process) is still bound to port `8080`. Operating systems allow only **one process per port** at a time.

This commonly happens when:
- You stopped the app using the IDE's terminal but the process kept running in the background
- Another Spring Boot project is already running on the same port

---

### ✅ Fix — Option 1: Stop via IntelliJ (Preferred)

Click the **⛔ Stop** button in IntelliJ's Run panel. This cleanly terminates the process and releases the port.

---

### ✅ Fix — Option 2: Kill the Process via Terminal

**Windows:**

```bash
# Find the PID using port 8080
netstat -ano | findstr :8080

# Kill the process (replace <PID> with actual value)
taskkill /PID <PID> /F
```

**macOS / Linux:**

```bash
# Find the PID
lsof -i :8080

# Kill the process
kill -9 <PID>
```

---

### ✅ Fix — Option 3: Change the Port

If you need multiple services running simultaneously, change the port in `application.properties`:

```properties
server.port=8081
```

> **📌 Rule of Thumb:** Port conflicts are one of the most common issues in backend development. Always check for lingering processes before assuming your code is broken. Keep one instance running per project during local development.

---

## ❌ Issue 4 — Whitelabel Error Page (404)

### 🔍 Symptom

Opening `http://localhost:8080/` in the browser shows:

```
Whitelabel Error Page
This application has no explicit mapping for /error
```

### 🧐 Root Cause

Spring Boot has no route defined for `/`. A backend application does not serve anything unless you explicitly map a URL path to a method. There is no "default homepage" — every endpoint must be declared.

### ✅ Fix — Create a Controller

Create a new file `HealthController.java` in your main package:

```java
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
public class HealthController {

    @GetMapping("/health")
    public String health() {
        return "✅ URL Shortener service is running.";
    }
}
```

Now visiting `http://localhost:8080/health` returns a response instead of a 404.

### 💡 Understanding Spring MVC Routing

| Annotation | Purpose |
|---|---|
| `@RestController` | Marks class as a REST controller; returns data directly (not a view) |
| `@GetMapping("/path")` | Maps HTTP GET requests for `/path` to this method |
| `@PostMapping("/path")` | Maps HTTP POST requests |
| `@PathVariable` | Extracts variables from the URL path |

For a URL Shortener, your core endpoints will look like this:

```java
@PostMapping("/shorten")
public String shorten(@RequestBody String originalUrl) { ... }

@GetMapping("/{shortCode}")
public void redirect(@PathVariable String shortCode, HttpServletResponse response) { ... }
```

> **📌 Rule of Thumb:** In Spring Boot, nothing is implicit. Every URL your application responds to must have a corresponding mapping. A 404 always means *no matching route was found* — check your controller annotations first.

---

## ❌ Issue 5 — Repository Bean Not Found

### 🔍 Symptom

Application fails to start with:

```
No qualifying bean of type 'UrlRepository' available
```

### 🧐 Root Cause

Spring Boot couldn't find or create the repository because database auto-configuration was disabled. When you exclude `DataSourceAutoConfiguration`, Spring never sets up JPA — so it never creates the `UrlRepository` bean either. It's like trying to use a librarian who was never hired.

In our case, this exclusion was still present from an earlier step:

```java
@SpringBootApplication(exclude = {DataSourceAutoConfiguration.class})
```

A second possible cause: the repository file was placed outside Spring's component scan path (wrong package). Spring only looks for beans inside the base package — anything outside is invisible to it.

### ✅ Fix

Remove the exclusion from the main application class:

```java
// Before (broken):
@SpringBootApplication(exclude = {DataSourceAutoConfiguration.class})

// After (correct):
@SpringBootApplication
public class UrlShortenerApplication {
    public static void main(String[] args) {
        SpringApplication.run(UrlShortenerApplication.class, args);
    }
}
```

Also verify your repository file is inside the base package:

```
src/main/java/com/urlshortener/backend/
├── UrlShortenerApplication.java       ← base package lives here
└── repository/
    └── UrlRepository.java             ← must be somewhere under this root
```

### 💡 How Spring Bean Creation Works

Spring creates beans (managed objects) in a specific order at startup:

```
1. DataSourceAutoConfiguration runs → creates DB connection
        ↓
2. JPA auto-configuration runs → sets up Hibernate
        ↓
3. Repository beans are created → UrlRepository is ready
        ↓
4. Service beans are created → UrlShortenerService receives UrlRepository
```

Disabling step 1 breaks the entire chain. Nothing in steps 2, 3, or 4 can run.

> **📌 Rule of Thumb:** Never disable `DataSourceAutoConfiguration` once you've configured a real database. That exclusion is only a temporary workaround for Step 2 of this project — remove it as soon as `application.properties` has your database credentials.

---

## ❌ Issue 6 — Java File Outside Source Root

### 🔍 Symptom

IntelliJ shows a warning banner on the file:

```
Java file is outside of module source root
```

The class cannot be imported elsewhere, and Spring cannot detect it.

### 🧐 Root Cause

The file was accidentally created outside the designated source directory. Maven and IntelliJ only treat files inside `src/main/java` as compilable Java source. Files created anywhere else are treated as plain text — they don't compile, and Spring can't scan them.

A common mistake: creating a new file in the project root or inside `src/` directly instead of `src/main/java/`.

```
# ❌ Wrong locations
src/UrlRepository.java
UrlRepository.java
backend/UrlRepository.java

# ✅ Correct location
src/main/java/com/urlshortener/backend/repository/UrlRepository.java
```

### ✅ Fix

Move the file into the correct package inside `src/main/java`:

```
src/main/java/com/urlshortener/backend/
├── controller/
│   └── UrlShortenerController.java
├── service/
│   └── UrlShortenerService.java
├── repository/
│   └── UrlRepository.java          ← file belongs here
├── model/
│   └── Url.java
└── UrlShortenerApplication.java
```

In IntelliJ: right-click the file → Refactor → Move → select the correct package.

### 💡 Why Maven Has a Fixed Folder Structure

Maven uses a **convention-over-configuration** approach. It expects source files in `src/main/java` so that it never needs to be told where to look — this is the agreed-upon standard across all Maven projects worldwide. Any tool (IntelliJ, Jenkins, GitHub Actions) that understands Maven automatically knows where your code lives.

> **📌 Rule of Thumb:** Always create new classes by right-clicking the target *package* in IntelliJ's Project Explorer, not the file system. This guarantees the file lands in the right place with the correct `package` declaration at the top.

---

## ❌ Issue 7 — Invalid Maven Dependencies

### 🔍 Symptom

Build fails during Maven sync or `mvn install` with:

```
'dependencies.dependency.version' is missing for org.springframework.boot:spring-boot-starter-data-jpa-test
```

Or similar errors about unresolvable artifact coordinates.

### 🧐 Root Cause

Non-existent dependency names were used in `pom.xml`. These artifacts simply do not exist on Maven Central:

```xml
<!-- ❌ These do not exist — Spring never published them -->
<artifactId>spring-boot-starter-data-jpa-test</artifactId>
<artifactId>spring-boot-starter-webmvc-test</artifactId>
```

Spring Boot provides one unified test starter that covers JPA testing, web layer testing, mocking, and assertions all in one:

### ✅ Fix

Replace any test-related dependencies with the single official starter:

```xml
<dependency>
    <groupId>org.springframework.boot</groupId>
    <artifactId>spring-boot-starter-test</artifactId>
    <scope>test</scope>
</dependency>
```

The `<scope>test</scope>` means this dependency is only available during test compilation and execution — it won't be included in your production JAR.

`spring-boot-starter-test` bundles everything you need for testing a Spring Boot application: JUnit 5, Mockito, AssertJ, Spring Test, and more.

> **📌 Rule of Thumb:** When adding Spring Boot dependencies, always search [mvnrepository.com](https://mvnrepository.com) or the [Spring Initializr](https://start.spring.io) to confirm an artifact exists before adding it. If you can't find it there, it doesn't exist.

---

## ❌ Issue 8 — Maven Command Not Found

### 🔍 Symptom

Running `mvn` in the terminal produces:

```
'mvn' is not recognized as an internal or external command
```

Or on macOS/Linux:

```
command not found: mvn
```

### 🧐 Root Cause

Maven is not installed globally on the machine, or it is installed but not added to the system's `PATH` environment variable. The `PATH` is the list of folders your terminal searches when you type a command — if Maven's `bin/` folder isn't in it, the terminal can't find `mvn`.

### ✅ Fix — Use the Maven Wrapper (Recommended)

You don't need to install Maven at all. Every Spring Boot project generated by Spring Initializr includes a **Maven Wrapper** — a self-contained script that downloads and uses the correct Maven version automatically:

```bash
# Windows
.\mvnw clean install
.\mvnw spring-boot:run

# macOS / Linux
./mvnw clean install
./mvnw spring-boot:run
```

If you get a permission error on macOS/Linux:
```bash
chmod +x mvnw   # make the script executable
./mvnw spring-boot:run
```

### ✅ Fix — Install Maven Globally (Optional)

If you prefer the global `mvn` command:

1. Download Maven from [maven.apache.org](https://maven.apache.org/download.cgi)
2. Extract it and note the path (e.g. `C:\Program Files\Apache\maven`)
3. Add `{maven-path}/bin` to your system's `PATH` environment variable
4. Restart the terminal and verify: `mvn -version`

> **📌 Rule of Thumb:** Always use `mvnw` over global `mvn`. It ensures every developer on the project — and every CI/CD pipeline — uses the exact same Maven version. Global installs vary between machines and cause subtle build differences.

---

## ❌ Issue 9 — Lombok Annotation Processor Error

### 🔍 Symptom

Build fails with:

```
annotationProcessorPath dependencies failed to resolve
version cannot be null for org.projectlombok:lombok
```

### 🧐 Root Cause

A custom Maven compiler plugin block was added to `pom.xml` that manually configured Lombok as an annotation processor path — but without specifying a version number:

```xml
<!-- ❌ Missing version — Maven doesn't know which Lombok to download -->
<annotationProcessorPaths>
    <path>
        <groupId>org.projectlombok</groupId>
        <artifactId>lombok</artifactId>
        <!-- no <version> here — Maven rejects this -->
    </path>
</annotationProcessorPaths>
```

### ✅ Fix — Remove the Custom Plugin Block

Spring Boot's parent POM (`spring-boot-starter-parent`) already handles Lombok's annotation processor automatically when you add the dependency. The custom plugin block is unnecessary and actively causes the error.

Your `pom.xml` only needs the dependency itself:

```xml
<!-- Just this — Spring Boot handles the rest -->
<dependency>
    <groupId>org.projectlombok</groupId>
    <artifactId>lombok</artifactId>
    <optional>true</optional>
</dependency>
```

No version needed either — `spring-boot-starter-parent` manages Lombok's version through its dependency management section.

### 💡 What Lombok Does

Lombok is a code-generation library. It reads annotations like `@Getter`, `@Setter`, `@NoArgsConstructor` during compilation and generates the boilerplate Java code automatically:

```java
// What you write with Lombok:
@Getter
@Setter
@NoArgsConstructor
public class Url {
    private Long id;
    private String shortCode;
    private String originalUrl;
}

// What Lombok generates for you at compile time:
// getId(), setId(), getShortCode(), setShortCode(),
// getOriginalUrl(), setOriginalUrl(), Url() constructor
```

> **📌 Rule of Thumb:** Avoid adding custom plugin configuration to `pom.xml` unless you have a specific reason and know exactly what you're overriding. Spring Boot's parent POM already configures most tools correctly — extra configuration usually causes conflicts, not improvements.

---

## ❌ Issue 10 — Redis Command Not Found

### 🔍 Symptom

Running `redis-server` in the terminal produces:

```
'redis-server' is not recognized as an internal or external command
```

### 🧐 Root Cause

Redis is not natively supported on Windows. The official Redis project only maintains builds for Linux and macOS. While third-party Windows ports exist, they are outdated and not recommended for development.

### ✅ Fix — Run Redis via Docker (Recommended for All Platforms)

Docker runs Redis inside a Linux container regardless of your host OS — Windows, macOS, or Linux all get the same, official, up-to-date Redis:

```bash
docker run -d --name redis-cache -p 6379:6379 redis:7
```

Verify it's running:
```bash
docker exec -it redis-cache redis-cli
PING
# Expected response: PONG
```

Stop and restart when needed:
```bash
docker stop redis-cache
docker start redis-cache
```

### ✅ Fix — Install Natively (macOS / Linux only)

```bash
# macOS
brew install redis
brew services start redis

# Ubuntu / Debian
sudo apt install redis-server
sudo systemctl start redis
```

> **📌 Rule of Thumb:** Use Docker for infrastructure tools like Redis, PostgreSQL, and Kafka during local development. It gives you clean, version-pinned instances that start in seconds and don't pollute your machine with global installations. When you move to production, the same Docker image runs on the server.

---

## ❌ Issue 11 — Spring Boot Version Compatibility

### 🔍 Symptom

Redis dependency fails to resolve, or the application fails to start with unexpected errors after adding new dependencies.

### 🧐 Root Cause

Using an unstable or unsupported combination of Spring Boot and Java versions:

```
Spring Boot 4.x  ← does not exist as a stable release
Java 25          ← not an LTS version, limited framework support
```

Spring Boot releases follow a specific support lifecycle, and not every Java version is an LTS (Long-Term Support) release. Mixing bleeding-edge versions causes dependency resolution failures, missing APIs, and unpredictable behaviour.

### ✅ Fix

Use a stable, LTS-aligned stack:

```
Spring Boot 3.2.x  ← stable, widely supported
Java 21            ← LTS release, fully supported by Spring Boot 3.x
```

In `pom.xml`, verify your parent:
```xml
<parent>
    <groupId>org.springframework.boot</groupId>
    <artifactId>spring-boot-starter-parent</artifactId>
    <version>3.2.5</version>
</parent>
```

And in IntelliJ: File → Project Structure → Project SDK → select Java 21.

### 💡 What LTS Means

Java releases two versions per year, but only some are designated **LTS (Long-Term Support)** — meaning they receive security patches and bug fixes for years. Non-LTS versions are supported for only 6 months.

| Java Version | Type | Support Until |
|---|---|---|
| Java 17 | LTS | 2029 |
| Java 21 | LTS ✅ | 2031 |
| Java 22, 23, 24 | Non-LTS | 6 months each |
| Java 25 | LTS (future) | Not yet released |

Spring Boot aligns its support with Java LTS versions. Using a non-LTS Java version with Spring Boot is like building on a foundation that will be removed in 6 months.

> **📌 Rule of Thumb:** For any project you intend to run, share, or put on a resume — use LTS Java + a stable Spring Boot release. Save experimental versions for throwaway experiments.

---

## ❌ Issue 12 — Redis Not Being Used (Logical Check)

### 🔍 Symptom

The application works correctly, Redis is running, but the console only ever shows database queries — never a cache hit. Every request goes to PostgreSQL.

### 🧐 Root Cause

Redis is connected but the cache lookup logic in the service isn't being reached, or it's always returning `null` even when data should be cached. Common causes:

- The `redisTemplate.opsForValue().set()` call is missing — data is fetched from DB but never stored in Redis
- The key used to store and the key used to retrieve don't match (e.g. storing with a prefix but looking up without one)
- Redis connection is failing silently and the app falls back to the database every time without logging an error

### ✅ Fix — Trace the Flow with Console Logs

Add explicit log statements so you can see exactly which path each request takes:

```java
public String getOriginalUrl(String shortCode) {

    // Step 1: Check Redis
    String cachedUrl = redisTemplate.opsForValue().get(shortCode);

    if (cachedUrl != null) {
        System.out.println("⚡ Cache HIT — served from Redis: " + shortCode);
        return cachedUrl;
    }

    // Step 2: Cache miss — go to DB
    System.out.println("💾 Cache MISS — fetching from DB: " + shortCode);
    String originalUrl = repository.findByShortCode(shortCode)
            .map(Url::getOriginalUrl)
            .orElseThrow(() -> new RuntimeException("Not found: " + shortCode));

    // Step 3: Store in Redis for next time
    redisTemplate.opsForValue().set(shortCode, originalUrl);
    System.out.println("📥 Stored in Redis: " + shortCode);

    return originalUrl;
}
```

**Expected console output:**

```
First request:
💾 Cache MISS — fetching from DB: abc123
📥 Stored in Redis: abc123

Second request (same short code):
⚡ Cache HIT — served from Redis: abc123
```

If the second request still shows `Cache MISS`, verify Redis directly:

```bash
docker exec -it redis-cache redis-cli
GET abc123
```

If it returns `nil`, the `set()` call isn't working — check that Redis is actually connected and the key matches exactly.

> **📌 Rule of Thumb:** Always test both cache hit and cache miss explicitly. Make two requests to the same short URL and confirm the second one never touches the database. A cache that isn't being used provides zero benefit — and you won't know unless you verify it.

---

## 🧠 Final Takeaways

| # | Lesson |
|---|---|
| 1 | **Always import `pom.xml` as a Maven project** — IntelliJ won't function correctly without it |
| 2 | **Every starter has configuration requirements** — understand what Spring Boot expects before adding dependencies |
| 3 | **Port conflicts are environment issues, not code bugs** — check running processes before debugging your code |
| 4 | **Explicit over implicit** — Spring Boot does a lot automatically, but routing always needs to be declared |
| 5 | **Never disable DataSourceAutoConfiguration with a live DB** — it breaks the entire JPA/Repository bean chain |
| 6 | **Always create Java files inside `src/main/java`** — files outside are invisible to the compiler and Spring |
| 7 | **Verify dependency names before adding them** — non-existent artifacts fail silently until build time |
| 8 | **Prefer `mvnw` over global `mvn`** — the wrapper guarantees version consistency across all environments |
| 9 | **Don't override Spring Boot's plugin configuration** — the parent POM already handles tools like Lombok correctly |
| 10 | **Use Docker for infrastructure tools** — Redis, PostgreSQL, and Kafka run cleanly without native installation |
| 11 | **Use LTS Java + stable Spring Boot** — bleeding-edge versions cause dependency failures and unpredictable behaviour |
| 12 | **Always verify your cache with two requests** — a misconfigured cache looks identical to a working one until you check |

---

> 💬 **A note on errors:** Every issue documented here is a normal part of development. The goal isn't to avoid errors — it's to recognise patterns quickly and understand *why* something failed, not just *how* to fix it. That understanding is what separates a working prototype from a production-ready service.