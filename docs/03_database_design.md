# 🗄️ Step 3 — Database Design & Integration

> In-memory storage got the core logic working. Now we make it permanent. This step covers why a database is necessary, how to design the schema, and how to wire PostgreSQL into the Spring Boot application.

---

## 📋 Table of Contents

- [Why We Need a Database](#-why-we-need-a-database)
- [Choosing a Database](#-choosing-a-database)
- [Schema Design](#-schema-design)
- [Setting Up PostgreSQL](#-setting-up-postgresql)
- [Connecting Spring Boot to PostgreSQL](#-connecting-spring-boot-to-postgresql)
- [Replacing In-Memory Storage](#-replacing-in-memory-storage)
- [Testing the Integration](#-testing-the-integration)
- [Key Takeaway](#-key-takeaway)

---

## ❌ Why We Need a Database

The current `HashMap` implementation has fundamental limitations that make it unsuitable beyond local development:

| Problem | Why It Matters |
|---|---|
| **Data lost on restart** | Every server restart wipes all stored URLs — unusable in production |
| **Single instance only** | If two server instances run simultaneously, they have separate maps and can't share data |
| **No querying** | You can't search, filter, sort, or aggregate stored links |
| **No persistence guarantees** | A crash mid-operation leaves no recoverable state |
| **Memory-bound** | The entire dataset lives in RAM — impractical at any real scale |

Replacing `HashMap` with a database solves all of these. The application logic stays exactly the same — only the storage layer changes.

---

## 🤔 Choosing a Database

For a URL shortener, the data model is simple: one table, two key columns. Most relational databases handle this well.

| Database | Type | Best For | Why / Why Not for This Project |
|---|---|---|---|
| **PostgreSQL** ✅ | Relational (SQL) | Production-grade, general purpose | Robust, open source, excellent Spring Boot support — our choice |
| **MySQL** | Relational (SQL) | Web apps, wide hosting support | Solid alternative, slightly fewer advanced features than PostgreSQL |
| **H2** | In-memory / embedded | Local dev & testing only | No separate install needed, but data doesn't persist across restarts |
| **MongoDB** | Document (NoSQL) | Flexible, schema-less data | Overkill for a simple key-value mapping |
| **Redis** | Key-value store | Caching, session storage | Excellent for caching redirects on top of PostgreSQL — covered in a later step |

**We use PostgreSQL** as the primary store. H2 is useful for running tests without a live database — it can be added alongside PostgreSQL without conflict.

---

## 📐 Schema Design

The URL mapping relationship requires just one table. Every column has a specific reason for existing.

```sql
CREATE TABLE url_mappings (
    id           BIGSERIAL     PRIMARY KEY,
    short_code   VARCHAR(10)   NOT NULL UNIQUE,
    original_url TEXT          NOT NULL,
    created_at   TIMESTAMP     NOT NULL DEFAULT NOW(),
    expires_at   TIMESTAMP,
    click_count  BIGINT        NOT NULL DEFAULT 0
);
```

### Column Reference

| Column | Type | Constraints | Purpose |
|---|---|---|---|
| `id` | `BIGSERIAL` | Primary Key | Auto-incrementing surrogate key — internal identifier |
| `short_code` | `VARCHAR(10)` | `NOT NULL`, `UNIQUE` | The short alias (e.g. `abc123`). Unique index enables fast lookup |
| `original_url` | `TEXT` | `NOT NULL` | The full destination URL. `TEXT` is used because URLs have no practical length limit |
| `created_at` | `TIMESTAMP` | `NOT NULL`, `DEFAULT NOW()` | Records when the link was created — useful for auditing and analytics |
| `expires_at` | `TIMESTAMP` | Nullable | Optional expiry — when set, the redirect returns `410 Gone` after this time |
| `click_count` | `BIGINT` | `DEFAULT 0` | Tracks how many times the short link has been used |

### Index Strategy

The `UNIQUE` constraint on `short_code` automatically creates an index. This is the most critical query path in the system — every redirect performs a lookup by `short_code`, so this index must exist.

```sql
-- Already created by the UNIQUE constraint, but explicit for clarity:
CREATE UNIQUE INDEX idx_short_code ON url_mappings (short_code);
```

> **📌 Design Note:** `expires_at` and `click_count` are included now, not later. Adding columns to a large table in production is expensive. It's much cheaper to design for near-future needs upfront than to run schema migrations on a live system.

---

## 🐘 Setting Up PostgreSQL

### Option 1 — Local Installation

**macOS (Homebrew):**
```bash
brew install postgresql@15
brew services start postgresql@15
```

**Ubuntu / Debian:**
```bash
sudo apt update
sudo apt install postgresql postgresql-contrib
sudo systemctl start postgresql
```

**Windows:**
Download the installer from [postgresql.org/download/windows](https://www.postgresql.org/download/windows/).

### Create the Database and User

```bash
# Connect to PostgreSQL as the default superuser
psql -U postgres

# Inside the psql shell:
CREATE DATABASE url_shortener;
CREATE USER url_user WITH PASSWORD 'your_password';
GRANT ALL PRIVILEGES ON DATABASE url_shortener TO url_user;
\q
```

---

### Option 2 — Docker *(Recommended for local dev)*

No installation required. Spin up a clean PostgreSQL instance in one command:

```bash
docker run --name url-shortener-db \
  -e POSTGRES_DB=url_shortener \
  -e POSTGRES_USER=url_user \
  -e POSTGRES_PASSWORD=your_password \
  -p 5432:5432 \
  -d postgres:15
```

Stop and remove when done:
```bash
docker stop url-shortener-db
docker rm url-shortener-db
```

> **📌 Tip:** Docker keeps your local machine clean — no lingering database processes, and you can recreate a fresh instance any time.

---

## 🔌 Connecting Spring Boot to PostgreSQL

### 1. Add Dependencies to `pom.xml`

```xml
<!-- PostgreSQL JDBC Driver -->
<dependency>
    <groupId>org.postgresql</groupId>
    <artifactId>postgresql</artifactId>
    <scope>runtime</scope>
</dependency>

<!-- Spring Data JPA (ORM + repository layer) -->
<dependency>
    <groupId>org.springframework.boot</groupId>
    <artifactId>spring-boot-starter-data-jpa</artifactId>
</dependency>
```

---

### 2. Configure `application.properties`

```properties
# Database connection
spring.datasource.url=jdbc:postgresql://localhost:5432/url_shortener
spring.datasource.username=url_user
spring.datasource.password=your_password
spring.datasource.driver-class-name=org.postgresql.Driver

# JPA / Hibernate settings
spring.jpa.database-platform=org.hibernate.dialect.PostgreSQLDialect
spring.jpa.hibernate.ddl-auto=update
spring.jpa.show-sql=true
spring.jpa.properties.hibernate.format_sql=true
```

> **⚠️ `ddl-auto` options explained:**
>
> | Value | Behaviour | Use When |
> |---|---|---|
> | `create` | Drops and recreates schema on every start | Never in production |
> | `update` | Applies only new changes, preserves data | Development |
> | `validate` | Checks schema matches entities, makes no changes | Staging / production |
> | `none` | Does nothing | When you manage schema manually |

---

### 3. Create the JPA Entity

```java
import jakarta.persistence.*;
import java.time.LocalDateTime;

@Entity
@Table(name = "url_mappings")
public class UrlMapping {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "short_code", nullable = false, unique = true, length = 10)
    private String shortCode;

    @Column(name = "original_url", nullable = false, columnDefinition = "TEXT")
    private String originalUrl;

    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt = LocalDateTime.now();

    @Column(name = "expires_at")
    private LocalDateTime expiresAt;

    @Column(name = "click_count", nullable = false)
    private Long clickCount = 0L;

    // Getters and setters omitted for brevity
}
```

---

### 4. Create the Repository

Spring Data JPA generates the SQL queries automatically — you just declare the method signature:

```java
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.Optional;

public interface UrlMappingRepository extends JpaRepository<UrlMapping, Long> {

    // SELECT * FROM url_mappings WHERE short_code = ?
    Optional<UrlMapping> findByShortCode(String shortCode);

    // SELECT EXISTS (SELECT 1 FROM url_mappings WHERE short_code = ?)
    boolean existsByShortCode(String shortCode);
}
```

---

## 🔄 Replacing In-Memory Storage

The service layer changes minimally — the core logic is identical. Only the storage mechanism is swapped.

**Before (in-memory):**
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
        return urlStore.get(shortCode);
    }
}
```

**After (database-backed):**
```java
@Service
public class UrlShortenerService {

    private final UrlMappingRepository repository;

    public UrlShortenerService(UrlMappingRepository repository) {
        this.repository = repository;
    }

    public String shortenUrl(String originalUrl) {
        String shortCode = generateUniqueShortCode();

        UrlMapping mapping = new UrlMapping();
        mapping.setShortCode(shortCode);
        mapping.setOriginalUrl(originalUrl);

        repository.save(mapping);
        return "http://localhost:8080/" + shortCode;
    }

    public String getOriginalUrl(String shortCode) {
        return repository.findByShortCode(shortCode)
                .map(UrlMapping::getOriginalUrl)
                .orElseThrow(() -> new RuntimeException("Short code not found: " + shortCode));
    }

    private String generateUniqueShortCode() {
        String code;
        do {
            code = UUID.randomUUID().toString().substring(0, 6);
        } while (repository.existsByShortCode(code)); // ensure uniqueness
        return code;
    }
}
```

> **📌 Notice:** The controller doesn't change at all. The API contract (`POST /shorten`, `GET /{shortCode}`) remains identical. This is the benefit of structuring the service layer properly — the storage implementation is an internal detail.

---

## 🧪 Testing the Integration

Once configured, verify the full flow manually before writing automated tests.

**1. Start the application and check the logs:**
```
Hibernate: create table url_mappings (...)
Started BackendApplication in 3.2 seconds
```

**2. Create a short URL via Postman or curl:**
```bash
curl -X POST http://localhost:8080/shorten \
  -H "Content-Type: application/json" \
  -d '{"url": "https://example.com/some/very/long/path"}'
```

Expected response:
```json
{
  "shortUrl": "http://localhost:8080/abc123",
  "shortCode": "abc123"
}
```

**3. Verify the record exists in PostgreSQL:**
```sql
SELECT * FROM url_mappings;
```

**4. Test the redirect:**
```bash
curl -v http://localhost:8080/abc123
# Should return: HTTP/1.1 302 Found, Location: https://example.com/...
```

**5. Restart the application and repeat step 4 — the redirect should still work.**
This is the critical test: data now survives a restart.

---

## 🧠 Key Takeaway

Replacing `HashMap` with PostgreSQL does not change what the system does — it changes how reliably it does it.

| Aspect | HashMap | PostgreSQL |
|---|---|---|
| Survives restart | ❌ | ✅ |
| Shared across instances | ❌ | ✅ |
| Queryable | ❌ | ✅ |
| Supports analytics | ❌ | ✅ |
| Setup required | None | Moderate |

The application logic — generate a code, store a mapping, look it up, redirect — is identical in both versions. That separation between *what* the system does and *where* it stores data is a foundational design principle. Get it right at this stage and adding a cache layer (Redis) on top becomes straightforward in the next step.