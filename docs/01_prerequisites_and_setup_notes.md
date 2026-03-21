# ⚠️ Common Setup Issues & Prerequisites

## 🎯 Goal

Understand the basic setup required to run a Spring Boot project and avoid common mistakes.

---

## 🧠 What is Maven?

Maven is a **build tool**.

### What does that mean?

It helps to:

* Download dependencies (libraries like Spring Boot)
* Compile your code
* Run your application

Instead of manually downloading libraries, Maven does it automatically using `pom.xml`.

---

## 📄 What is `pom.xml`?

This is the **heart of your project configuration**.

It contains:

* Project information
* Dependencies (Spring, database drivers, etc.)
* Build instructions

---

## 🔧 What is `mvnw` / `mvnw.cmd`?

These are **Maven Wrapper files**.

### Why are they important?

They allow anyone to run the project **without installing Maven manually**.

* `mvnw` → for Linux/Mac
* `mvnw.cmd` → for Windows

---

## ⚠️ Common Issue Faced

### Problem:

```
mvnw not found
```

### Cause:

Project was not extracted properly OR folder structure was incorrect.

Example mistake:

```
backend/backend/
```

---

## ✅ Fix

Ensure correct structure:

```
scalable-url-shortener/
└── backend/
    ├── mvnw
    ├── mvnw.cmd
    ├── pom.xml
    └── src/
```

---

## ▶️ Ways to Run the Project

### 1. Using IntelliJ (Recommended for beginners)

* Open `BackendApplication.java`
* Click Run ▶️

### Why?

IDE handles everything internally.

---

### 2. Using Maven Wrapper

```bash
./mvnw spring-boot:run
```

👉 Runs your Spring Boot application

---

### 3. Using Maven (if installed globally)

```bash
mvn spring-boot:run
```

---

## 🧠 Key Takeaway

Understanding your tools (Maven, project structure) is as important as writing code.
