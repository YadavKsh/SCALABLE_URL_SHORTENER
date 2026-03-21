# 🌱 Step 4 — Spring Boot Basics, Annotations & Short Code Generation

> Before adding more features, it's worth understanding what Spring Boot is doing when your application starts and handles a request. This document covers the architecture, every annotation used in this project, redirect mechanics, and how short codes are generated.

---

## 📋 Table of Contents

- [What is Spring Boot?](#-what-is-spring-boot)
- [Project Architecture](#-project-architecture)
- [The Three Layers](#-the-three-layers)
    - [Controller](#-controller)
    - [Service](#-service)
    - [Model](#-model)
- [Annotations Explained](#-annotations-explained)
- [Dependency Injection](#-dependency-injection)
- [Request Flow](#-request-flow)
- [Redirect Handling](#-redirect-handling--the-most-important-concept)
- [Base62 Short Code Generation](#-base62-short-code-generation)
- [Key Takeaway](#-key-takeaway)

---

## 🚀 What is Spring Boot?

Spring Boot is a framework for building backend applications in Java. It removes the boilerplate that traditional Java web development required — no manual server configuration, no XML wiring, no writing factories to create objects.

When you start a Spring Boot application, it:

1. **Starts an embedded web server** (Tomcat by default) — no separate installation needed
2. **Scans your code** for annotated classes (`@RestController`, `@Service`, etc.)
3. **Wires everything together** automatically — classes that depend on each other are connected without manual configuration
4. **Applies sensible defaults** — JSON serialisation, error handling, connection pooling, and more, all pre-configured

The result: you write business logic, Spring handles the plumbing.

---

## 🧱 Project Architecture

This project follows a standard **three-layer architecture**. Each layer has one responsibility and communicates only with the layer directly below it.

```
HTTP Request
     │
     ▼
┌─────────────┐
│  Controller │  ← Receives HTTP request, returns HTTP response
└──────┬──────┘
       │ calls
       ▼
┌─────────────┐
│   Service   │  ← Contains business logic (generate code, lookup URL)
└──────┬──────┘
       │ reads/writes
       ▼
┌─────────────┐
│   Storage   │  ← HashMap (now) → PostgreSQL (next step)
└─────────────┘
```

This separation matters because it means you can swap the storage layer (HashMap → PostgreSQL → Redis) without touching the controller or service. Each layer is independently testable and replaceable.

---

## 🔍 The Three Layers

### 🌐 Controller

The controller is the **entry point for every HTTP request**. Its only job is to:
- Accept the incoming request and extract its data
- Call the appropriate service method
- Return an HTTP response

It contains no logic. It does not talk to storage. It delegates everything to the service.

```java
@RestController
public class UrlShortenerController {

    private final UrlShortenerService service;

    public UrlShortenerController(UrlShortenerService service) {
        this.service = service;
    }

    @PostMapping("/shorten")
    public ResponseEntity<Map<String, String>> shortenUrl(@RequestBody Map<String, String> body) {
        String originalUrl = body.get("url");
        String shortUrl = service.shortenUrl(originalUrl);
        return ResponseEntity.ok(Map.of("shortUrl", shortUrl));
    }

    @GetMapping("/{shortCode}")
    public ResponseEntity<?> redirect(@PathVariable String shortCode) {
        String originalUrl = service.getOriginalUrl(shortCode);
        return ResponseEntity.status(302)
                .location(URI.create(originalUrl))
                .build();
    }
}
```

---

### 🧠 Service

The service is the **brain of the application**. It contains all business logic — decisions, transformations, and rules. The controller tells it *what* to do; the service decides *how* to do it.

```java
@Service
public class UrlShortenerService {

    private final Map<String, String> urlStore = new HashMap<>();

    public String shortenUrl(String originalUrl) {
        String shortCode = generateShortCode();
        urlStore.put(shortCode, originalUrl);
        return "http://localhost:8080/" + shortCode;
    }

    public String getOriginalUrl(String shortCode) {
        String originalUrl = urlStore.get(shortCode);
        if (originalUrl == null) {
            throw new RuntimeException("Short code not found: " + shortCode);
        }
        return originalUrl;
    }

    private String generateShortCode() {
        // Covered in detail in the Base62 section below
        String charset = "abcdefghijklmnopqrstuvwxyzABCDEFGHIJKLMNOPQRSTUVWXYZ0123456789";
        StringBuilder code = new StringBuilder();
        Random random = new Random();
        for (int i = 0; i < 6; i++) {
            code.append(charset.charAt(random.nextInt(charset.length())));
        }
        return code.toString();
    }
}
```

When we replace `HashMap` with PostgreSQL in the next step, only this file changes — the controller remains untouched.

---

### 📦 Model

A model is a Java class that **represents a unit of data** in your system. For now, we're using `Map<String, String>` directly, which is fine for getting the logic right. As the application grows, a dedicated model class makes the code more structured and readable.

**Current approach — simple, no setup:**
```java
Map<String, String> urlStore = new HashMap<>();
// "abc123" → "https://example.com/..."
```

**Future model class — used when integrating with the database:**
```java
@Entity
@Table(name = "url_mappings")
public class UrlMapping {
    private Long id;
    private String shortCode;
    private String originalUrl;
    private LocalDateTime createdAt;
}
```

The model mirrors the database table — one field per column. Spring Data JPA reads this class to know how to read and write rows.

---

## 🏷️ Annotations Explained

Spring Boot uses annotations to identify what role each class or method plays. Here are all the annotations used in this project:

| Annotation | Placed On | What It Does |
|---|---|---|
| `@SpringBootApplication` | Main class | Bootstraps the entire application; triggers component scanning |
| `@RestController` | Controller class | Marks class as an HTTP controller; methods return data (JSON by default) |
| `@Service` | Service class | Marks class as business logic component; makes it available for injection |
| `@Entity` | Model class | Marks class as a JPA entity mapped to a database table |
| `@PostMapping("/path")` | Controller method | Handles `POST` requests to the specified path |
| `@GetMapping("/path")` | Controller method | Handles `GET` requests to the specified path |
| `@RequestBody` | Method parameter | Deserialises the JSON request body into a Java object automatically |
| `@PathVariable` | Method parameter | Extracts a value from the URL path (e.g. `/abc123` → `"abc123"`) |

### How `@RequestBody` Works

```
Incoming JSON body:                    Java object:
{                           →          Map<String, String> body
  "url": "https://google.com"            body.get("url") = "https://google.com"
}
```

Spring handles the JSON-to-Java conversion automatically using the Jackson library. You never write parsing code manually.

### How `@PathVariable` Works

```
URL:      GET /abc123
                ↓
Method:   redirect(@PathVariable String shortCode)
                                         ↓
Result:   shortCode = "abc123"
```

---

## 🔗 Dependency Injection

One of Spring Boot's most important features is **Dependency Injection (DI)**. Instead of creating objects manually with `new`, you declare what a class needs and Spring provides it.

**Without Dependency Injection — manual, tightly coupled:**
```java
public class UrlShortenerController {
    // ❌ You create the object — hard to test, hard to swap implementations
    private UrlShortenerService service = new UrlShortenerService();
}
```

**With Dependency Injection — Spring manages it:**
```java
public class UrlShortenerController {
    private final UrlShortenerService service;

    // ✅ Spring sees this constructor and injects the service automatically
    public UrlShortenerController(UrlShortenerService service) {
        this.service = service;
    }
}
```

Spring scans your code at startup, finds all classes annotated with `@Service`, `@RestController`, etc., creates instances of them, and automatically passes them where they're needed. This is called the **IoC Container** (Inversion of Control).

The practical benefit: when writing tests, you can pass a fake service implementation into the controller without changing a single line of controller code.

---

## 🔄 Request Flow

Tracing a full request through all three layers:

### `POST /shorten` — Creating a Short URL

```
1. Client sends POST /shorten with body: { "url": "https://example.com/..." }
         ↓
2. @PostMapping("/shorten") matches the request
         ↓
3. @RequestBody converts JSON → Map<String, String>
         ↓
4. Controller calls service.shortenUrl(originalUrl)
         ↓
5. Service generates short code, stores mapping, returns short URL
         ↓
6. Controller wraps result in ResponseEntity.ok(...) → HTTP 200
         ↓
7. Client receives: { "shortUrl": "http://localhost:8080/abc123" }
```

### `GET /{shortCode}` — Redirecting

```
1. Browser requests GET /abc123
         ↓
2. @GetMapping("/{shortCode}") matches
         ↓
3. @PathVariable extracts "abc123" from the path
         ↓
4. Controller calls service.getOriginalUrl("abc123")
         ↓
5. Service looks up mapping → "https://example.com/..."
         ↓
6. Controller builds HTTP 302 response with Location header
         ↓
7. Browser follows redirect → loads original URL
```

---

## 🔁 Redirect Handling — The Most Important Concept

Redirect handling is where many beginners make a critical mistake. A redirect is not returning a URL string — it's a specific HTTP response that instructs the browser to navigate elsewhere.

### ❌ The Wrong Way

```java
@GetMapping("/{shortCode}")
public String redirect(@PathVariable String shortCode) {
    return service.getOriginalUrl(shortCode); // just returns text — no redirect
}
```

The browser receives the string `"https://example.com/..."` as plain text and displays it. Nothing redirects.

### ✅ The Right Way

```java
@GetMapping("/{shortCode}")
public ResponseEntity<?> redirect(@PathVariable String shortCode) {
    String originalUrl = service.getOriginalUrl(shortCode);
    return ResponseEntity
            .status(302)
            .location(URI.create(originalUrl))
            .build();
}
```

### Breaking Down the Redirect Response

```java
ResponseEntity
    .status(302)                        // 1. HTTP status 302 = "go somewhere else"
    .location(URI.create(originalUrl))  // 2. Location header = where to go
    .build()                            // 3. No body needed — finalise the response
```

**What the browser receives:**
```http
HTTP/1.1 302 Found
Location: https://example.com/some/very/long/path
```

**What the browser does:**
```
1. Sees status 302 → "I need to go somewhere else"
2. Reads Location header → the destination
3. Fires a new GET request to that URL automatically
4. User lands on the original page
```

### Why `URI.create()` and Not Just a String?

The `.location()` method requires a `URI` object, not a plain `String`. A `URI` is validated as a well-formed address by Java — a raw string could be anything.

```java
URI.create("https://example.com")   // ✅ Validated URI object
"https://example.com"               // ❌ Wrong type — won't compile here
```

### The Rule

```
Redirect  =  HTTP 302 status  +  Location header
```

Without the status code, the browser treats the response as content to display. Without the Location header, the browser has nowhere to go. Both are required.

---

## 🔤 Base62 Short Code Generation

Short codes like `Xy3K9a` are generated using **Base62 encoding**. Understanding why Base62 was chosen — and the difference between random generation and true encoding — makes you a more deliberate developer.

### What is Base62?

Base62 uses exactly 62 characters:

```
a-z  →  26 lowercase letters
A-Z  →  26 uppercase letters
0-9  →  10 digits
─────────────────────────────
Total: 62 characters
```

These characters are all **URL-safe** — no encoding needed, no `%20` or `+` symbols. The result is a short code that is compact, readable, and safe to put directly in a URL.

### Why Base62 Over Alternatives?

| Approach | Example Output | Problem |
|---|---|---|
| Raw UUID | `f47ac10b-58cc-4372` | Too long, has hyphens, ugly |
| UUID truncated | `f47ac10b` | Hex only — wastes character space, still not pretty |
| Base64 | `Xy3K/9a=` | Contains `/`, `+`, `=` — breaks in URLs |
| **Base62** ✅ | `Xy3K9a` | Short, clean, URL-safe, large space |

With 6 Base62 characters: 62⁶ = **56 billion possible codes** — more than enough headroom.

### Random Generation vs True Encoding

There are two distinct approaches, and it's important to know the difference:

**Approach 1 — Random Base62 (what we use now)**
```java
private static final String CHARSET =
    "abcdefghijklmnopqrstuvwxyzABCDEFGHIJKLMNOPQRSTUVWXYZ0123456789";
private static final int CODE_LENGTH = 6;

private String generateShortCode() {
    StringBuilder code = new StringBuilder();
    Random random = new Random();
    for (int i = 0; i < CODE_LENGTH; i++) {
        code.append(CHARSET.charAt(random.nextInt(CHARSET.length())));
    }
    return code.toString();
}
```

Picks 6 characters at random from the 62-character set. Simple, stateless, and works well at small scale. The trade-off: random codes can collide — two calls could theoretically produce the same code. This must be handled (check if code exists, regenerate if so).

---

**Approach 2 — True Base62 Encoding (used in production systems)**

Real systems like Bitly use a counter-based approach: every new URL gets an auto-incremented database ID, which is then encoded into Base62.

```
Database ID:  12345
     ↓
Base62 encode:  12345 ÷ 62 = 199 remainder 7  → character at index 7 = 'h'
                  199 ÷ 62 = 3 remainder 13   → character at index 13 = 'n'
                    3 ÷ 62 = 0 remainder 3    → character at index 3  = 'd'
     ↓
Short code:  "dnh"  (digits reversed for standard encoding)
```

```java
public String encodeBase62(long id) {
    StringBuilder encoded = new StringBuilder();
    while (id > 0) {
        encoded.append(CHARSET.charAt((int)(id % 62)));
        id /= 62;
    }
    return encoded.reverse().toString();
}
```

| | Random Base62 | Counter + Base62 Encoding |
|---|---|---|
| Collision possible | ✅ Yes — must check | ❌ Never — IDs are unique |
| Requires database ID | ❌ No | ✅ Yes |
| Predictable / enumerable | ❌ No | ✅ Yes (sequential) |
| Best for | Early development | Production systems |

> **📌 Why we start with random:** It requires no database, no auto-increment ID, and no encoding logic — just the core concept. Once PostgreSQL is integrated in the next step, migrating to true Base62 encoding is straightforward and worth doing.

---

## 🧠 Key Takeaway

| Concept | One-Line Summary |
|---|---|
| `@RestController` | This class handles HTTP requests and returns data |
| `@Service` | This class contains business logic |
| `@RequestBody` | Converts incoming JSON into a Java object |
| `@PathVariable` | Extracts a segment from the URL path |
| Dependency Injection | Spring creates and connects objects — you declare what you need |
| HTTP Redirect | `302 status` + `Location header` — not a returned string |
| Base62 (random) | Pick 6 characters from a-z A-Z 0-9 — simple, works at small scale |
| Base62 (encoding) | Encode a database ID into Base62 — no collisions, used in production |