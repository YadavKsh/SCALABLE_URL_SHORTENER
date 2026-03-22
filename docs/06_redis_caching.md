# ⚡ Step 7 — Redis Integration (Caching Layer)

> Our backend works. Data persists. But every single redirect hits PostgreSQL — a database sitting on disk. For a URL shortener that could handle thousands of clicks per second, that's a bottleneck. This step adds Redis as a caching layer: a blazing-fast middle stop that answers most requests before they ever reach the database.

---

## 📋 Table of Contents

- [The Problem We're Solving](#-the-problem-were-solving)
- [What is Redis?](#-what-is-redis)
- [How the Architecture Changes](#-how-the-architecture-changes)
- [The Cache-Aside Pattern](#-the-cache-aside-pattern)
- [Setting Up Redis](#-setting-up-redis)
- [Connecting Spring Boot to Redis](#-connecting-spring-boot-to-redis)
- [Updating the Service Layer](#-updating-the-service-layer)
- [End-to-End Request Flow](#-end-to-end-request-flow)
- [Verifying It Works](#-verifying-it-works)
- [Important Limitations](#-important-limitations)
- [Key Takeaways](#-key-takeaways)

---

## 🐢 The Problem We're Solving

Right now, every redirect follows this path:

```
User clicks short link
        ↓
Backend receives request
        ↓
PostgreSQL reads from disk  ← this is the slow part
        ↓
Response sent to user
```

**Why is reading from disk slow?**

Imagine you need a phone number. You could either:
- Ask a friend who has it memorised → answer in 1 second
- Find the physical phone book, flip to the right page → answer in 30 seconds

PostgreSQL is the phone book — it stores data on your hard drive (disk). Reading from disk is orders of magnitude slower than reading from memory (RAM).

For a URL shortener, the **redirect is the most frequent operation** by far. Every time someone clicks a link, that's a database read. At scale, this creates a serious problem.

| Operation | Where data lives | Approximate speed |
|---|---|---|
| PostgreSQL read | Hard disk | ~1–10 ms |
| Redis read | RAM | ~0.1–0.3 ms |
| Ratio | | Redis is **10–100× faster** |

---

## 🧠 What is Redis?

**Redis** stands for *Remote Dictionary Server*. The name tells you a lot: it's essentially a giant dictionary (key → value) that lives entirely in your computer's RAM instead of on disk.

```
Redis is just this, but impossibly fast:

"abc123"  →  "https://example.com/very/long/url"
"xy9kp1"  →  "https://another-site.com/page"
"q7mN2z"  →  "https://docs.spring.io/spring-framework/..."
```

**The key properties of Redis:**

| Property | What it means in plain English |
|---|---|
| **In-memory** | Everything lives in RAM — no disk involved, so reads are near-instant |
| **Key-value store** | You store things by name and retrieve them by name, like a locker system |
| **Optional expiry** | You can say "forget this after 10 minutes" — perfect for caches |
| **Not permanent** | If Redis restarts, the data is gone. That's fine — PostgreSQL still has everything |

> **📌 The important distinction:** Redis is a *cache*, not a *database*. It holds a temporary copy of frequently-used data for speed. PostgreSQL is still the permanent home of your data — the *source of truth*. Redis is just the fast shortcut in front of it.

---

## 🧱 How the Architecture Changes

Before Redis, every request went straight to PostgreSQL:

```
Controller  →  Service  →  PostgreSQL
```

After Redis, requests check the cache first:

```
Controller  →  Service  →  Redis  →  (only if not in Redis)  →  PostgreSQL
```

The majority of requests will be answered by Redis and never touch PostgreSQL at all. This is what "reducing database load" means — PostgreSQL does far less work.

---

## 🔄 The Cache-Aside Pattern

The strategy we use is called **Cache-Aside** (also called *Lazy Loading*). It's one of the most common caching patterns in backend development.

The name "Cache-Aside" means: the cache sits *aside* from the main flow. You check it manually — it's not automatic. Here's the decision tree for every redirect request:

```
Request arrives for /abc123
          ↓
   Check Redis for "abc123"
          ↓
    ┌─────┴─────┐
  FOUND       NOT FOUND
(Cache Hit)  (Cache Miss)
    │               │
    │         Query PostgreSQL
    │               │
    │         Store result
    │         in Redis for
    │         next time
    │               │
    └──────┬────────┘
           ↓
    Return original URL
    → HTTP 302 redirect
```

**Cache Hit** = Redis had the answer. Fast, no database involved.

**Cache Miss** = Redis didn't have it (first time this short code was requested). Go to PostgreSQL, get the answer, *then store it in Redis* so next time it's a hit.

Over time, the most frequently-clicked links live in Redis. The database only gets called for rare or brand-new links.

---

## ⚙️ Setting Up Redis

The easiest way to run Redis locally is with Docker — no installation, no configuration files, one command:

```bash
docker run -d --name redis-cache -p 6379:6379 redis:7
```

Breaking this down word by word:

| Part | What it means |
|---|---|
| `docker run` | Start a new container |
| `-d` | Run in the background (detached) — don't block the terminal |
| `--name redis-cache` | Give the container a name so you can refer to it later |
| `-p 6379:6379` | Connect port 6379 on your computer to port 6379 inside the container. 6379 is Redis's default port |
| `redis:7` | Use the official Redis image, version 7 |

### Verify Redis is running

```bash
# Open a terminal inside the Redis container
docker exec -it redis-cache redis-cli

# Type this and press Enter:
PING
```

If Redis responds with `PONG`, it's alive and ready.

```bash
127.0.0.1:6379> PING
PONG
```

Think of `PING` / `PONG` as knocking on a door to check if someone is home.

---

## 🔌 Connecting Spring Boot to Redis

### Step 1 — Add the dependency to `pom.xml`

```xml
<dependency>
    <groupId>org.springframework.boot</groupId>
    <artifactId>spring-boot-starter-data-redis</artifactId>
</dependency>
```

`spring-boot-starter-data-redis` is Spring's pre-packaged bundle for Redis support — it includes the connection library, the template classes you'll use to talk to Redis, and all the auto-configuration.

### Step 2 — Tell Spring where Redis is (`application.properties`)

```properties
# Redis connection
spring.data.redis.host=localhost   # Redis is running on this computer
spring.data.redis.port=6379        # On this port (Redis's default)
```

That's all Spring needs. It will automatically create a Redis connection when the app starts.

---

## 🛠️ Updating the Service Layer

Only the service changes. The controller and repository stay exactly the same.

```java
@Service
public class UrlShortenerService {

    private final UrlRepository repository;
    private final StringRedisTemplate redisTemplate;  // ← new

    public UrlShortenerService(UrlRepository repository,
                               StringRedisTemplate redisTemplate) {
        this.repository = repository;
        this.redisTemplate = redisTemplate;
    }

    public String getOriginalUrl(String shortCode) {

        // ── Step 1: Check Redis first ──────────────────────────────
        String cachedUrl = redisTemplate.opsForValue().get(shortCode);

        if (cachedUrl != null) {
            System.out.println("⚡ Cache HIT — served from Redis: " + shortCode);
            return cachedUrl;  // Redis had it — done, no DB call
        }

        // ── Step 2: Cache miss — go to PostgreSQL ──────────────────
        System.out.println("💾 Cache MISS — fetching from database: " + shortCode);

        String originalUrl = repository.findByShortCode(shortCode)
                .map(Url::getOriginalUrl)
                .orElseThrow(() -> new RuntimeException("Short code not found: " + shortCode));

        // ── Step 3: Store result in Redis for next time ────────────
        redisTemplate.opsForValue().set(shortCode, originalUrl);
        // Optional: set an expiry so stale data doesn't live forever
        // redisTemplate.opsForValue().set(shortCode, originalUrl, 24, TimeUnit.HOURS);

        return originalUrl;
    }

    // shortenUrl() stays unchanged — caching is only needed for reads
    public String shortenUrl(String originalUrl) { /* ... same as before */ }
}
```

### Breaking down the Redis API

**`StringRedisTemplate`**
This is Spring's built-in class for talking to Redis when both your keys and values are plain text strings. Since our short codes and URLs are both strings, this is exactly what we need. Think of it as the remote control for Redis.

**`redisTemplate.opsForValue()`**
Redis supports different data structures (strings, lists, sets, etc.). `opsForValue()` says: *"I want to work with simple string values."* It returns an object with `get` and `set` methods.

**`.get(shortCode)`**
Sends the command `GET abc123` to Redis. Returns the value if found, `null` if not. This is the cache lookup.

**`.set(shortCode, originalUrl)`**
Sends the command `SET abc123 "https://example.com/..."` to Redis. Stores the mapping in memory.

**Why only cache the `getOriginalUrl` method and not `shortenUrl`?**
Caching is for *reads* — operations you do over and over. A URL is shortened once but redirected potentially thousands of times. The creation path (`POST /shorten`) hits the database once and that's fine. The redirect path (`GET /{shortCode}`) is the high-frequency read that benefits from caching.

---

## 🔄 End-to-End Request Flow

### First request for a short code (Cache Miss)

```
1. User clicks http://localhost:8080/abc123
         ↓
2. Controller extracts "abc123"
         ↓
3. Service checks Redis: GET abc123
         ↓
4. Redis returns null (never seen this before)
         ↓
5. Service queries PostgreSQL: SELECT WHERE short_code = 'abc123'
         ↓
6. PostgreSQL returns "https://example.com/..."
         ↓
7. Service stores it: Redis SET abc123 → "https://example.com/..."
         ↓
8. HTTP 302 redirect sent to browser
```

### Every request after that (Cache Hit)

```
1. User clicks http://localhost:8080/abc123
         ↓
2. Controller extracts "abc123"
         ↓
3. Service checks Redis: GET abc123
         ↓
4. Redis returns "https://example.com/..." instantly ⚡
         ↓
5. HTTP 302 redirect sent to browser
         (PostgreSQL never involved)
```

---

## 🧪 Verifying It Works

### Watch the console logs

Run the app and make two requests to the same short URL. You should see:

```
First request:
💾 Cache MISS — fetching from database: abc123

Second request:
⚡ Cache HIT — served from Redis: abc123
```

The second request never touched PostgreSQL.

### Check Redis directly

```bash
docker exec -it redis-cache redis-cli

# Look up a short code directly in Redis
GET abc123
```

Expected output:
```
"https://www.google.com"
```

If you see the original URL, Redis stored it successfully after the first request.

```bash
# See all keys currently in Redis
KEYS *
```

Each key is a short code that has been requested at least once.

---

## ⚠️ Important Limitations

**Redis is not a replacement for PostgreSQL.** This is the most important thing to understand.

| | Redis | PostgreSQL |
|---|---|---|
| Speed | ⚡ Near-instant | 🐢 Slower (disk) |
| Persistence | ❌ Lost on restart | ✅ Permanent |
| Role | Speed layer (cache) | Source of truth |
| If it goes down | App falls back to DB | App is broken |

If Redis crashes and restarts, all cached data disappears. That's completely fine — the next request for each short code will be a cache miss, hit PostgreSQL, and repopulate Redis. The system self-heals automatically. **Never store data in Redis that doesn't already exist in PostgreSQL.**

---

## 🧠 Key Takeaways

| Concept | Plain English |
|---|---|
| **Redis** | A dictionary that lives in RAM — reading from it is like recalling something from memory instead of looking it up in a book |
| **Cache** | A fast temporary copy of data you've already fetched once, kept close at hand for next time |
| **Cache Hit** | Redis had the answer — fast path, no database call |
| **Cache Miss** | Redis didn't have it — slow path, go to PostgreSQL, then store in Redis |
| **Cache-Aside pattern** | Check the cache first; on a miss, fetch from DB and populate the cache |
| **`StringRedisTemplate`** | Spring's remote control for Redis, for string keys and values |
| **`opsForValue().get()`** | Ask Redis: "do you have this key?" |
| **`opsForValue().set()`** | Tell Redis: "remember this key-value pair" |
| **Source of truth** | PostgreSQL — the permanent record that Redis merely copies from |

The mental model: Redis is a notepad you keep on your desk with the most-looked-up phone numbers. Your full address book is PostgreSQL. You check the notepad first — if it's there, great. If not, you look it up in the address book and jot it on the notepad for next time.

---