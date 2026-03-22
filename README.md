# 🔗 URL Shortener

> A backend service that converts long URLs into short, shareable links and redirects users to the original destination — similar to [Bitly](https://bitly.com) or [TinyURL](https://tinyurl.com).

🌐 **Live Demo:** [https://url-shortener-spring.netlify.app](https://url-shortener-spring.netlify.app)

---

## 📋 Table of Contents

- [What Is a URL Shortener?](#-what-is-a-url-shortener)
- [Why Build One?](#-why-build-one)
- [How the System Works](#-how-the-system-works)
  - [Phase 1 — URL Shortening](#phase-1--url-shortening-creation)
  - [Phase 2 — Redirection](#phase-2--redirection-usage)
  - [The Core Mapping](#-the-core-mapping)
- [Tech Stack](#-tech-stack)
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
                  https://url-shortener-spring.netlify.app/abc123
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
Short URL returned to user:  https://url-shortener-spring.netlify.app/abc123
```

**Short code generation** is a key design decision. Common approaches:

| Strategy | How It Works | Trade-off |
|---|---|---|
| Random alphanumeric | `UUID` truncated or `Base62` encoded | Simple, but no guaranteed uniqueness without a check |
| Hash-based | MD5 / SHA of the original URL, take first N chars | Deterministic — same URL always yields same code |
| Counter-based | Auto-increment ID encoded in Base62 | Predictable, sequential — easy to enumerate |

This project uses **random Base62** (6 characters = 56 billion possible codes).

---

### Phase 2 — Redirection (Usage)

This phase runs every time someone opens the short link.

```
User visits https://url-shortener-spring.netlify.app/abc123 in browser
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

`302` is used here so every redirect hits the server, enabling future analytics and link expiry.

---

### 🔑 The Core Mapping

Both phases share one central concept:

```
shortCode  ──────────────────►  originalUrl
 "abc123"                        "https://example.com/..."
```

- The **shortening phase** writes this mapping
- The **redirection phase** reads this mapping

Every other feature in this project (persistence, caching, analytics) is an optimisation layered on top of this single relationship.

---

## 🛠️ Tech Stack

| Layer | Technology |
|---|---|
| **Frontend** | HTML, Tailwind CSS, Vanilla JavaScript |
| **Backend** | Java 21, Spring Boot 3.x |
| **Database** | PostgreSQL (Render) |
| **Cache** | Redis (Upstash) |
| **Containerisation** | Docker |
| **Frontend Hosting** | Netlify |
| **Backend Hosting** | Render |

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
  "shortUrl": "https://your-backend.onrender.com/abc123",
  "shortCode": "abc123",
  "originalUrl": "https://example.com/some/very/long/url"
}
```

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

> **⚠️ Important concept:** The browser address bar always fires a **GET** request. POST requests must be triggered programmatically — via a form submission, JavaScript `fetch`, or an API tool. This is why shortening (which creates data) uses `POST` and redirection (which only reads data) uses `GET`.

---

## 🗄️ Storage Strategy

All three storage phases are fully implemented and running in production.

---

### Phase 1 — In-Memory ✅ (Development only)

Used during early development to validate the core logic without any infrastructure:

```java
Map<String, String> urlStore = new HashMap<>();
```

Replaced by PostgreSQL once the logic was confirmed working. Not used in production.

---

### Phase 2 — PostgreSQL ✅ (Production)

All URL mappings are stored permanently in a managed PostgreSQL instance on Render:

```sql
CREATE TABLE url_mappings (
    id          SERIAL PRIMARY KEY,
    short_code  VARCHAR(10)  UNIQUE NOT NULL,
    original_url TEXT        NOT NULL,
    created_at  TIMESTAMP    DEFAULT NOW()
);
```

Data survives server restarts, supports deduplication (same URL always returns the same short code), and serves as the single source of truth for the system.

---

### Phase 3 — Redis Cache ✅ (Production)

Redis (via Upstash) sits in front of PostgreSQL and handles the majority of redirect lookups without touching the database:

```
Request for /abc123
        ↓
Check Redis (Upstash) → HIT  → return immediately ⚡ (sub-millisecond)
                      → MISS → query PostgreSQL → cache result → return
```

On a cache miss, the result is stored in Redis so every subsequent request for the same short code is served from memory. New short codes are also written to Redis immediately on creation, so the very first redirect is already a cache hit.

---

## 🚀 Future Enhancements

| Feature | Description |
|---|---|
| ✏️ Custom short codes | Let users choose their own alias (`/my-link`) |
| 📊 Click analytics | Track visits per short code (count, timestamp, location) |
| ⏳ Link expiration | Short codes that automatically become invalid after a set time |
| 🔒 Rate limiting | Prevent abuse of the `/shorten` endpoint |
| 🔑 User accounts | Private links, dashboards, link management |

---

## 🧠 Key Takeaway

At its core, a URL shortener is:

> A system that stores a mapping from a short identifier to a long URL, then uses that mapping to redirect users.

```
Store:     shortCode → originalUrl
Retrieve:  shortCode → redirect → originalUrl
```

Everything else — the database, the cache, the analytics, the scaling — is built around making this simple operation faster, more reliable, and more useful.