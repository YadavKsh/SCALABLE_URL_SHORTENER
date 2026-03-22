# 🌐 Code Walkthrough — `UrlShortenerController.java`

> The Controller is the front door of your application. Every HTTP request that arrives — whether from a browser, the frontend, or Postman — enters here first. This document explains every single line so you understand not just *what* it does, but *why* it's written that way.

---

## 📋 Table of Contents

- [Recommended Docs Folder Structure](#-recommended-docs-folder-structure)
- [The Full File at a Glance](#-the-full-file-at-a-glance)
- [Package Declaration](#1-package-declaration)
- [Import Statements](#2-import-statements)
- [Class-Level Annotations](#3-class-level-annotations)
- [The Constructor — Dependency Injection](#4-the-constructor--dependency-injection)
- [POST /shorten — Creating a Short URL](#5-post-shorten--creating-a-short-url)
- [GET /{shortCode} — Redirecting](#6-get-shortcode--redirecting)
- [The URL Validator](#7-the-url-validator--isvalidurl)
- [How All the Pieces Connect](#-how-all-the-pieces-connect)

---

## 📄 The Full File at a Glance

```java
package com.urlshortener.backend.controller;

import com.urlshortener.backend.service.UrlShortenerService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.net.URI;
import java.util.Map;

@CrossOrigin(origins = "*")
@RestController
public class UrlShortenerController {

    private final UrlShortenerService service;

    public UrlShortenerController(UrlShortenerService service) {
        this.service = service;
    }

    @PostMapping("/shorten")
    public ResponseEntity<?> shortenUrl(@RequestBody Map<String, String> request) {
        String originalUrl = request.get("url");

        if (originalUrl == null || originalUrl.isEmpty() || !isValidUrl(originalUrl)) {
            return ResponseEntity.badRequest().body(Map.of("error", "Invalid URL"));
        }

        String shortCode = service.shortenUrl(originalUrl);
        String shortUrl = "http://localhost:8081/" + shortCode;

        return ResponseEntity.ok(Map.of(
                "shortUrl", shortUrl,
                "shortCode", shortCode,
                "originalUrl", originalUrl
        ));
    }

    @GetMapping("/{shortCode}")
    public ResponseEntity<?> redirect(@PathVariable String shortCode) {
        String originalUrl = service.getOriginalUrl(shortCode);

        if (originalUrl == null) {
            return ResponseEntity.notFound().build();
        }

        return ResponseEntity
                .status(302)
                .location(URI.create(originalUrl))
                .build();
    }

    private boolean isValidUrl(String url) {
        try {
            new java.net.URL(url).toURI();
            return true;
        } catch (Exception e) {
            return false;
        }
    }
}
```

---

## 1. Package Declaration

```java
package com.urlshortener.backend.controller;
```

**What it is:** The very first line of every Java file. It declares which folder this file belongs to — like a postal address for your class.

**Why it matters:** Java uses packages to organise files and prevent naming conflicts. `com.urlshortener.backend.controller` maps directly to the folder path `src/main/java/com/urlshortener/backend/controller/` on your file system.

**The naming convention:** Packages are written in reverse domain order. `com.urlshortener` is the company/project identifier, `backend` is the module, and `controller` is the layer. This is a universal Java convention — every professional project follows it.

Spring Boot also uses this package path to know where to scan for classes. Your main application class lives at `com.urlshortener.backend`, so Spring scans everything inside that package and all sub-packages (`controller`, `service`, `repository`, etc.) automatically.

---

## 2. Import Statements

```java
import com.urlshortener.backend.service.UrlShortenerService;
```
Your own class. This tells Java: *"I need to use `UrlShortenerService`, which lives in the `service` sub-package of this project."* Without this, Java wouldn't know where to find it.

---

```java
import org.springframework.http.ResponseEntity;
```
`ResponseEntity` is a Spring class that represents a **complete HTTP response** — status code, headers, and body all in one object. It's how you send back structured responses rather than just raw strings.

---

```java
import org.springframework.web.bind.annotation.*;
```
The `*` (wildcard) imports everything from this package at once. This covers all the mapping annotations you use in this file: `@RestController`, `@PostMapping`, `@GetMapping`, `@RequestBody`, `@PathVariable`, and `@CrossOrigin`. Writing them one by one would be verbose; the wildcard is standard practice when using multiple annotations from the same package.

---

```java
import java.net.URI;
```
`URI` (Uniform Resource Identifier) is a Java class that represents a web address in a structured, type-safe way. You need it when building the redirect response — `.location()` requires a `URI` object, not a plain `String`.

---

```java
import java.util.Map;
```
`Map` is a Java data structure that stores key-value pairs — like a dictionary. You use it here to read the incoming JSON request body (`{"url": "..."}`) and to build the JSON response body (`{"shortUrl": "...", "shortCode": "..."}`).

---

## 3. Class-Level Annotations

```java
@CrossOrigin(origins = "*")
```
**What it does:** Enables **CORS** — Cross-Origin Resource Sharing.

**Why you need it:** Browsers have a built-in security rule: JavaScript running on one domain (e.g. `localhost:3000` — your frontend) is blocked from making requests to a different domain (e.g. `localhost:8081` — your backend). This is called the **Same-Origin Policy**.

`@CrossOrigin(origins = "*")` tells Spring to add a response header that instructs the browser: *"It's okay — allow requests from any origin."*

`"*"` means **all** origins are allowed. In production, you'd replace this with your actual frontend URL (e.g. `"https://yourdomain.com"`) so only your frontend can talk to your backend.

```
Without @CrossOrigin:
Browser says: "I'm on localhost:3000. This response came from localhost:8081. BLOCKED."

With @CrossOrigin(origins = "*"):
Browser says: "The server said it's fine. ALLOWED."
```

---

```java
@RestController
```
Combines two annotations into one:
- `@Controller` — marks this class as a Spring MVC controller (Spring will route HTTP requests to it)
- `@ResponseBody` — tells Spring to write the return value of every method directly into the HTTP response body as JSON, instead of treating it as a view name

Without `@RestController`, Spring would try to find an HTML template called `"Invalid URL"` when you return that string — not what you want. With it, every return value is automatically serialised to JSON.

---

```java
public class UrlShortenerController {
```
A standard Java class declaration. `public` means it's accessible from anywhere in the project. The class name matches the file name — this is mandatory in Java.

---

## 4. The Constructor — Dependency Injection

```java
private final UrlShortenerService service;
```
Declares a field to hold a reference to the service layer. `private` — only this class can access it. `final` — once set in the constructor, it can never be reassigned. This is a good practice: it makes the dependency explicit and prevents accidental reassignment.

---

```java
public UrlShortenerController(UrlShortenerService service) {
    this.service = service;
}
```
This is **Constructor Injection** — the recommended way to wire dependencies in Spring Boot.

When Spring starts up, it sees that `UrlShortenerController` needs a `UrlShortenerService`. It looks in its container, finds the `@Service`-annotated `UrlShortenerService` bean it already created, and passes it into this constructor automatically. You never write `new UrlShortenerService()` anywhere.

`this.service = service` — `this.service` refers to the field declared above. `service` (without `this`) refers to the constructor parameter. This line assigns the injected object to the field.

**Why `final` + constructor injection is better than `@Autowired` on the field:**
```java
// ❌ Field injection — works but harder to test and hides dependencies
@Autowired
private UrlShortenerService service;

// ✅ Constructor injection — explicit, testable, Spring-recommended
private final UrlShortenerService service;
public UrlShortenerController(UrlShortenerService service) {
    this.service = service;
}
```
With constructor injection, you can create the controller in a unit test by simply passing a mock service object — no Spring context required.

---

## 5. `POST /shorten` — Creating a Short URL

### The method signature

```java
@PostMapping("/shorten")
```
Tells Spring: *"When an HTTP POST request arrives at the path `/shorten`, run this method."*

POST is used here because this operation **creates** data — it stores a new short code in the database. The HTTP convention is: GET for reading, POST for creating.

---

```java
public ResponseEntity<?> shortenUrl(@RequestBody Map<String, String> request) {
```

`ResponseEntity<?>` — the return type. `ResponseEntity` lets you control the full HTTP response (status code + body). The `<?>` is a **wildcard generic** — it means *"this response might contain any type of object in the body."* You use `<?>` here because sometimes you return a success map, other times an error map — two different types from the same method.

`@RequestBody` — tells Spring: *"Take the JSON body of the incoming HTTP request and convert it into the next parameter."*

`Map<String, String> request` — the JSON body `{"url": "https://example.com/..."}` becomes a `Map` where `"url"` is the key and the URL string is the value.

```
Incoming HTTP request body:     Java parameter after @RequestBody:
{                         →     Map<String, String> request
  "url": "https://..."            request.get("url") = "https://..."
}
```

---

### Extracting the URL

```java
String originalUrl = request.get("url");
```
Pulls the value associated with the key `"url"` out of the map. If the request body was `{"url": "https://google.com"}`, then `originalUrl` is now `"https://google.com"`. If someone sent `{"link": "https://google.com"}` instead (wrong key), `originalUrl` would be `null`.

---

### Input Validation

```java
if (originalUrl == null || originalUrl.isEmpty() || !isValidUrl(originalUrl)) {
    return ResponseEntity.badRequest().body(Map.of("error", "Invalid URL"));
}
```

Three conditions checked in order:

**`originalUrl == null`** — the `"url"` key was missing from the request body entirely. `request.get("url")` returned `null`.

**`originalUrl.isEmpty()`** — the key was present but the value was an empty string: `{"url": ""}`. An empty string would pass the `null` check but is still not a valid URL.

**`!isValidUrl(originalUrl)`** — the URL is present and non-empty, but isn't a valid web address (e.g. `"not-a-url"` or `"hello"`). The `!` means NOT — if `isValidUrl` returns `false`, this condition is `true` and we reject it.

`ResponseEntity.badRequest()` — creates a response with HTTP status `400 Bad Request`. This is the correct status code when the client sent something malformed.

`.body(Map.of("error", "Invalid URL"))` — attaches a JSON body to the 400 response: `{"error": "Invalid URL"}`. `Map.of()` is a shorthand for creating a small, fixed map — here with one entry.

The response the client receives:
```http
HTTP/1.1 400 Bad Request
Content-Type: application/json

{"error": "Invalid URL"}
```

---

### Calling the Service

```java
String shortCode = service.shortenUrl(originalUrl);
```
Delegates the actual work to the service layer. The controller doesn't generate short codes or touch the database — it just calls the service and gets back the result. This is the rule: **controllers route, services decide**.

`shortenUrl()` returns just the short code string (e.g. `"abc123"`), not the full URL. The controller builds the full short URL itself in the next line.

---

### Building the Short URL

```java
String shortUrl = "http://localhost:8081/" + shortCode;
```
Concatenates the base URL with the short code to produce the full clickable link: `"http://localhost:8081/abc123"`.

The port is `8081` here — matching the port configured in `application.properties`. In production, this would be your actual domain (e.g. `"https://short.yourdomain.com/" + shortCode`).

---

### Building the Success Response

```java
return ResponseEntity.ok(Map.of(
        "shortUrl", shortUrl,
        "shortCode", shortCode,
        "originalUrl", originalUrl
));
```

`ResponseEntity.ok()` — creates a response with HTTP status `200 OK`, meaning everything worked.

`Map.of("shortUrl", shortUrl, "shortCode", shortCode, "originalUrl", originalUrl)` — builds a map with three entries. Spring serialises this map to JSON automatically.

The client receives:
```http
HTTP/1.1 200 OK
Content-Type: application/json

{
  "shortUrl": "http://localhost:8081/abc123",
  "shortCode": "abc123",
  "originalUrl": "https://example.com/very/long/url"
}
```

Returning all three fields (not just the short URL) is good API design — the frontend can display the original URL alongside the short one without making another request.

---

## 6. `GET /{shortCode}` — Redirecting

### The method signature

```java
@GetMapping("/{shortCode}")
```
Maps HTTP GET requests to this method. The `{shortCode}` in curly braces is a **path variable placeholder** — it matches any value at that position in the URL.

```
GET /abc123   →  shortCode = "abc123"
GET /xy9kp1   →  shortCode = "xy9kp1"
GET /q7mN2z   →  shortCode = "q7mN2z"
```

---

```java
public ResponseEntity<?> redirect(@PathVariable String shortCode) {
```
`@PathVariable` — tells Spring to extract the value from the URL path (the part that matched `{shortCode}`) and assign it to the `shortCode` parameter. The name in the annotation and the variable name must match the placeholder name in `@GetMapping`.

---

### Looking Up the Original URL

```java
String originalUrl = service.getOriginalUrl(shortCode);
```
Calls the service which checks Redis first, then PostgreSQL if not cached. Returns the original long URL, or `null` if the short code doesn't exist.

---

### Handling Not Found

```java
if (originalUrl == null) {
    return ResponseEntity.notFound().build();
}
```
`ResponseEntity.notFound()` — creates a response with HTTP status `404 Not Found`. This is the correct status when the requested resource (the short code) doesn't exist in the system.

`.build()` — finalises the response with no body. A 404 doesn't need a body here — though in a more polished version you might add `{"error": "Short code not found"}`.

The client receives:
```http
HTTP/1.1 404 Not Found
```

---

### Sending the Redirect

```java
return ResponseEntity
        .status(302)
        .location(URI.create(originalUrl))
        .build();
```

This is the core of the entire URL shortener. Let's go line by line:

**`.status(302)`** — sets the HTTP response status to `302 Found`. This is a redirect status code. It tells the browser: *"The thing you're looking for isn't here — go to a different URL."*

**`.location(URI.create(originalUrl))`** — adds a `Location` header to the response. This header contains the URL the browser should navigate to. `URI.create(originalUrl)` converts the plain string into a `URI` object, which is what `.location()` requires.

**`.build()`** — finalises the response. Redirect responses have no body — the `Location` header carries all the information needed.

The browser receives:
```http
HTTP/1.1 302 Found
Location: https://example.com/very/long/url
```

The browser then fires a brand new GET request to that `Location` URL automatically. The user lands on the original page without ever seeing the redirect happen.

```
User types:  http://localhost:8081/abc123
                         ↓
           Backend sends:  302 + Location: https://example.com/...
                         ↓
           Browser follows:  GET https://example.com/...
                         ↓
           User sees:  the original website
```

---

## 7. The URL Validator — `isValidUrl()`

```java
private boolean isValidUrl(String url) {
    try {
        new java.net.URL(url).toURI();
        return true;
    } catch (Exception e) {
        return false;
    }
}
```

**`private`** — this method is only used inside this class. It's a helper, not part of the public API.

**`boolean`** — returns either `true` (valid URL) or `false` (invalid URL).

**`new java.net.URL(url)`** — tries to create a `URL` object from the string. Java's `URL` class parses the string and checks whether it conforms to a valid URL structure (has a protocol like `http://`, a host, etc.). If it's malformed, this throws an exception.

**`.toURI()`** — converts the `URL` to a `URI`. This additional step catches edge cases that `URL` alone might accept but that are still technically invalid URIs (e.g. URLs with illegal characters in certain positions).

**`return true`** — if both steps completed without throwing, the string is a valid URL.

**`catch (Exception e)`** — if *either* step threw an exception, the string is not a valid URL.

**`return false`** — signals invalidity back to the calling code, which then returns the 400 error.

**Why `java.net.URL` is written with the full package path:**
`URL` is already imported indirectly, but writing `java.net.URL` explicitly makes it clear exactly which class is being used. There's also a `java.net.URI` already imported — using the full path avoids any potential confusion between the two.

**What this validates and doesn't:**

| Input | Result |
|---|---|
| `"https://google.com"` | ✅ Valid |
| `"http://localhost:8081/path"` | ✅ Valid |
| `"not-a-url"` | ❌ Invalid — no protocol |
| `"ftp://files.example.com"` | ✅ Valid — `ftp://` is a valid protocol |
| `""` (empty string) | ❌ Invalid — caught by `isEmpty()` before this runs |
| `null` | ❌ Invalid — caught by `== null` before this runs |

> **📌 Note:** This validator checks format only — it does not verify that the URL actually exists or is reachable. A URL like `https://this-domain-definitely-does-not-exist-xyz.com` would pass validation. Checking reachability would require making an actual HTTP request, which adds latency and complexity — overkill for this project.

---

## 🔗 How All the Pieces Connect

```
                    UrlShortenerController
                           │
              ┌────────────┴────────────┐
              │                         │
    POST /shorten                 GET /{shortCode}
    shortenUrl()                  redirect()
              │                         │
              ↓                         ↓
    1. Extract URL from body    1. Extract shortCode from path
    2. Validate format          2. Call service.getOriginalUrl()
    3. Call service.shortenUrl()3. If null → 404
    4. Build short URL string   4. If found → 302 + Location header
    5. Return 200 + JSON body
              │                         │
              └────────────┬────────────┘
                           │
                  UrlShortenerService
                  (business logic lives here —
                   controller just calls it)
```

The controller's job is deliberately narrow: receive the request, validate the input, call the service, return the response. No business logic, no database knowledge. If tomorrow you wanted to change how short codes are generated, or switch from PostgreSQL to MongoDB, this controller file would not need to change at all.