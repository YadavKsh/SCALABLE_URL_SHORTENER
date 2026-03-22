# 🧠 Code Walkthrough — `UrlShortenerService.java`

> The Service is the brain of the application. While the Controller handles the HTTP conversation, the Service handles every decision — checking for duplicates, generating short codes, talking to the database, and managing the Redis cache. This document explains every single line.

---

## 📋 Table of Contents

- [The Full File at a Glance](#-the-full-file-at-a-glance)
- [Package Declaration](#1-package-declaration)
- [Import Statements](#2-import-statements)
- [Class-Level Annotation and Declaration](#3-class-level-annotation-and-declaration)
- [Fields — The Dependencies and Constants](#4-fields--the-dependencies-and-constants)
- [The Constructor](#5-the-constructor)
- [shortenUrl() — Creating a Short Code](#6-shortenurl--creating-a-short-code)
- [getOriginalUrl() — The Cache-Aside Lookup](#7-getoriginalurl--the-cache-aside-lookup)
- [generateShortCode() — Building the Code](#8-generateshortcode--building-the-code)
- [How All Three Methods Connect](#-how-all-three-methods-connect)

---

## 📄 The Full File at a Glance

```java
package com.urlshortener.backend.service;

import com.urlshortener.backend.model.Url;
import com.urlshortener.backend.repository.UrlRepository;
import org.springframework.stereotype.Service;
import org.springframework.data.redis.core.StringRedisTemplate;

import java.util.Optional;
import java.util.Random;

@Service
public class UrlShortenerService {

    private final UrlRepository repository;
    private final StringRedisTemplate redisTemplate;
    private static final String CHARSET = "abcdefghijklmnopqrstuvwxyzABCDEFGHIJKLMNOPQRSTUVWXYZ0123456789";
    private static final int BASE = CHARSET.length();

    public UrlShortenerService(UrlRepository repository, StringRedisTemplate redisTemplate) {
        this.repository = repository;
        this.redisTemplate = redisTemplate;
    }

    public String shortenUrl(String originalUrl) {
        Optional<Url> existing = repository.findByOriginalUrl(originalUrl);
        if (existing.isPresent()) {
            return existing.get().getShortCode();
        }

        String shortCode = generateShortCode();

        while (repository.findByShortCode(shortCode).isPresent()) {
            shortCode = generateShortCode();
        }

        Url url = new Url(shortCode, originalUrl);
        repository.save(url);
        redisTemplate.opsForValue().set(shortCode, originalUrl);

        return shortCode;
    }

    public String getOriginalUrl(String shortCode) {
        String cachedUrl = redisTemplate.opsForValue().get(shortCode);

        if (cachedUrl != null) {
            System.out.println("⚡ Served from Redis");
            return cachedUrl;
        }

        Optional<Url> url = repository.findByShortCode(shortCode);

        if (url.isPresent()) {
            String originalUrl = url.get().getOriginalUrl();
            redisTemplate.opsForValue().set(shortCode, originalUrl);
            System.out.println("💾 Fetched from DB and cached");
            return originalUrl;
        }

        return null;
    }

    private String generateShortCode() {
        StringBuilder shortCode = new StringBuilder();
        Random random = new Random();

        for (int i = 0; i < 6; i++) {
            shortCode.append(CHARSET.charAt(random.nextInt(BASE)));
        }

        return shortCode.toString();
    }
}
```

---

## 1. Package Declaration

```java
package com.urlshortener.backend.service;
```

Declares that this file lives in the `service` layer of the project. Maps to the folder path `src/main/java/com/urlshortener/backend/service/` on disk.

Keeping services in their own package enforces the three-layer architecture — it's immediately obvious from the package name what role this class plays. Spring also uses this path as part of its component scan to find and register this class as a bean.

---

## 2. Import Statements

```java
import com.urlshortener.backend.model.Url;
```
Your own `Url` model class — the Java object that maps to the `url_mappings` table in PostgreSQL. The service uses it to create new records and read existing ones.

---

```java
import com.urlshortener.backend.repository.UrlRepository;
```
Your own repository interface. This is the messenger between the service and PostgreSQL — calling `repository.save()`, `repository.findByShortCode()`, etc.

---

```java
import org.springframework.stereotype.Service;
```
The `@Service` annotation lives in this package. Without this import, the annotation at the top of the class wouldn't be recognised.

---

```java
import org.springframework.data.redis.core.StringRedisTemplate;
```
Spring's built-in class for talking to Redis. `StringRedisTemplate` is specifically designed for cases where both keys and values are plain strings — which is exactly our case (`shortCode` → `originalUrl`). This is what allows you to call `.opsForValue().get()` and `.set()` to read and write from the cache.

---

```java
import java.util.Optional;
```
`Optional` is a Java wrapper class that honestly represents a value that **might or might not exist**. Instead of returning `null` when something isn't found (which can cause crashes if you forget to check), `Optional` forces you to consciously handle both the "found" and "not found" cases. Used here because `repository.findByShortCode()` might find a match or might find nothing.

---

```java
import java.util.Random;
```
Java's built-in random number generator. Used in `generateShortCode()` to pick random characters from the Base62 character set.

---

## 3. Class-Level Annotation and Declaration

```java
@Service
```
Tells Spring: *"This class contains business logic — create one instance of it and keep it in the application container."*

When Spring starts up, it scans the project, finds `@Service` on this class, creates a single instance (called a **bean**), and makes it available for injection elsewhere. That's how the `UrlShortenerController` receives it in its constructor — Spring created the `UrlShortenerService` bean first, then passed it into the controller.

`@Service` is functionally identical to `@Component` (a generic Spring-managed class), but using `@Service` specifically communicates intent: *"this is the business logic layer."* It makes the code self-documenting.

---

```java
public class UrlShortenerService {
```
Standard Java class declaration. `public` means Spring (and anything else in the project) can access it. The class name describes exactly what it does — no ambiguity.

---

## 4. Fields — The Dependencies and Constants

```java
private final UrlRepository repository;
```
Holds the reference to the repository that communicates with PostgreSQL. `private` — nothing outside this class can touch it directly. `final` — assigned once in the constructor and never changed. Declaring dependencies as `final` is a best practice: it guarantees the service always has a valid repository from the moment it's created.

---

```java
private final StringRedisTemplate redisTemplate;
```
Holds the reference to Spring's Redis client. Same `private final` reasoning as above. This is the object you call `.opsForValue().get()` and `.set()` on.

---

```java
private static final String CHARSET = "abcdefghijklmnopqrstuvwxyzABCDEFGHIJKLMNOPQRSTUVWXYZ0123456789";
```
The alphabet for short code generation. Contains all 62 URL-safe characters:
- `a-z` → 26 lowercase letters
- `A-Z` → 26 uppercase letters
- `0-9` → 10 digits

**`private`** — only used inside this class.

**`static`** — belongs to the class itself, not to any particular instance. There's only ever one copy of this string in memory, shared across all method calls. Since this string never changes, making it `static` is correct and efficient.

**`final`** — the value can never be reassigned after declaration. Together with `static`, this is the Java convention for a **constant**.

**ALL_CAPS naming** — `CHARSET` and `BASE` are written in uppercase with underscores. This is the universal Java convention for constants. Any developer reading this code immediately knows these values never change.

---

```java
private static final int BASE = CHARSET.length();
```
The total number of characters in `CHARSET` — which is `62`. Used in `generateShortCode()` as the upper bound for the random number picker.

Rather than hardcoding the magic number `62` directly in the code (which is cryptic), storing it as `BASE` makes the intent clear. It also means if you ever add or remove characters from `CHARSET`, `BASE` automatically stays correct without a separate update.

`CHARSET.length()` is called once at class load time and stored. It doesn't recompute on every method call.

---

## 5. The Constructor

```java
public UrlShortenerService(UrlRepository repository, StringRedisTemplate redisTemplate) {
    this.repository = repository;
    this.redisTemplate = redisTemplate;
}
```

Constructor injection — the same pattern used in the controller. Spring sees that `UrlShortenerService` needs a `UrlRepository` and a `StringRedisTemplate`, finds both beans already created in its container, and passes them in automatically.

`StringRedisTemplate` is auto-created by Spring Boot when it finds `spring-boot-starter-data-redis` on the classpath and a valid Redis host/port in `application.properties`. You never create it manually — Spring provides it.

Two dependencies injected, two `this.x = x` assignments. Each `this.` prefix distinguishes the class field from the constructor parameter that happens to share the same name.

---

## 6. `shortenUrl()` — Creating a Short Code

```java
public String shortenUrl(String originalUrl) {
```
`public` — callable from the controller. Returns a `String` — specifically the generated short code (e.g. `"abc123"`), not the full URL. The controller is responsible for assembling the full short URL string.

---

### Duplicate Check

```java
Optional<Url> existing = repository.findByOriginalUrl(originalUrl);
```
Before generating anything, check: has this exact URL been shortened before?

`repository.findByOriginalUrl(originalUrl)` runs a database query: `SELECT * FROM url_mappings WHERE original_url = ?`. If a match is found, it's wrapped in `Optional.of(result)`. If not found, it returns `Optional.empty()`.

This is a **deduplication check** — if someone shortens `https://google.com` twice, they get the same short code back both times. This avoids cluttering the database with duplicate records for the same URL.

---

```java
if (existing.isPresent()) {
    return existing.get().getShortCode();
}
```
`existing.isPresent()` — checks whether the `Optional` contains a value (i.e. the URL was found in the database). Returns `true` if found, `false` if not.

`existing.get()` — unwraps the `Optional` to get the actual `Url` object inside. You should only call `.get()` after confirming `.isPresent()` is `true` — calling it on an empty `Optional` throws an exception.

`.getShortCode()` — gets the `shortCode` field from the `Url` entity. This is the getter method generated either by Lombok or written manually in the model.

The method **returns early** here — no code below this runs if the URL already exists. This is called a **guard clause**: handle the special case first, then proceed with the normal flow.

---

### Generating a Unique Short Code

```java
String shortCode = generateShortCode();
```
Calls the private helper method (explained in section 8) which picks 6 random Base62 characters and returns a string like `"Xy3K9a"`.

---

```java
while (repository.findByShortCode(shortCode).isPresent()) {
    shortCode = generateShortCode();
}
```
**Collision handling.** A collision is when two different URLs randomly generate the same short code.

With 62⁶ = 56 billion possible codes this is extremely rare, but *possible*. This loop handles it correctly: keep generating a new code until we find one that doesn't already exist in the database.

`repository.findByShortCode(shortCode).isPresent()` — checks if this code is already taken. If it is (`true`), generate a new one and check again. The loop only exits when a truly unique code is found.

This is a **do-until** pattern using a `while` loop. In practice, this loop almost never executes more than once — but the one time it would have caused a collision, this code silently handles it.

---

### Saving to the Database

```java
Url url = new Url(shortCode, originalUrl);
```
Creates a new `Url` entity object with the generated short code and the original URL. This object represents the row that will be inserted into the `url_mappings` table. The constructor `new Url(shortCode, originalUrl)` is defined in the model class.

---

```java
repository.save(url);
```
Tells Spring Data JPA to persist this object to PostgreSQL. Behind the scenes, JPA runs:
```sql
INSERT INTO url_mappings (short_code, original_url) VALUES ('abc123', 'https://...')
```
The `id` column is auto-generated by the database — you don't set it manually. After `save()` returns, the record exists permanently on disk.

---

### Populating the Cache Immediately

```java
redisTemplate.opsForValue().set(shortCode, originalUrl);
```
After saving to PostgreSQL, immediately store the same mapping in Redis.

This is a deliberate choice: the very first redirect for this short code will be a **cache hit** — Redis already has it. Without this line, the first redirect would be a cache miss (hitting the database), and Redis would only be populated from the second request onward.

`opsForValue()` — selects the String value operations from Redis (as opposed to lists, sets, or other Redis data structures).
`.set(shortCode, originalUrl)` — stores the key-value pair in Redis: `"abc123"` → `"https://example.com/..."`.

---

```java
return shortCode;
```
Returns the short code string back to the controller (`"abc123"`). The controller then builds the full URL (`"http://localhost:8081/abc123"`) and includes it in the response.

---

## 7. `getOriginalUrl()` — The Cache-Aside Lookup

```java
public String getOriginalUrl(String shortCode) {
```
Called by the controller on every redirect request. Takes the short code from the URL path and returns the original long URL — or `null` if it doesn't exist.

---

### Step 1 — Check Redis First

```java
String cachedUrl = redisTemplate.opsForValue().get(shortCode);
```
Sends the `GET shortCode` command to Redis. Returns the cached original URL if it exists, or `null` if this short code has never been cached.

This is the fast path — Redis operates entirely in RAM and responds in under a millisecond. If the answer is here, we never touch the database.

---

```java
if (cachedUrl != null) {
    System.out.println("⚡ Served from Redis");
    return cachedUrl;
}
```
If Redis returned a value (not `null`), we have a **cache hit**. Return immediately. The `System.out.println` is a development log — it lets you verify in the console that Redis is actually being used. You'd replace this with a proper logger (`SLF4J` / `Logback`) in production.

The method **returns early** again — database code below never runs on a cache hit.

---

### Step 2 — Fallback to PostgreSQL

```java
Optional<Url> url = repository.findByShortCode(shortCode);
```
Redis didn't have it — **cache miss**. Now query PostgreSQL.

`findByShortCode(shortCode)` runs: `SELECT * FROM url_mappings WHERE short_code = ?`. Returns an `Optional` containing the `Url` entity if found, or empty if the short code doesn't exist anywhere.

---

```java
if (url.isPresent()) {
    String originalUrl = url.get().getOriginalUrl();
```
If the database found a matching row:
- `url.isPresent()` — confirms the `Optional` contains a value
- `url.get()` — unwraps the `Optional` to get the `Url` entity
- `.getOriginalUrl()` — extracts the original URL string from the entity

---

```java
    redisTemplate.opsForValue().set(shortCode, originalUrl);
```
**Cache population.** Now that we've fetched from the database, store the result in Redis so the *next* request for this short code is a cache hit.

This is the essence of the **Cache-Aside pattern**: you don't pre-load the cache. You load it lazily — only when something is actually requested and found in the database.

---

```java
    System.out.println("💾 Fetched from DB and cached");
    return originalUrl;
}
```
Logs the cache miss for development visibility, then returns the original URL.

---

```java
return null;
```
Reached only if `url.isPresent()` was `false` — meaning the short code exists neither in Redis nor in PostgreSQL. It simply doesn't exist in the system.

The controller checks for `null` and returns a `404 Not Found` response. Returning `null` here is a simple signal — in a more mature codebase you might throw a custom exception like `ShortCodeNotFoundException` instead, which is cleaner and easier to test.

---

## 8. `generateShortCode()` — Building the Code

```java
private String generateShortCode() {
```
`private` — this is an internal helper. Nothing outside this class calls it directly. It exists purely to serve `shortenUrl()`.

---

```java
StringBuilder shortCode = new StringBuilder();
```
`StringBuilder` is a mutable (changeable) string builder. Ordinary Java `String` objects are **immutable** — every time you concatenate strings with `+`, Java creates a brand new `String` object in memory. For a loop that runs 6 times, that's 6 unnecessary objects.

`StringBuilder` solves this by maintaining one internal character buffer that you append to. When you're done, call `.toString()` once to get the final string. It's the correct tool whenever you're building a string incrementally in a loop.

---

```java
Random random = new Random();
```
Creates a new random number generator. `Random` uses a mathematical formula to produce numbers that appear random. Each call to `random.nextInt(n)` returns a different number between `0` (inclusive) and `n` (exclusive).

---

```java
for (int i = 0; i < 6; i++) {
```
A standard `for` loop that runs exactly 6 times — one iteration per character in the short code.

`int i = 0` — start the counter at 0.
`i < 6` — keep going while `i` is less than 6 (so i = 0, 1, 2, 3, 4, 5 — six iterations total).
`i++` — increment `i` by 1 after each iteration.

Why 6 characters? 62⁶ = 56 billion possible combinations — more than enough for any realistic URL shortener.

---

```java
    shortCode.append(CHARSET.charAt(random.nextInt(BASE)));
```
This single line does three things, from the inside out:

**`random.nextInt(BASE)`** — generates a random integer between `0` and `61` (since `BASE` is `62` and `nextInt` is exclusive of the upper bound). Think of this as rolling a 62-sided die.

**`CHARSET.charAt(...)`** — picks the character at that random index in the `CHARSET` string. Index 0 is `'a'`, index 25 is `'z'`, index 26 is `'A'`, index 51 is `'Z'`, index 52 is `'0'`, index 61 is `'9'`.

```
random.nextInt(62) returns, say, 29
CHARSET.charAt(29) = 'D'  (the 30th character: a-z is 0-25, A-Z starts at 26, so 26='A', 27='B', 28='C', 29='D')
```

**`shortCode.append(...)`** — adds that character to the end of the `StringBuilder`.

After 6 iterations, `shortCode` contains 6 randomly selected Base62 characters.

---

```java
return shortCode.toString();
```
Converts the `StringBuilder` to a regular immutable `String` and returns it. The result is a 6-character code like `"Xy3K9a"` or `"q7mN2z"`.

---

## 🔗 How All Three Methods Connect

```
shortenUrl("https://example.com/...")
          │
          ├─ 1. Check DB for duplicate
          │       └─ Found? → return existing shortCode immediately
          │
          ├─ 2. generateShortCode() → "abc123"
          │       └─ Already taken in DB? → generateShortCode() again
          │
          ├─ 3. Save new Url entity to PostgreSQL
          │
          ├─ 4. Store in Redis immediately
          │
          └─ 5. Return "abc123" to controller


getOriginalUrl("abc123")
          │
          ├─ 1. Check Redis for "abc123"
          │       └─ Found (cache hit)? → return immediately ⚡
          │
          ├─ 2. Query PostgreSQL for "abc123"
          │       └─ Not found? → return null → controller sends 404
          │
          ├─ 3. Store result in Redis for next time
          │
          └─ 4. Return originalUrl to controller → controller sends 302


generateShortCode()   [private — only called by shortenUrl()]
          │
          ├─ Create empty StringBuilder
          ├─ Loop 6 times:
          │       └─ Pick random index (0–61) → get character from CHARSET → append
          └─ Return 6-character string e.g. "Xy3K9a"
```

The service is deliberately the **only place** in the entire application that knows about Redis and PostgreSQL. The controller has no idea how URLs are stored or cached — it just calls `shortenUrl()` and `getOriginalUrl()` and trusts the service to handle the rest. This separation is what makes the code maintainable: to swap Redis for Memcached, or PostgreSQL for MySQL, you change this one file and nothing else.