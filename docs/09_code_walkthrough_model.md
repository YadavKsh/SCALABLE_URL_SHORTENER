# 📦 Code Walkthrough — `Url.java` (Model)

> The Model is the simplest file in the project, but it carries significant weight. It is the single source of truth for what a URL mapping looks like — both as a Java object in memory and as a row in the PostgreSQL database. This document explains every line, including the invisible code Lombok generates for you.

---

## 📋 Table of Contents

- [The Full File at a Glance](#-the-full-file-at-a-glance)
- [Package Declaration](#1-package-declaration)
- [Import Statements](#2-import-statements)
- [Class-Level Annotations](#3-class-level-annotations)
- [Fields and Their Annotations](#4-fields-and-their-annotations)
- [Constructors](#5-constructors)
- [What Lombok Generates Behind the Scenes](#6-what-lombok-generates-behind-the-scenes)
- [What the Database Table Looks Like](#7-what-the-database-table-looks-like)
- [How the Model Connects to the Rest of the Project](#-how-the-model-connects-to-the-rest-of-the-project)

---

## 📄 The Full File at a Glance

```java
package com.urlshortener.backend.model;

import jakarta.persistence.*;
import lombok.Data;

@Data
@Entity
public class Url {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(unique = true)
    private String shortCode;

    @Column(unique = true, columnDefinition = "TEXT")
    private String originalUrl;

    public Url() {}

    public Url(String shortCode, String originalUrl) {
        this.shortCode = shortCode;
        this.originalUrl = originalUrl;
    }
}
```

That's the entire file — 28 lines. Yet it defines a full database table, a Java data object, and all the boilerplate methods needed to use it. Most of the heavy lifting is done invisibly by the annotations.

---

## 1. Package Declaration

```java
package com.urlshortener.backend.model;
```

This file lives in the `model` layer — the package dedicated to classes that represent data. Maps to `src/main/java/com/urlshortener/backend/model/` on disk.

The model layer sits at the bottom of the architecture. It has no knowledge of the controller, the service, or Redis. It only knows one thing: what a URL mapping looks like. This makes it the most stable layer in the project — it almost never changes.

```
Controller   ← knows about Service
Service      ← knows about Repository and Model
Repository   ← knows about Model
Model        ← knows about nothing (pure data)
```

---

## 2. Import Statements

```java
import jakarta.persistence.*;
```

Imports everything from the `jakarta.persistence` package at once using the `*` wildcard. This package is part of **JPA (Java Persistence API)** — the standard Java specification for mapping objects to database tables.

The specific annotations it covers here: `@Entity`, `@Id`, `@GeneratedValue`, `@Column`, and `GenerationType`. Without this import, none of those annotations would be recognised by the compiler.

> **`jakarta` vs `javax`:** You may see older tutorials using `javax.persistence.*`. In Spring Boot 3.x (which this project uses), the namespace moved from `javax` to `jakarta` as part of the Jakarta EE migration. They are functionally identical — just a rename. If you see `javax.persistence` errors, it means you're on an older Spring Boot version.

---

```java
import lombok.Data;
```

Imports Lombok's `@Data` annotation. Lombok is a code-generation library that reads annotations during compilation and writes boilerplate Java code so you don't have to. `@Data` is its most commonly used annotation, explained in detail in section 3.

---

## 3. Class-Level Annotations

```java
@Data
```

`@Data` is a Lombok shortcut that combines five annotations into one:

| What Lombok generates | What it does |
|---|---|
| `@Getter` | A `getX()` method for every field |
| `@Setter` | A `setX()` method for every field |
| `@ToString` | A `toString()` method that prints all fields |
| `@EqualsAndHashCode` | `equals()` and `hashCode()` methods based on all fields |
| `@RequiredArgsConstructor` | A constructor for all `final` fields (none here, so this produces no extra constructor) |

Without `@Data`, you would need to write all of this manually — which for 3 fields would be roughly 50 lines of repetitive, error-prone code. Lombok generates it all at compile time, invisibly. The generated code is not in the file but it exists in the compiled `.class` file.

The service calls `url.getShortCode()` and `url.getOriginalUrl()` — those methods come from `@Data`. You never wrote them, but they exist.

---

```java
@Entity
```

This is the most important annotation on this class. It tells JPA: *"This Java class represents a table in the database. Every instance of this class represents one row in that table."*

When Spring Boot starts with `spring.jpa.hibernate.ddl-auto=update` in `application.properties`, Hibernate (the JPA implementation Spring uses) reads every `@Entity` class and creates or updates the corresponding database table automatically. The class name `Url` becomes the table name (by default, lowercase: `url`) — though best practice is to specify it explicitly with `@Table(name = "url_mappings")`.

```
Java class  Url          →   database table  url
Java field  shortCode    →   database column short_code
Java field  originalUrl  →   database column original_url
Java object new Url(...) →   one row in the table
```

> **📌 Note:** JPA automatically converts Java's `camelCase` field names to SQL's `snake_case` column names. `shortCode` becomes `short_code`, `originalUrl` becomes `original_url`. This is the default naming strategy — no configuration needed.

---

```java
public class Url {
```

Standard Java class declaration. `public` so it's accessible from the service and repository layers. The class is named `Url` because it represents a URL mapping — the noun the entire system is built around.

---

## 4. Fields and Their Annotations

### The Primary Key

```java
@Id
```
Tells JPA: *"This field is the primary key of the table."* Every database table needs exactly one primary key — a column that uniquely identifies each row. Without `@Id`, JPA doesn't know which field to use as the identifier and will throw an error on startup.

---

```java
@GeneratedValue(strategy = GenerationType.IDENTITY)
```
Tells JPA: *"I don't want to set the ID manually — let the database auto-assign it."*

`GenerationType.IDENTITY` means the database handles ID generation using its own auto-increment mechanism. In PostgreSQL, this maps to a `BIGSERIAL` or `SERIAL` column — every time a new row is inserted, the database automatically assigns the next number.

```
First URL inserted  →  id = 1
Second URL inserted →  id = 2
Third URL inserted  →  id = 3
...and so on
```

You never write `url.setId(...)` anywhere in the code. The database assigns the ID during `repository.save(url)`, and JPA immediately updates the Java object so `url.getId()` returns the assigned value after the save.

The other `GenerationType` strategies exist for different use cases:

| Strategy | How ID is assigned |
|---|---|
| `IDENTITY` | Database auto-increments (our choice — simplest) |
| `SEQUENCE` | Uses a database sequence object (more flexible) |
| `TABLE` | Uses a separate table to track IDs (slower, rarely used) |
| `AUTO` | JPA picks a strategy automatically based on the database |

---

```java
private Long id;
```
`Long` (capital L) is Java's object wrapper for a 64-bit integer. It can hold values from roughly −9.2 quintillion to +9.2 quintillion — effectively unlimited for a URL shortener's row count.

Why `Long` and not `int`? Two reasons. First, `int` is a primitive and can't be `null` — JPA needs the ID field to be nullable before the record is saved (when the ID hasn't been assigned yet). Second, `Long` supports far larger values than `int`'s maximum of ~2.1 billion.

---

### The Short Code Column

```java
@Column(unique = true)
private String shortCode;
```

`@Column` is how you customise how a field maps to its database column.

`unique = true` — adds a `UNIQUE` constraint to the `short_code` column in PostgreSQL. This means the database will **reject any insert** that tries to store a short code that already exists. It's a safety net at the database level — even if the service's collision-handling code had a bug, the database would prevent duplicate short codes.

This `unique = true` also creates an **index** on the column automatically. An index is like a book's index — it lets the database find a row by `short_code` almost instantly instead of scanning every row. This is critical for performance: every redirect performs a `findByShortCode()` lookup, and that lookup hits this index.

No `columnDefinition` needed — `String` maps to `VARCHAR(255)` by default, which is more than enough for a 6-character code.

---

### The Original URL Column

```java
@Column(unique = true, columnDefinition = "TEXT")
private String originalUrl;
```

Two attributes here:

**`unique = true`** — no two rows can store the same original URL. This supports the deduplication check in `shortenUrl()`: if someone shortens `https://google.com` twice, the service finds the existing record and returns the same short code, never inserting a duplicate. The database constraint makes this guarantee bulletproof.

**`columnDefinition = "TEXT"`** — overrides the default column type. By default, JPA maps `String` to `VARCHAR(255)`, which has a maximum length of 255 characters. URLs can be far longer than that — query parameters alone can push a URL past 255 characters. `TEXT` in PostgreSQL stores strings of unlimited length. This is the correct type for URLs.

---

## 5. Constructors

```java
public Url() {}
```

The **no-argument constructor** (also called the default constructor or no-args constructor). This appears to do nothing — and in your code, you never call it directly. But it is **required by JPA**.

When JPA fetches a row from the database, it first creates a blank `Url` object using this no-args constructor, then populates each field using the setter methods. If this constructor didn't exist, JPA would throw an error every time it tried to read a row. It's invisible but essential.

```
JPA fetching a row from DB:
1. Url url = new Url()            ← uses no-args constructor
2. url.setId(1L)                  ← sets each field from the row
3. url.setShortCode("abc123")
4. url.setOriginalUrl("https://...")
5. return url                     ← hands the fully-populated object to your code
```

---

```java
public Url(String shortCode, String originalUrl) {
    this.shortCode = shortCode;
    this.originalUrl = originalUrl;
}
```

The **convenience constructor** used in the service layer:

```java
Url url = new Url(shortCode, originalUrl);
```

This lets you create a fully-populated `Url` object in one line instead of three:

```java
// Without the convenience constructor (verbose):
Url url = new Url();
url.setShortCode(shortCode);
url.setOriginalUrl(originalUrl);

// With the convenience constructor (clean):
Url url = new Url(shortCode, originalUrl);
```

Notice that `id` is not a parameter. That's intentional — `id` is assigned by the database on insert, so there's nothing to pass in. You create the object without an ID; the database assigns one during `repository.save(url)`.

`this.shortCode = shortCode` — `this.shortCode` refers to the field on the object. `shortCode` (without `this`) refers to the constructor parameter. This line assigns the parameter value to the field.

---

## 6. What Lombok Generates Behind the Scenes

Because of `@Data`, the following code is **generated at compile time** and added to the `.class` file — you don't see it in the source, but it's fully available at runtime:

```java
// Generated by @Getter
public Long getId() { return id; }
public String getShortCode() { return shortCode; }
public String getOriginalUrl() { return originalUrl; }

// Generated by @Setter
public void setId(Long id) { this.id = id; }
public void setShortCode(String shortCode) { this.shortCode = shortCode; }
public void setOriginalUrl(String originalUrl) { this.originalUrl = originalUrl; }

// Generated by @ToString
public String toString() {
    return "Url(id=" + id + ", shortCode=" + shortCode + ", originalUrl=" + originalUrl + ")";
}

// Generated by @EqualsAndHashCode
public boolean equals(Object o) { /* compares all fields */ }
public int hashCode() { /* hash based on all fields */ }
```

The service calls `url.getShortCode()` and `url.getOriginalUrl()` — both of those come from this generated code. JPA calls `url.setId()`, `url.setShortCode()`, `url.setOriginalUrl()` when loading rows from the database — those setters also come from this generated code.

Without Lombok, the model file would be about 80 lines instead of 28.

---

## 7. What the Database Table Looks Like

When Spring Boot starts with `ddl-auto=update`, Hibernate reads the `Url` entity and creates this table in PostgreSQL:

```sql
CREATE TABLE url (
    id           BIGSERIAL PRIMARY KEY,
    short_code   VARCHAR(255) UNIQUE,
    original_url TEXT UNIQUE
);
```

And here's what a few rows look like:

```
 id │ short_code │ original_url
────┼────────────┼──────────────────────────────────────────────────
  1 │ abc123     │ https://example.com/very/long/url/with/parameters
  2 │ Xy3K9a     │ https://github.com/some-user/some-repository
  3 │ q7mN2z     │ https://docs.spring.io/spring-framework/reference
```

Each Java object instance maps to exactly one row. `new Url("abc123", "https://...")` becomes row 1.

---

## 🔗 How the Model Connects to the Rest of the Project

The `Url` class is used in three places — but never by the controller:

```
UrlShortenerService
    │
    ├── new Url(shortCode, originalUrl)
    │       creates an object to save
    │
    ├── url.getShortCode()
    │       reads the short code from a found record
    │
    └── url.getOriginalUrl()
            reads the original URL from a found record

UrlRepository
    │
    ├── JpaRepository<Url, Long>
    │       tells Spring this repository works with Url objects
    │
    └── Optional<Url> findByShortCode(...)
            returns a Url wrapped in Optional

Hibernate (JPA's engine — runs automatically)
    │
    ├── Reads @Entity, @Id, @Column annotations
    ├── Creates the table on startup
    ├── Uses no-args constructor to reconstruct objects from DB rows
    └── Uses setters (from @Data) to populate each field
```

The controller never touches the `Url` class directly. It only receives and returns strings — the service is responsible for converting between strings and `Url` objects. This is intentional: the controller should be unaware of how data is stored. If you rename a field in the model, only the service needs to update its calls to `getShortCode()` or `getOriginalUrl()` — the controller is unaffected.