# 🔗 URL Shortener — Project Overview

> A backend service that converts long URLs into short, shareable links and redirects users to the original destination — similar to [Bitly](https://bitly.com) or [TinyURL](https://tinyurl.com).

---

## 📋 Table of Contents

- [What Is a URL Shortener?](#-what-is-a-url-shortener)
- [Why Build One?](#-why-build-one)
- [How the System Works](#-how-the-system-works)
  - [Phase 1 — URL Shortening](#phase-1--url-shortening-creation)
  - [Phase 2 — Redirection](#phase-2--redirection-usage)
  - [The Core Mapping](#-the-core-mapping)
- [API Design](#-api-design)
- [Storage Strategy](#-storage-strategy)
- [Future Enhancements](#-future-enhancements)
- [Key Takeaway](#-key-takeaway)

---

## 🎯 What Is a URL Shortener?

A URL Shortener takes a long, unwieldy URL and produces a short alias that redirects to the same destination.

```
https://example.com/some/very/long/url/with/many/parameters?ref=campaign&utm_source=email
                              ↓
                  http://localhost:8080/abc123
```

That's the entire job — store a mapping, serve a redirect.

---

## 🤔 Why Build One?

Long URLs are inconvenient in several real-world contexts:

| Problem | Example |
|---|---|
| Hard to share verbally | Impossible to dictate over a phone call |
| Break in emails / documents | Clients truncate long URLs, making them unclickable |
| Ugly in print / marketing | QR codes and print ads need short links |
| Leaks internal path structure | Long URLs can reveal folder names, IDs, query params |

Beyond convenience, building a URL shortener teaches core backend concepts: REST API design, data persistence, redirection mechanics, and scalability patterns — which is exactly what this project is for.

---

## 🔄 How the System Works

The entire system revolves around **two phases** that mirror each other.

---

### Phase 1 — URL Shortening (Creation)

This phase runs once when a user wants to create a short link.

```
User submits long URL
        ↓
Backend generates a unique short code  (e.g. "abc123")
        ↓
System stores the mapping:  "abc123" → "https://example.com/..."
        ↓
Short URL returned to user:  http://localhost:8080/abc123
```

**Short code generation** is a key design decision. Common approaches:

| Strategy | How It Works | Trade-off |
|---|---|---|
| Random alphanumeric | `UUID` truncated or `Base62` encoded | Simple, but no guaranteed uniqueness without a check |
| Hash-based | MD5 / SHA of the original URL, take first N chars | Deterministic — same URL always yields same code |
| Counter-based | Auto-increment ID encoded in Base62 | Predictable, sequential — easy to enumerate |

For this project, we start with **random alphanumeric** (`Base62`, 6 characters = 56 billion possible codes).

---

### Phase 2 — Redirection (Usage)

This phase runs every time someone opens the short link.

```
User visits http://localhost:8080/abc123 in browser
        ↓
Backend extracts the short code: "abc123"
        ↓
Looks up stored mapping → finds "https://example.com/..."
        ↓
Returns HTTP 302 redirect → browser navigates to original URL
```

**Why HTTP 302 and not 301?**

| Status Code | Type | Browser Behaviour |
|---|---|---|
| `301 Moved Permanently` | Permanent | Browser caches the redirect — won't hit your server again |
| `302 Found` | Temporary | Browser always checks your server — lets you update or expire links |

Use `302` during development and when you want analytics (each visit hits your server). Use `301` only if links are truly permanent and you want to offload traffic.

---

### 🔑 The Core Mapping

Both phases share one central concept:

```
shortCode  ──────────────────►  originalUrl
 "abc123"                        "https://example.com/..."
```

- The **shortening phase** writes this mapping
- The **redirection phase** reads this mapping

Every other feature in this project (persistence, caching, analytics) is just an optimisation layered on top of this single relationship.

---

## 🌐 API Design

The service exposes two endpoints.

### `POST /shorten` — Create a Short URL

**Request:**
```http
POST /shorten
Content-Type: application/json

{
  "url": "https://example.com/some/very/long/url"
}
```

**Response:**
```http
HTTP/1.1 200 OK

{
  "shortUrl": "http://localhost:8080/abc123",
  "shortCode": "abc123"
}
```

Triggered by: a frontend form, a button click, or an API client like Postman.

---

### `GET /{shortCode}` — Redirect to Original URL

**Request:**
```http
GET /abc123
```

**Response:**
```http
HTTP/1.1 302 Found
Location: https://example.com/some/very/long/url
```

Triggered by: entering the short URL directly in a browser address bar.

> **⚠️ Important concept:** The browser address bar always fires a **GET** request. POST requests must be triggered programmatically — via a form submission, JavaScript `fetch`, or an API tool. This is why shortening (which creates data) uses `POST` and redirection (which only reads data) uses `GET`.

---

## 🗄️ Storage Strategy

### Phase 1 — In-Memory (Current)

For the initial implementation, mappings are stored in a `HashMap` in application memory:

```java
// Simple, no setup required — perfect for getting the logic right first
Map<String, String> urlStore = new HashMap<>();

// Store:  urlStore.put("abc123", "https://example.com/...");
// Lookup: urlStore.get("abc123");
```

**Limitations:**
- All data is lost when the application restarts
- Cannot scale across multiple server instances
- No persistence, no querying

This is intentional — get the core logic right before introducing infrastructure complexity.

---

### Phase 2 — Database (Planned)

Replace `HashMap` with a PostgreSQL table:

```sql
CREATE TABLE url_mappings (
    id          SERIAL PRIMARY KEY,
    short_code  VARCHAR(10)  UNIQUE NOT NULL,
    original_url TEXT        NOT NULL,
    created_at  TIMESTAMP    DEFAULT NOW()
);
```

Data survives restarts, supports analytics, and works across multiple server instances.

---

### Phase 3 — Cache Layer (Planned)

Add Redis in front of the database for high-traffic short codes:

```
Request for /abc123
        ↓
Check Redis cache → HIT → return immediately (sub-millisecond)
                 → MISS → query PostgreSQL → cache result → return
```

Redirects are read-heavy and the data rarely changes — a perfect use case for caching.

---

## 🚀 Future Enhancements

| Feature | Description |
|---|---|
| 🗃️ Database persistence | PostgreSQL — survive restarts, enable analytics |
| ⚡ Redis caching | Cache hot links for sub-millisecond redirects |
| ✏️ Custom short codes | Let users choose their own alias (`/my-link`) |
| 📊 Click analytics | Track visits per short code (count, timestamp, location) |
| ⏳ Link expiration | Short codes that automatically become invalid after a set time |
| 🔒 Rate limiting | Prevent abuse of the `/shorten` endpoint |
| 🔑 User accounts | Private links, dashboards, link management |
| 🐳 Containerisation | Docker + Docker Compose for portable deployment |

---

## 🧠 Key Takeaway

At its core, a URL shortener is:

> A system that stores a mapping from a short identifier to a long URL, then uses that mapping to redirect users.

```
Store:     shortCode → originalUrl
Retrieve:  shortCode → redirect → originalUrl
```

Everything else — the database, the cache, the analytics, the scaling — is built around making this simple operation faster, more reliable, and more useful. Start with the mapping. Build outward from there.