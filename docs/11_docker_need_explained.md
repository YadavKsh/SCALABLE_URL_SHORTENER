# 🐳 Why We Used Docker — URL Shortener Backend

> Docker wasn't added to this project to be trendy. It was added because deployment was broken without it. This document explains the specific problems encountered, why they happened, and exactly how Docker solved each one.

---

## 📋 Table of Contents

- [The Problem](#-the-problem)
- [What Was Failing and Why](#-what-was-failing-and-why)
- [What Docker Actually Is](#-what-docker-actually-is)
- [How Docker Solved Each Problem](#-how-docker-solved-each-problem)
- [The Dockerfile — Line by Line](#-the-dockerfile--line-by-line)
- [Why Docker Was Specifically Required for Render](#-why-docker-was-specifically-required-for-render)
- [The Mental Model](#-the-mental-model)
- [Key Takeaway](#-key-takeaway)

---

## 🧩 The Problem

The Spring Boot backend worked perfectly on a local machine. The moment it was pushed to Render for deployment, it failed — with a series of errors that had nothing to do with the application code itself.

The root cause of every single failure was the same thing: **the server didn't have the same environment as the development machine**. Different operating system configuration, different tools installed, different versions, different file permissions. The code was correct. The environment wasn't.

This is one of the most common and frustrating problems in software deployment, and it has a name: the **"works on my machine"** problem.

```
Local machine:   ✅ App runs perfectly
Render server:   ❌ Fails before even starting
```

The code in both cases is identical. The difference is everything around it.

---

## ❌ What Was Failing and Why

### 1. Java Not Configured Correctly

**Error:**
```
JAVA_HOME is not defined correctly
```

**Why it happened:** The application requires JDK 21 specifically. Render's default server environment either didn't have Java installed, or had a different version. The environment variable `JAVA_HOME` — which tells the operating system where to find Java — wasn't set to a valid location.

**Why this is hard to fix manually:** You'd have to SSH into the server, install the correct JDK version, set `JAVA_HOME` in the right config file, and hope it persists across deploys. This is fragile, hard to reproduce, and breaks every time Render provisions a new server instance.

---

### 2. Maven Not Available

**Error:**
```
mvn: command not found
```

**Why it happened:** Maven is the build tool that compiles the Spring Boot project and packages it into a runnable JAR file. Render's server environment didn't have Maven installed, so the build command simply didn't exist.

The project includes a Maven Wrapper (`mvnw`) specifically to avoid needing a global Maven installation — but that introduced the next problem.

---

### 3. Maven Wrapper Had No Execute Permission

**Error:**
```
Permission denied: ./mvnw
```

**Why it happened:** On Linux (which is what Render's servers run), shell scripts are not executable by default. The `mvnw` file was created on a Windows or macOS machine and committed to Git without execute permissions. When Render cloned the repository and tried to run `./mvnw`, Linux refused because the file wasn't marked as executable.

This is a file permission problem that is entirely invisible during local development on Windows and easy to miss on macOS.

---

### 4. Build Fails Due to Mismatched Versions

**Error:**
```
Build failed — incompatible Java version / missing tool
```

**Why it happened:** Even when Maven was available, the Java version on the server might not match what the project was written for. Spring Boot 3.x requires Java 17 minimum and was developed with Java 21. If the server had Java 11 or Java 17 with a different patch version, certain APIs and language features used in the code wouldn't compile or run correctly.

Version mismatches are silent and inconsistent — they might work in some environments and fail in others for reasons that are difficult to diagnose.

---

### 5. Runtime Behaviour Differs Between Environments

Even when a build succeeded, the running application could behave differently because:
- The underlying Linux distribution has different default settings
- File system paths are structured differently
- Network configurations differ
- Environment variables expected by the app are missing

All of these are the same root problem: the server is not the same environment as the development machine, and there's no guarantee it will be unless you make it so explicitly.

---

## 🐳 What Docker Actually Is

Docker is a tool that lets you define an exact environment — the operating system, the tools, the runtime, the configuration — and package your application inside that definition. The result is called a **container**.

A container is self-contained. It carries everything the application needs to run: the OS layer, the JDK, Maven, the compiled code. When the container starts, it always starts with exactly the same environment — regardless of what machine or cloud server it's running on.

The analogy: think of a container like a **shipping container** on a cargo ship. The container doesn't care whether it's on a ship, a truck, or in a warehouse. Whatever is inside is perfectly preserved. The handler (the cloud server) just needs to be able to run containers — it doesn't need to know anything about what's inside.

```
Without Docker:
┌──────────────────┐     ┌──────────────────┐
│ Local Machine    │     │ Render Server    │
│ Java 21 ✅       │ ≠   │ Java 17 ❌        │
│ Maven 3.9 ✅     │     │ No Maven ❌       │
│ Ubuntu 22.04 ✅  │     │ Debian 11 ❌      │
└──────────────────┘     └──────────────────┘
  App works here            App fails here

With Docker:
┌──────────────────────────────────────────────┐
│ Docker Container (same everywhere)           │
│ eclipse-temurin:21-jdk-jammy                 │
│ Java 21 ✅  Maven via mvnw ✅  Ubuntu ✅     │
└──────────────────────────────────────────────┘
  Local machine  →  Render  →  Any cloud  →  All identical ✅
```

---

## ✅ How Docker Solved Each Problem

| Problem | What Docker Does |
|---|---|
| Java not installed / wrong version | The Docker image `eclipse-temurin:21-jdk-jammy` includes JDK 21 — it's baked into the image, not installed separately |
| Maven not found | The Maven Wrapper (`mvnw`) is included in the project files that are copied into the container. It downloads Maven itself on first run. |
| `mvnw` permission denied | `RUN chmod +x mvnw` makes the script executable inside the container before it's called |
| Mismatched versions | The container always uses exactly JDK 21 on Ubuntu 22.04 — no version drift, no surprises |
| Runtime environment differences | The entire runtime is defined in the Dockerfile — same OS, same Java, same configuration, every time |
| Manual server configuration | None required. Render runs the container. Everything the app needs is inside it. |

---

## 📄 The Dockerfile — Line by Line

```dockerfile
FROM eclipse-temurin:21-jdk-jammy
```

Sets the **base image** — the starting point for the container. This is a pre-built image maintained by the Eclipse Foundation that contains:
- Ubuntu 22.04 LTS ("Jammy Jellyfish") — a stable, widely-used Linux distribution
- JDK 21 — the full Java Development Kit, not just the runtime

This single line solves the Java installation problem entirely. You never install Java manually.

`eclipse-temurin` is the recommended OpenJDK distribution for production. It receives regular security patches and is one of the most widely used JDK distributions in industry.

---

```dockerfile
WORKDIR /app
```

Creates a directory called `/app` inside the container and sets it as the working directory for all subsequent commands. Think of it as doing `mkdir /app && cd /app` inside the container.

Any `COPY`, `RUN`, or `CMD` instructions that follow will execute relative to this path. Using a dedicated working directory keeps the container's file system organised — your files are isolated in `/app` rather than scattered across the root.

---

```dockerfile
COPY . .
```

Copies everything from your project folder (on the machine running the Docker build) into `/app` (inside the container). The first `.` is the source (your project root), the second `.` is the destination (the current working directory inside the container, which is `/app`).

After this line, the entire project — `backend/`, `frontend/`, `docs/`, everything — exists inside the container.

---

```dockerfile
WORKDIR /app/backend
```

Changes the working directory to `/app/backend` — the subfolder where the Spring Boot application lives. Since the repo has both `frontend/` and `backend/` at the root, this narrows focus to just the backend for the remaining commands.

All subsequent `RUN` commands execute from here, so `./mvnw` refers to `/app/backend/mvnw`.

---

```dockerfile
RUN chmod +x mvnw
```

Makes the Maven Wrapper script executable inside the container. This is the direct fix for the `Permission denied: ./mvnw` error.

`chmod` is the Linux command for changing file permissions. `+x` adds the execute permission. Without this line, the next `RUN` instruction would fail with the same permission error that caused deployment failures before Docker.

This must appear **before** the `./mvnw` call. Order matters in Dockerfiles — each `RUN` instruction executes sequentially.

---

```dockerfile
RUN ./mvnw clean package -DskipTests
```

Runs the Maven build inside the container. This is the command that compiles all the Java source files and packages them into a single executable JAR file.

Breaking down the flags:

`clean` — deletes any previous build output from the `target/` directory before starting. Ensures the build starts from a clean state and isn't affected by leftover files from a previous build.

`package` — tells Maven to run the full build lifecycle: compile the source code, run any processors (like Lombok), and package everything into a JAR file at `target/backend-0.0.1-SNAPSHOT.jar`.

`-DskipTests` — skips running automated tests during the build. Tests that involve the database or Redis would fail at build time because neither PostgreSQL nor Redis is available inside the Docker build environment. The tests themselves are correct — they just need a running database to work, which isn't present during the image build.

After this line, the runnable JAR exists at `/app/backend/target/backend-0.0.1-SNAPSHOT.jar`.

---

```dockerfile
EXPOSE 8081
```

Documents that the application inside the container listens for incoming connections on port 8081. This is primarily informational — it tells other developers and deployment platforms what port to expect the app to use.

Importantly, `EXPOSE` does not actually open the port. The actual port mapping happens when you run the container (`docker run -p 8081:8081`) or when Render configures its routing. Render reads the `PORT` environment variable and routes external traffic to whatever port the app is listening on.

---

```dockerfile
CMD ["java", "-jar", "target/backend-0.0.1-SNAPSHOT.jar"]
```

The command that runs when the container starts. This is equivalent to running this in the terminal:

```bash
java -jar target/backend-0.0.1-SNAPSHOT.jar
```

It starts the Spring Boot application by executing the JAR file built in the previous step.

`CMD` is different from `RUN`. `RUN` executes during the **image build** phase (when creating the container). `CMD` executes when the **container starts** (at deployment, every time the server restarts the container).

The array format `["java", "-jar", "..."]` is the preferred syntax because it runs the command directly — without invoking a shell first. This means signals like `SIGTERM` (sent when the server wants to stop the container gracefully) reach the Java process directly, allowing for clean shutdown.

---

## 🖥️ Why Docker Was Specifically Required for Render

Render supports several deployment methods. For a Java Spring Boot application, the options are:

```
Option 1: Native build environment
→ Render would need to install Java, Maven, configure PATH, etc.
→ Render does not have a pre-configured Java build environment
→ This requires complex manual configuration that breaks across deploys

Option 2: Docker container ✅
→ You define the environment once in the Dockerfile
→ Render builds and runs the container
→ No Java installation required on Render's side
→ Works identically every deploy
```

Docker is the correct choice for any language or runtime that isn't natively supported by a platform's build system. Java falls into this category on Render's free tier. The Dockerfile is a complete, self-contained build specification that any container-capable platform can execute — Render, AWS, Google Cloud, Azure, or a server you configure yourself.

---

## 🧠 The Mental Model

Think of Docker in three stages:

```
1. Dockerfile  →  Recipe
   Defines what the environment should look like.
   "Start with Ubuntu + JDK 21. Copy these files. Run this build. Start with this command."

2. Docker Image  →  Snapshot
   The result of building the Dockerfile.
   A frozen, complete snapshot of the environment and compiled application.
   Built once, used many times.

3. Docker Container  →  Running instance
   The image brought to life.
   What actually runs on Render's server.
   You can run multiple containers from the same image.
```

When you push to GitHub and Render redeploys:
1. Render builds a new image from your Dockerfile
2. Render stops the old container
3. Render starts a new container from the new image
4. Your updated application is live

---

## 🧠 Key Takeaway

Every deployment failure encountered before Docker — Java not found, Maven not installed, permission denied, environment mismatch — was a symptom of the same underlying problem: **the server's environment was not controlled**.

Docker solves this by making the environment part of the code. The `Dockerfile` is a versioned, committed, reproducible specification of exactly what the application needs to run. It travels with the code. It is built the same way every time. It runs the same way everywhere.

The result: deployment goes from "configure the server to match your app" to "ship the app with its own environment". That shift is what makes Docker the industry standard for deploying backend services.

```
Before Docker:   Code is portable. Environment is not.
After Docker:    Code and environment are portable together.
```