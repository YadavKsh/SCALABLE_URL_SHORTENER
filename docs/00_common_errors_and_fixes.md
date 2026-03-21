# ⚠️ Common Errors & Fixes (Step-by-Step Debugging Guide)

## 🎯 Goal

Document real issues faced during setup and how to fix them.

This helps:

* Beginners understand problems
* Avoid repeating mistakes
* Learn how systems actually behave

---

# ❌ Issue 1: Maven Project Not Recognized

## 🔍 Problem

* IntelliJ did not show Run ▶️ button
* Dependencies were not loaded
* Project behaved like plain folder

---

## ❓ Why This Happened

IntelliJ did not recognize the project as a **Maven project**.

---

## ✅ Fix

Right-click:

```text
backend/pom.xml → Add as Maven Project
```

---

## 🧠 Explanation

Maven is a **build tool** that:

* Downloads dependencies
* Compiles project
* Manages configuration

Without Maven import, IntelliJ cannot understand the project.

---

## 📌 Key Learning

Always ensure `pom.xml` is properly loaded.

---

# ❌ Issue 2: Database Configuration Error

## 🔍 Problem

Application failed with:

"Failed to configure a DataSource"

---

## ❓ Why This Happened

* Added `Spring Data JPA`
* Spring Boot assumed database is needed
* But no DB configuration was provided

---

## ✅ Fix

Temporarily disable DB auto-configuration:

```java
import org.springframework.boot.jdbc.autoconfigure.DataSourceAutoConfiguration;

@SpringBootApplication(exclude = {DataSourceAutoConfiguration.class})
```

---

## 🧠 Explanation

Spring Boot auto-configures everything.

When JPA is added:
→ It tries to connect to a database automatically

Since no DB config exists → crash

---

## 📌 Key Learning

Spring Boot is powerful but requires proper configuration guidance.

---

# ❌ Issue 3: Port Already in Use

## 🔍 Problem

Application failed with:

"Port 8080 was already in use"

---

## ❓ Why This Happened

Another process (likely previous app instance) was already using port 8080.

---

## ✅ Fix Option 1 (Preferred)

Stop running process in IntelliJ:

* Click ⛔ Stop button

---

## ✅ Fix Option 2

Kill process manually:

```bash
netstat -ano | findstr :8080
taskkill /PID <pid> /F
```

---

## ✅ Fix Option 3 (Quick Workaround)

Change port:

```properties
server.port=8081
```

---

## 🧠 Explanation

Only one application can use a port at a time.

---

## 📌 Key Learning

Port conflicts are very common in backend development.

---

# ❌ Issue 4: Whitelabel Error Page (404)

## 🔍 Problem

Opening:

http://localhost:8080/

Shows 404 error

---

## ❓ Why This Happened

No route (`/`) was defined.

Spring Boot had no instruction for handling root URL.

---

## ✅ Fix

Create a controller:

```java
@GetMapping("/health")
public String health() {
    return "Application is running";
}
```

---

## 🧠 Explanation

Backend applications require explicit routing.

---

## 📌 Key Learning

Nothing works automatically—you must define endpoints.

---

# 🧠 Final Takeaways

* Errors are part of development
* Understanding "why" is more important than fixing
* Proper documentation makes your project production-level

---
