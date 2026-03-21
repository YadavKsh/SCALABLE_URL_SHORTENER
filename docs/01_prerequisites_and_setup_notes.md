# ⚙️ Setup, Prerequisites & Project Structure

> Before writing a single line of application code, your local environment and project structure need to be correct. This guide explains the tools involved, why they matter, and how to avoid the most common setup mistakes.

---

## 📋 Table of Contents

- [Prerequisites Checklist](#-prerequisites-checklist)
- [Understanding the Tools](#-understanding-the-tools)
    - [What is Maven?](#-what-is-maven)
    - [What is pom.xml?](#-what-is-pomxml)
    - [What is mvnw / mvnw.cmd?](#-what-is-mvnw--mvnwcmd)
- [Expected Project Structure](#-expected-project-structure)
- [Common Setup Issue — mvnw Not Found](#-common-setup-issue--mvnw-not-found)
- [Ways to Run the Project](#-ways-to-run-the-project)
- [Key Takeaway](#-key-takeaway)

---

## ✅ Prerequisites Checklist

Before running this project, confirm the following are installed:

| Tool | Minimum Version | Check Command | Purpose |
|---|---|---|---|
| **Java JDK** | 17+ | `java -version` | Compiles and runs the application |
| **Maven** *(optional)* | 3.6+ | `mvn -version` | Build tool — not needed if using `mvnw` |
| **IntelliJ IDEA** *(optional)* | Any | — | Recommended IDE for Spring Boot development |
| **Git** | Any | `git --version` | Cloning the repository |

> **💡 Note:** Maven itself does not need to be installed globally. The project ships with a **Maven Wrapper** (`mvnw`) that handles this for you — explained below.

---

## 🧠 Understanding the Tools

### 📦 What is Maven?

Maven is a **build and dependency management tool** for Java projects. Think of it like `npm` for Node.js or `pip` for Python — but for Java.

Without Maven, you would need to:
- Manually download every library (Spring Boot, JDBC drivers, testing frameworks, etc.)
- Add them to your classpath by hand
- Write custom scripts to compile and package your code

Maven automates all of this. You declare what your project needs in `pom.xml`, and Maven handles the rest.

```
You declare dependencies in pom.xml
            ↓
Maven reads pom.xml at build time
            ↓
Downloads required JARs from Maven Central (remote repository)
            ↓
Caches them locally at ~/.m2/repository
            ↓
Makes them available to your project
```

---

### 📄 What is `pom.xml`?

`pom.xml` (Project Object Model) is the **central configuration file** for your Maven project. It lives at the root of the `backend/` folder and defines everything Maven needs to know about your project.

A minimal `pom.xml` contains three things:

```xml
<!-- 1. Project identity -->
<groupId>com.example</groupId>
<artifactId>url-shortener</artifactId>
<version>0.0.1-SNAPSHOT</version>

<!-- 2. Dependencies — libraries your project needs -->
<dependencies>
    <dependency>
        <groupId>org.springframework.boot</groupId>
        <artifactId>spring-boot-starter-web</artifactId>
    </dependency>
</dependencies>

<!-- 3. Build plugins — tools that run during compilation -->
<build>
    <plugins>
        <plugin>
            <groupId>org.springframework.boot</groupId>
            <artifactId>spring-boot-maven-plugin</artifactId>
        </plugin>
    </plugins>
</build>
```

> **📌 Important:** IntelliJ must recognise `pom.xml` as a Maven project for the IDE to resolve dependencies and enable the Run button. If the project looks like a plain folder, right-click `pom.xml` → **Add as Maven Project**. See the [Common Errors guide](./common-errors-and-fixes.md) for details.

---

### 🔧 What is `mvnw` / `mvnw.cmd`?

These are **Maven Wrapper** scripts bundled with your project.

| File | Platform |
|---|---|
| `mvnw` | Linux / macOS |
| `mvnw.cmd` | Windows |

The wrapper solves a common team problem: *"It works on my machine."* Instead of requiring every developer to install the same version of Maven globally, the wrapper downloads and uses the exact Maven version the project was built with — automatically, on first run.

This means a developer can clone the repository and run the project immediately, even with no Maven installation.

```bash
# This works even if Maven is not installed globally
./mvnw spring-boot:run
```

> **📌 Important:** `mvnw` must be **executable** on Linux/macOS. If you get a permission error, run:
> ```bash
> chmod +x mvnw
> ```

---

## 📁 Expected Project Structure

After cloning or extracting the project, your folder structure should look exactly like this:

```
scalable-url-shortener/
└── backend/
    ├── mvnw                  ← Maven Wrapper (Linux/macOS)
    ├── mvnw.cmd              ← Maven Wrapper (Windows)
    ├── pom.xml               ← Project configuration & dependencies
    └── src/
        ├── main/
        │   ├── java/
        │   │   └── com/example/urlshortener/
        │   │       └── BackendApplication.java
        │   └── resources/
        │       └── application.properties
        └── test/
            └── java/
```

Every file has a specific role:

| File / Folder | Purpose |
|---|---|
| `mvnw` / `mvnw.cmd` | Runs Maven without a global installation |
| `pom.xml` | Declares dependencies and build config |
| `src/main/java/` | Your application source code |
| `src/main/resources/` | Config files (`application.properties`) |
| `src/test/java/` | Unit and integration tests |

---

## ❌ Common Setup Issue — `mvnw` Not Found

### Symptom

```bash
bash: ./mvnw: No such file or directory
```

Or in IntelliJ: Maven sync fails, no dependencies load, no Run button.

### Root Cause

The project was extracted into a **nested folder** by mistake. This happens when unzipping creates a duplicate parent directory:

```
# ❌ Incorrect — double-nested
scalable-url-shortener/
└── backend/
    └── backend/        ← duplicate folder
        ├── mvnw
        └── pom.xml

# ✅ Correct — mvnw and pom.xml directly inside backend/
scalable-url-shortener/
└── backend/
    ├── mvnw
    └── pom.xml
```

### Fix

Navigate into the correct directory before running any commands:

```bash
# Confirm you're in the right place — you should see pom.xml
ls

# Expected output:
# mvnw  mvnw.cmd  pom.xml  src/
```

If the structure is nested, move the contents up one level:

```bash
# macOS / Linux
mv backend/backend/* backend/
rm -r backend/backend/
```

---

## ▶️ Ways to Run the Project

There are three ways to start the application. Choose based on your setup.

---

### Option 1 — IntelliJ IDEA *(Recommended for beginners)*

1. Open IntelliJ and select **File → Open** → navigate to the `backend/` folder
2. Wait for Maven sync to complete (progress bar at the bottom)
3. Open `src/main/java/com/example/urlshortener/BackendApplication.java`
4. Click the **▶️ Run** button next to the `main` method

**Why this is recommended:** IntelliJ handles the Maven lifecycle, classpath, and JDK configuration internally. The feedback on errors is also much clearer than the terminal.

---

### Option 2 — Maven Wrapper *(No global Maven needed)*

```bash
# Navigate to the backend directory first
cd scalable-url-shortener/backend

# Run the application
./mvnw spring-boot:run        # Linux / macOS
mvnw.cmd spring-boot:run      # Windows
```

Use this when working from the terminal or in a CI/CD pipeline.

---

### Option 3 — Global Maven Installation

```bash
cd scalable-url-shortener/backend
mvn spring-boot:run
```

Use this only if Maven is already installed globally (`mvn -version` confirms this). Functionally identical to Option 2 — the wrapper just removes the requirement.

---

### Confirming the App is Running

Regardless of which method you use, a successful startup looks like this in the console:

```
Started BackendApplication in 2.345 seconds (process running for 2.8)
```

Then visit `http://localhost:8080/health` — if you see a response, the server is up.

---

## 🧠 Key Takeaway

Setup problems are environment problems, not code problems. The two most common causes of a broken start are:

1. **Wrong folder structure** — `pom.xml` and `mvnw` are buried in a nested directory
2. **Maven not recognised by IntelliJ** — the IDE opened the folder but never imported it as a Maven project

Spending five minutes verifying your structure and tool setup before writing code saves hours of confusing errors later. Understanding *what each file does* — not just that it exists — is what makes debugging these issues fast.