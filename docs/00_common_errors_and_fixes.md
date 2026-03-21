# 🛠️ Common Errors & Fixes — URL Shortener Backend

> A practical debugging guide documenting real issues encountered during project setup, with root-cause explanations and step-by-step fixes.

---

## 📋 Table of Contents

- [Issue 1 — Maven Project Not Recognized](#-issue-1--maven-project-not-recognized)
- [Issue 2 — Database Auto-Configuration Failure](#-issue-2--database-auto-configuration-failure)
- [Issue 3 — Port Already in Use](#-issue-3--port-already-in-use)
- [Issue 4 — Whitelabel Error Page (404)](#-issue-4--whitelabel-error-page-404)
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

## 🧠 Final Takeaways

| # | Lesson |
|---|---|
| 1 | **Always import `pom.xml` as a Maven project** — IntelliJ won't function correctly without it |
| 2 | **Every starter has configuration requirements** — understand what Spring Boot expects before adding dependencies |
| 3 | **Port conflicts are environment issues, not code bugs** — check running processes before debugging your code |
| 4 | **Explicit over implicit** — Spring Boot does a lot automatically, but routing always needs to be declared |

---

> 💬 **A note on errors:** Every issue documented here is a normal part of development. The goal isn't to avoid errors — it's to recognise patterns quickly and understand *why* something failed, not just *how* to fix it. That understanding is what separates a working prototype from a production-ready service.