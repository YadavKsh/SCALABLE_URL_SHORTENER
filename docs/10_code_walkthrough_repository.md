# 🗂️ Code Walkthrough — `UrlRepository.java` (Repository)

> The Repository is the most deceptively simple file in the project. It's 12 lines long, has no method bodies, and yet it gives you a complete database access layer — reads, writes, deletes, counts, and custom queries — all without a single line of SQL. This document explains how that's possible and what every part of this file actually does.

---

## 📋 Table of Contents

- [The Full File at a Glance](#-the-full-file-at-a-glance)
- [Package Declaration](#1-package-declaration)
- [Import Statements](#2-import-statements)
- [interface vs class](#3-interface-vs-class)
- [extends JpaRepository\<Url, Long\>](#4-extends-jparepositoryurl-long)
- [The Free Methods You Inherit](#5-the-free-methods-you-inherit)
- [Custom Query Methods](#6-custom-query-methods)
- [How Spring Implements This Interface](#7-how-spring-implements-this-interface)
- [How the Repository Connects to the Rest of the Project](#-how-the-repository-connects-to-the-rest-of-the-project)
- [The Full Picture — All Four Files Together](#-the-full-picture--all-four-files-together)

---

## 📄 The Full File at a Glance

```java
package com.urlshortener.backend.repository;

import com.urlshortener.backend.model.Url;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface UrlRepository extends JpaRepository<Url, Long> {

    Optional<Url> findByShortCode(String shortCode);

    Optional<Url> findByOriginalUrl(String originalUrl);
}
```

12 lines. No SQL. No `@Repository` annotation. No implementation class. And yet the service can call `repository.save()`, `repository.findByShortCode()`, `repository.findByOriginalUrl()`, `repository.existsByShortCode()`, and more — and they all work. Here's why.

---

## 1. Package Declaration

```java
package com.urlshortener.backend.repository;
```

This file lives in the `repository` layer. Maps to `src/main/java/com/urlshortener/backend/repository/` on disk.

The repository layer sits between the service and the database. It's the only layer that communicates with PostgreSQL. The service calls repository methods; the repository translates those calls into SQL and talks to the database.

```
Controller
    ↓
Service          ← calls repository methods
    ↓
Repository       ← translates to SQL, talks to PostgreSQL
    ↓
PostgreSQL
```

---

## 2. Import Statements

```java
import com.urlshortener.backend.model.Url;
```
Your `Url` entity class. The repository needs to know what type of object it's storing and retrieving. Every method that returns a database record returns it as a `Url` object (or `Optional<Url>`).

---

```java
import org.springframework.data.jpa.repository.JpaRepository;
```
The parent interface from Spring Data JPA that this repository extends. `JpaRepository` comes pre-loaded with all the standard database operations — save, find, delete, count, and more. By extending it, your repository inherits all of them for free.

---

```java
import java.util.Optional;
```
The wrapper class for values that might or might not exist. Both custom methods return `Optional<Url>` rather than `Url` directly — because a search might find a record or might find nothing.

---

## 3. `interface` vs `class`

```java
public interface UrlRepository extends JpaRepository<Url, Long> {
```

The first thing to notice: this is an `interface`, not a `class`.

In Java, a `class` provides both the definition of behaviour and the actual implementation — the code that runs. An `interface` only provides the **definition** — it describes what methods exist and what they return, but contains no code for how they work.

```java
// A class — has both definition AND implementation
public class UrlShortenerService {
    public String getOriginalUrl(String shortCode) {
        // actual code here
    }
}

// An interface — has definition ONLY, no implementation
public interface UrlRepository {
    Optional<Url> findByShortCode(String shortCode);
    // no code — just the signature
}
```

Normally you'd need to write a class that `implements` the interface and provides the actual code. But Spring Data JPA reads this interface at startup and **generates the implementation automatically**. You declare what you need; Spring writes the code that makes it work.

This is one of the most powerful features of Spring — you write a contract (the interface), and Spring fulfils it.

---

```java
public interface UrlRepository
```

`public` — accessible from anywhere in the project, specifically from the service layer. The service receives this repository via constructor injection and calls its methods.

`UrlRepository` — the name follows the convention `[EntityName]Repository`. Since the entity is `Url`, the repository is `UrlRepository`. This naming is standard across all Spring projects worldwide — any developer joining the project will immediately understand this file's purpose.

---

## 4. `extends JpaRepository<Url, Long>`

This single clause is what gives you a complete database layer without writing any code.

**`extends`** in the context of interfaces means: *"Inherit all the method definitions from this parent interface."* Whatever `JpaRepository` declares, `UrlRepository` automatically has too.

**`JpaRepository`** is an interface provided by Spring Data JPA. It sits at the top of a hierarchy of repository interfaces, each adding more capabilities:

```
CrudRepository           ← basic save, find, delete, count
    └── PagingAndSortingRepository   ← adds pagination and sorting
            └── JpaRepository        ← adds flush, batch operations, and more
                    └── UrlRepository ← your interface, adds custom queries
```

By extending `JpaRepository`, you inherit everything in the entire chain.

**`<Url, Long>`** — the two generic type parameters that tell `JpaRepository` what it's working with:

```
JpaRepository < Url ,  Long >
               │       │
               │       └── The type of the primary key (id field is Long)
               └────────── The entity class this repository manages
```

This matters because Spring uses these types to generate the correct SQL. It knows to run queries against the `url` table, and it knows the primary key column contains `Long` values. Without these parameters, Spring wouldn't know which table to query or how to identify rows.

---

## 5. The Free Methods You Inherit

By extending `JpaRepository<Url, Long>`, your repository automatically has all of these working methods — no code required:

```java
// ── Saving ────────────────────────────────────────────────────
repository.save(url);
// INSERT if new, UPDATE if already exists (checks by id)
// Returns the saved entity (with id populated after INSERT)

// ── Finding ───────────────────────────────────────────────────
repository.findById(1L);
// SELECT * FROM url WHERE id = 1
// Returns Optional<Url> — empty if not found

repository.findAll();
// SELECT * FROM url
// Returns List<Url> — all rows in the table

repository.existsById(1L);
// SELECT COUNT(*) FROM url WHERE id = 1
// Returns true/false

repository.count();
// SELECT COUNT(*) FROM url
// Returns the total number of rows as long

// ── Deleting ──────────────────────────────────────────────────
repository.delete(url);
// DELETE FROM url WHERE id = ?

repository.deleteById(1L);
// DELETE FROM url WHERE id = 1

repository.deleteAll();
// DELETE FROM url  (clears the entire table)
```

Every one of these is available immediately — you didn't write a single line to get them. Spring generates a proxy class at startup that implements all of these methods with real SQL.

---

## 6. Custom Query Methods

This is where Spring Data JPA's most impressive feature comes in: **derived query methods**. Spring reads the name of your method, parses it like a sentence, and generates the SQL automatically.

### Method 1

```java
Optional<Url> findByShortCode(String shortCode);
```

Spring parses this name word by word:

```
find          →   SELECT *
By            →   WHERE
ShortCode     →   short_code (converts camelCase to snake_case)
```

Generated SQL:
```sql
SELECT * FROM url WHERE short_code = ?
```

The `?` is replaced with the value of `shortCode` parameter when the method is called. This is a **prepared statement** — the SQL template is compiled once and the value is safely substituted at runtime, which also prevents SQL injection attacks.

**Why `Optional<Url>` and not just `Url`?**

Because a search might find a row, or it might find nothing. If you used `Url` as the return type and the short code didn't exist, Spring would return `null` — and forgetting to check for `null` causes a `NullPointerException` crash.

`Optional<Url>` forces the service to explicitly handle both cases:
```java
// Service is forced to consider the "not found" case
Optional<Url> result = repository.findByShortCode("abc123");

if (result.isPresent()) {
    // found — safe to use result.get()
} else {
    // not found — return null or throw exception
}
```

---

### Method 2

```java
Optional<Url> findByOriginalUrl(String originalUrl);
```

Same parsing pattern:

```
find          →   SELECT *
By            →   WHERE
OriginalUrl   →   original_url
```

Generated SQL:
```sql
SELECT * FROM url WHERE original_url = ?
```

This is called in `shortenUrl()` to check for duplicates before creating a new short code. If someone tries to shorten `https://google.com` and it's already in the database, this query finds the existing record so the service can return the existing short code instead of creating a new one.

---

### How Far the Naming Magic Goes

Spring Data's method naming can handle surprisingly complex queries — all from just the method name:

```java
// Two conditions with AND
Optional<Url> findByShortCodeAndOriginalUrl(String shortCode, String originalUrl);
// → SELECT * FROM url WHERE short_code = ? AND original_url = ?

// Just checking existence (returns boolean, not an object)
boolean existsByShortCode(String shortCode);
// → SELECT COUNT(*) FROM url WHERE short_code = ? (returns true if count > 0)

// Counting rows matching a condition
long countByOriginalUrl(String originalUrl);
// → SELECT COUNT(*) FROM url WHERE original_url = ?

// Deleting by a field
void deleteByShortCode(String shortCode);
// → DELETE FROM url WHERE short_code = ?

// Finding multiple results
List<Url> findAllByOriginalUrl(String originalUrl);
// → SELECT * FROM url WHERE original_url = ?  (returns all matches)
```

The naming convention is: `[action]By[FieldName][Condition]`. Spring supports `And`, `Or`, `Not`, `Like`, `Between`, `LessThan`, `GreaterThan`, `OrderBy`, and more — all derived purely from the method name.

---

## 7. How Spring Implements This Interface

You defined an interface. Interfaces in Java have no implementation. So how do the methods actually run?

At application startup, Spring Data JPA:

```
1. Scans for interfaces that extend JpaRepository
         ↓
2. Finds UrlRepository
         ↓
3. Generates a concrete implementation class in memory:
   (something like UrlRepositoryImpl)
         ↓
4. For each method:
   - findByShortCode → parses name → generates SQL → wraps in JDBC call
   - findByOriginalUrl → parses name → generates SQL → wraps in JDBC call
   - save → generates INSERT/UPDATE SQL from the entity
   - findById → generates SELECT WHERE id = ? SQL
   - (all inherited methods similarly)
         ↓
5. Registers UrlRepositoryImpl as a Spring bean
         ↓
6. When UrlShortenerService requests UrlRepository via constructor injection,
   Spring provides this generated implementation
```

This entire process happens invisibly, before your first request arrives. By the time the application is ready to serve traffic, `UrlRepository` is fully implemented and working.

---

## 🔗 How the Repository Connects to the Rest of the Project

```
UrlShortenerService
        │
        │  calls:
        ├── repository.findByOriginalUrl(originalUrl)   ← duplicate check
        ├── repository.findByShortCode(shortCode)       ← collision check + redirect lookup
        ├── repository.save(url)                        ← persist new mapping
        │
        ↓
UrlRepository (interface)
        │
        │  Spring generates implementation at startup
        │
        ↓
PostgreSQL
        │
        ├── url table
        │     id | short_code | original_url
        │      1 | abc123     | https://example.com/...
        │      2 | Xy3K9a     | https://github.com/...
        └── Queries execute here, results return as Url objects
```

The controller never calls the repository directly — it only calls the service. The repository is entirely hidden behind the service layer. This is intentional: if you later wanted to switch from PostgreSQL to MySQL or MongoDB, you'd only change the repository layer. The controller and service would be unaffected.

---

## 🏗️ The Full Picture — All Four Files Together

Now that all four files are documented, here's how they connect as a complete system:

```
HTTP Request arrives
        │
        ▼
┌─────────────────────────────────────────────────────┐
│  UrlShortenerController                             │
│  @RestController                                    │
│                                                     │
│  Receives HTTP request                              │
│  Validates input                                    │
│  Delegates to service                               │
│  Returns HTTP response (200 / 302 / 400 / 404)      │
└────────────────────┬────────────────────────────────┘
                     │ calls
                     ▼
┌─────────────────────────────────────────────────────┐
│  UrlShortenerService                                │
│  @Service                                           │
│                                                     │
│  Checks for duplicate URLs                          │
│  Generates short codes (Base62)                     │
│  Handles collisions                                 │
│  Checks Redis cache first (getOriginalUrl)          │
│  Falls back to DB on cache miss                     │
│  Populates cache after DB hit                       │
└──────┬──────────────────────────┬───────────────────┘
       │ calls                    │ calls
       ▼                          ▼
┌──────────────┐        ┌──────────────────────┐
│UrlRepository │        │  StringRedisTemplate │
│  interface   │        │  (Spring-provided)   │
│              │        │                      │
│ findByShort  │        │  .opsForValue()      │
│   Code()     │        │  .get(shortCode)     │
│ findByOrig   │        │  .set(key, value)    │
│   inalUrl()  │        └──────────┬───────────┘
│ save()       │                   │
└──────┬───────┘                   │
       │ Spring generates          │
       │ SQL at runtime            │
       ▼                           ▼
┌──────────────┐        ┌──────────────────────┐
│  PostgreSQL  │        │       Redis          │
│              │        │                      │
│  url table   │        │  In-memory cache     │
│  (permanent) │        │  (fast, temporary)   │
└──────────────┘        └──────────────────────┘
       ▲
       │ Hibernate reads
       │ @Entity, @Id,
       │ @Column to build
       │ the table
       │
┌──────────────┐
│  Url.java    │
│  @Entity     │
│              │
│  id          │
│  shortCode   │
│  originalUrl │
└──────────────┘
```

Every file has exactly one responsibility:
- **Controller** — HTTP in, HTTP out
- **Service** — all decisions and logic
- **Repository** — database access
- **Model** — what the data looks like

Change one without touching the others. That's the architecture.