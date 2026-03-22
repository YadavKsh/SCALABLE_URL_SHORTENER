# 🗄️ Step 6 — Database Integration (PostgreSQL)

> We've been storing URLs in the computer's memory — like writing on a whiteboard. The moment the server restarts, everything disappears. In this step, we plug in a real database so data survives forever, just like saving a file to your hard drive.

---

## 📋 Table of Contents

- [The Problem with Memory Storage](#-the-problem-with-memory-storage)
- [How the Architecture Changes](#-how-the-architecture-changes)
- [Setting Up PostgreSQL](#-setting-up-postgresql)
- [Connecting Spring Boot to PostgreSQL](#-connecting-spring-boot-to-the-database)
- [The Entity — `Url.java`](#-the-entity--urljava)
- [The Repository — `UrlRepository.java`](#-the-repository--urlrepositoryjava)
- [The Service — What Changes](#-the-service--what-changes)
- [End-to-End Flow](#-end-to-end-flow)
- [Verifying It Actually Works](#-verifying-it-actually-works)
- [Common Issues](#-common-issues)
- [Key Takeaways](#-key-takeaways)

---

## ❌ The Problem with Memory Storage

Until now, our service stored data like this:

```java
Map<String, String> urlStore = new HashMap<>();
```

Think of a `HashMap` like a **sticky note on your desk**. The moment you switch off your computer (restart the server), the sticky note is gone. Everything written on it is lost forever.

A database is like a **filing cabinet**. Even if you turn off your computer, close the office, and come back next week — the files are still there exactly where you left them.

| Storage Type | What it's like | Survives restart? |
|---|---|---|
| `HashMap` | Sticky note | ❌ No |
| PostgreSQL | Filing cabinet | ✅ Yes |

---

## 🧱 How the Architecture Changes

Before, the request travelled through three stops:

```
Controller  →  Service  →  HashMap (in memory)
```

After adding a database, there are four stops:

```
Controller  →  Service  →  Repository  →  PostgreSQL (on disk)
```

Two new words here: **Repository** and **PostgreSQL**. Both are explained in detail below — for now, just understand that the Repository is the messenger between your code and the database.

---

## ⚙️ Setting Up PostgreSQL

**PostgreSQL** is the actual database software — it's a program that runs on your computer and stores data in an organised way, like a very advanced Excel. We need to start it and create a "drawer" (database) for our project.

### Step 1 — Start PostgreSQL

```bash
# Windows
net start postgresql

# macOS (if installed via Homebrew)
brew services start postgresql@15

# Linux
sudo systemctl start postgresql
```

This starts the PostgreSQL program running in the background, waiting for our app to connect to it.

---

### Step 2 — Open the PostgreSQL Command Line

```bash
psql -U postgres
```

`psql` is the command-line tool for talking to PostgreSQL. Think of it like opening a chat window directly to the database. `-U postgres` means "log in as the user named `postgres`" (the default admin user).

---

### Step 3 — Create a Database

```sql
CREATE DATABASE url_shortener;
```

A **database** is like a named folder inside PostgreSQL. You can have many databases in one PostgreSQL installation. This command creates a folder called `url_shortener` where all our tables will live.

---

### Step 4 — Switch Into That Database

```sql
\c url_shortener
```

`\c` means "connect to". You're now inside the `url_shortener` folder, and any commands you run will affect this database.

---

## 🔌 Connecting Spring Boot to the Database

Now we need to tell our Spring Boot application **where the database is** and **how to log into it**. We do this in one file.

Open `src/main/resources/application.properties` and add:

```properties
# Where is the database? (localhost = this computer, 5432 = PostgreSQL's default port)
spring.datasource.url=jdbc:postgresql://localhost:5432/url_shortener

# The username and password you use to log into PostgreSQL
spring.datasource.username=postgres
spring.datasource.password=your_password

# Tell Spring to automatically create/update the table structure from our Java classes
spring.jpa.hibernate.ddl-auto=update

# Print every SQL query in the console so we can see what's happening
spring.jpa.show-sql=true
```

**Breaking down `spring.datasource.url`:**

```
jdbc:postgresql://localhost:5432/url_shortener
│    │           │         │    │
│    │           │         │    └── Database name
│    │           │         └─────── Port number (PostgreSQL's default)
│    │           └───────────────── This computer
│    └───────────────────────────── Database type
└────────────────────────────────── Java's standard prefix for DB connections
```

> **⚠️ Remove the exclusion from earlier!**
> In Step 2, we added this to prevent Spring from crashing before we had a database:
> ```java
> @SpringBootApplication(exclude = {DataSourceAutoConfiguration.class})
> ```
> Now that we have a real database configured, **delete that `exclude` part**. Spring Boot should auto-configure the database connection now — that's what we want.

---

## 🏷️ The Entity — `Url.java`

An **Entity** is a Java class that represents a table in the database. Every field in the class becomes a column in the table. Think of it like this:

```
Java class  =  Table in the database
Java field  =  Column in the table
Java object =  One row in the table
```

Here's what ours looks like:

```java
import jakarta.persistence.*;

@Entity                              // "Hey Spring — this class is a database table"
@Table(name = "url_mappings")        // The table will be named "url_mappings" in PostgreSQL
public class Url {

    @Id                                              // This field is the Primary Key
    @GeneratedValue(strategy = GenerationType.IDENTITY)  // Auto-increment (1, 2, 3...)
    private Long id;

    @Column(unique = true, nullable = false, length = 10)
    private String shortCode;                        // e.g. "abc123"

    @Column(columnDefinition = "TEXT", nullable = false)
    private String originalUrl;                      // e.g. "https://example.com/..."

    // Getters and setters
    public Long getId() { return id; }
    public String getShortCode() { return shortCode; }
    public void setShortCode(String shortCode) { this.shortCode = shortCode; }
    public String getOriginalUrl() { return originalUrl; }
    public void setOriginalUrl(String originalUrl) { this.originalUrl = originalUrl; }
}
```

### What each annotation does

**`@Entity`**
Tells Spring: *"This Java class is not just a regular class — it maps to a database table."* Without this, Spring ignores the class completely when setting up the database.

**`@Table(name = "url_mappings")`**
Tells Spring what to name the table in PostgreSQL. If you skip this, Spring names the table after your class (so it would be called `url`). Being explicit is cleaner.

**`@Id`**
Every table needs a **Primary Key** — a column that uniquely identifies each row, like a student ID number. This annotation marks which field is that unique ID.

**`@GeneratedValue(strategy = GenerationType.IDENTITY)`**
You don't want to manually assign ID numbers like 1, 2, 3... That would be tedious and error-prone. This tells the database: *"Every time a new row is inserted, automatically give it the next number."* So the first URL gets `id = 1`, the second gets `id = 2`, and so on.

**`@Column(unique = true)`**
Means no two rows can have the same value in this column. Since two different URLs can't share the same short code, this prevents duplicates at the database level.

**`@Column(columnDefinition = "TEXT")`**
By default, PostgreSQL stores strings as `VARCHAR(255)` — meaning max 255 characters. URLs can be much longer than that. `TEXT` removes that limit entirely.

**What the table looks like in PostgreSQL after this:**
```
 id │ short_code │ original_url
────┼────────────┼──────────────────────────────────
  1 │ abc123     │ https://example.com/very/long/url
  2 │ xy9kp1     │ https://another-site.com/page
```

---

## 🗂️ The Repository — `UrlRepository.java`

A **Repository** is the part of the code that talks to the database. Instead of writing raw SQL queries like `SELECT * FROM url_mappings WHERE short_code = 'abc123'`, you write a simple Java method and Spring writes the SQL for you automatically.

Think of the Repository as a **librarian**. You say: *"Find me the book with this title."* The librarian knows exactly which shelf to look on and brings it back. You don't need to know how the library is organised.

```java
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.Optional;

public interface UrlRepository extends JpaRepository<Url, Long> {

    // Spring reads the method name and generates:
    // SELECT * FROM url_mappings WHERE short_code = ?
    Optional<Url> findByShortCode(String shortCode);

    // SELECT * FROM url_mappings WHERE original_url = ?
    Optional<Url> findByOriginalUrl(String originalUrl);
}
```

### Breaking down the keywords

**`interface`**
An `interface` in Java is like a **job description**. It describes *what* needs to be done, but doesn't say *how* to do it. Spring Boot reads this interface and automatically provides the implementation — the actual code that runs the SQL queries. You get full database access without writing a single line of query logic.

**`extends JpaRepository<Url, Long>`**
`extends` means: *"inherit everything from this parent."*

`JpaRepository` is a ready-made interface from Spring that already knows how to do common database operations — save a record, delete a record, find all records, find by ID, and so on. By extending it, your `UrlRepository` gets all of those operations for free.

The `<Url, Long>` part tells it: *"We're working with `Url` objects, and the ID type is `Long`."*

Free operations you inherit automatically:
```java
repository.save(url);           // INSERT or UPDATE
repository.findById(1L);        // SELECT WHERE id = 1
repository.findAll();           // SELECT * FROM url_mappings
repository.delete(url);         // DELETE
repository.count();             // SELECT COUNT(*)
```

**`Optional<Url>`**
`Optional` is a wrapper that honestly communicates: *"This might return a result, or it might return nothing."*

Without `Optional`, if no short code is found, you'd get `null` — and accidentally using `null` without checking crashes your program with a `NullPointerException`. `Optional` forces you to handle the "not found" case explicitly:

```java
Optional<Url> result = repository.findByShortCode("abc123");

if (result.isPresent()) {
    String originalUrl = result.get().getOriginalUrl();
    // redirect to originalUrl
} else {
    // return 404
}

// Or more elegantly:
String originalUrl = repository.findByShortCode("abc123")
    .orElseThrow(() -> new RuntimeException("Short code not found"));
```

**`findByShortCode(String shortCode)`**
This is Spring Data's **method naming magic**. Spring reads the method name word-by-word:
- `find` → SELECT
- `By` → WHERE
- `ShortCode` → the field named `shortCode`

The result: Spring automatically generates `SELECT * FROM url_mappings WHERE short_code = ?` with zero SQL from you. The same works for `findByOriginalUrl`, `findByIdAndShortCode`, `findAllByOrderByCreatedAtDesc`, and so on — Spring can parse almost any combination you write.

---

## 🔄 The Service — What Changes

The service layer replaces its `HashMap` with the `UrlRepository`. The logic stays identical — only the storage mechanism changes.

```java
@Service
public class UrlShortenerService {

    // Before: private final Map<String, String> urlStore = new HashMap<>();
    // After:
    private final UrlRepository repository;

    // Spring automatically injects the repository (Dependency Injection)
    public UrlShortenerService(UrlRepository repository) {
        this.repository = repository;
    }

    public String shortenUrl(String originalUrl) {

        // Check if this URL was already shortened — avoid duplicates
        Optional<Url> existing = repository.findByOriginalUrl(originalUrl);
        if (existing.isPresent()) {
            return "http://localhost:8080/" + existing.get().getShortCode();
        }

        // Generate a new short code and save it
        String shortCode = generateUniqueShortCode();

        Url url = new Url();
        url.setShortCode(shortCode);
        url.setOriginalUrl(originalUrl);

        repository.save(url);   // ← INSERT INTO url_mappings (...) VALUES (...)

        return "http://localhost:8080/" + shortCode;
    }

    public String getOriginalUrl(String shortCode) {
        return repository.findByShortCode(shortCode)   // ← SELECT WHERE short_code = ?
                .map(Url::getOriginalUrl)
                .orElseThrow(() -> new RuntimeException("Short code not found: " + shortCode));
    }

    private String generateUniqueShortCode() {
        String charset = "abcdefghijklmnopqrstuvwxyzABCDEFGHIJKLMNOPQRSTUVWXYZ0123456789";
        String code;
        Random random = new Random();
        do {
            StringBuilder sb = new StringBuilder();
            for (int i = 0; i < 6; i++) {
                sb.append(charset.charAt(random.nextInt(charset.length())));
            }
            code = sb.toString();
        } while (repository.existsByShortCode(code)); // keep trying until truly unique
        return code;
    }
}
```

> **📌 Notice:** The controller doesn't change at all. The API (`POST /shorten`, `GET /{shortCode}`) stays identical. This is the reward for good layered architecture — swapping the storage layer is an internal detail that nothing outside the service needs to know about.

---

## 🔄 End-to-End Flow

### `POST /shorten` — Creating a Short URL

```
1. Client sends POST /shorten  {"url": "https://example.com/..."}
         ↓
2. Controller receives request, calls service.shortenUrl()
         ↓
3. Service checks: does this URL already exist in DB?
         ↓
4. If not → generates short code → calls repository.save()
         ↓
5. Repository runs: INSERT INTO url_mappings (short_code, original_url) VALUES (...)
         ↓
6. PostgreSQL writes the row to disk
         ↓
7. Short URL returned to client:  { "shortUrl": "http://localhost:8080/abc123" }
```

### `GET /{shortCode}` — Redirecting

```
1. Browser visits http://localhost:8080/abc123
         ↓
2. Controller extracts "abc123", calls service.getOriginalUrl()
         ↓
3. Service calls repository.findByShortCode("abc123")
         ↓
4. Repository runs: SELECT * FROM url_mappings WHERE short_code = 'abc123'
         ↓
5. PostgreSQL finds the row, returns original_url
         ↓
6. Controller returns HTTP 302 + Location: https://example.com/...
         ↓
7. Browser follows redirect → user lands on the original page
```

---

## 🧪 Verifying It Actually Works

There's one test that proves everything is working correctly, and it's the most important test you can run.

### The Restart Test — The Real Proof

```
1. Start the server
2. POST /shorten with a URL → get back a short code (e.g. /abc123)
3. Stop the server completely
4. Start the server again
5. Open http://localhost:8080/abc123 in the browser
```

If it still redirects → the data survived the restart → **persistence is working** ✅

If it returns 404 → the data was lost → something is still in memory, not in the database ❌

---

### Direct Database Inspection

You can also look directly inside PostgreSQL to confirm the row is there:

```sql
-- Connect first: psql -U postgres -d url_shortener
SELECT * FROM url_mappings;
```

Expected output:
```
 id │ short_code │ original_url
────┼────────────┼──────────────────────────────────
  1 │ abc123     │ https://example.com/very/long/url
```

If you see your data here, PostgreSQL received and stored it correctly.

---

## ⚠️ Common Issues

| Symptom | Likely Cause | Fix |
|---|---|---|
| `Connection refused` on startup | PostgreSQL is not running | Run `net start postgresql` (Windows) or `brew services start postgresql` (Mac) |
| `password authentication failed` | Wrong password in `application.properties` | Double-check `spring.datasource.password` |
| `Table "url_mappings" does not exist` | `ddl-auto` not set | Add `spring.jpa.hibernate.ddl-auto=update` to properties |
| `No qualifying bean of type UrlRepository` | `@Entity` or package structure wrong | Make sure `Url.java` has `@Entity` and is in the right package |
| `404` after restart | `exclude = DataSourceAutoConfiguration` still present | Remove that line from `@SpringBootApplication` |

---

## 🧠 Key Takeaways

| Concept | What it is in plain English |
|---|---|
| **PostgreSQL** | A program that stores your data on disk — it survives restarts |
| **`@Entity`** | "This Java class = a table in the database" |
| **`@Id`** | The unique ID column — every row has one |
| **`@GeneratedValue`** | "Auto-number each row — don't make me do it manually" |
| **`@Column`** | Fine-tune how a field is stored (size, uniqueness, nullability) |
| **Repository** | The messenger between your code and the database |
| **`JpaRepository`** | A pre-built repository with save, find, delete etc. already written |
| **`Optional<T>`** | "This might exist or might not — check before using" |
| **`findByShortCode()`** | Spring reads the method name and writes the SQL for you |
| **`ddl-auto=update`** | Spring automatically creates/updates tables from your `@Entity` classes |

The core idea hasn't changed — the system still maps `shortCode → originalUrl`. The only difference is *where* that map lives: previously a sticky note in RAM, now a filing cabinet on disk.