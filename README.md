# 🔗 URL Shortener - Project Overview

## 🎯 What is this Project?

This project is a **URL Shortener service**, similar to platforms like Bitly.

It allows users to:

* Convert long URLs into short, manageable links
* Use the short link to automatically redirect to the original URL

---

## 🧠 Why Do We Need a URL Shortener?

Long URLs can be:

* Hard to share
* Difficult to remember
* Unpleasant to display

Example:

```
https://example.com/some/very/long/url/with/many/parameters
```

Becomes:

```
http://localhost:8080/abc123
```

---

## 🔄 How the System Works

The system works in **two main phases**:

---

## 🔹 1. URL Shortening (Creation Phase)

This happens when a user wants to create a short URL.

### 📌 Flow:

1. User provides a long URL
2. Backend generates a unique short code
3. The system stores a mapping

```
shortCode → original URL
```

4. A short URL is returned to the user

---

### ✅ Example:

Input:

```
https://example.com/long-url
```

Output:

```
http://localhost:8080/abc123
```

---

## 🔹 2. Redirection (Usage Phase)

This happens when a user opens the short URL in a browser.

### 📌 Flow:

1. User visits:

```
http://localhost:8080/abc123
```

2. Backend extracts `abc123`
3. System looks up stored mapping
4. Finds original URL
5. Redirects user to original URL

---

## 🔗 How Both Phases Are Connected

The system revolves around a simple idea:

```
shortCode → original URL
```

* **Shortening phase** creates this mapping
* **Redirection phase** uses this mapping

Without storing this relationship, the system cannot function.

---

## 🌐 How Requests Are Handled

### 🔹 Creating Short URL

* Endpoint:

```
POST /shorten
```

* Triggered by:

    * Frontend (button click)
    * API tools (Postman)

---

### 🔹 Using Short URL

* Endpoint:

```
GET /{shortCode}
```

* Triggered by:

    * Browser address bar

---

## ⚠️ Important Concept

* Browser URL bar always sends **GET requests**
* POST requests are triggered via:

    * Frontend forms
    * JavaScript
    * API tools

---

## 🧪 Initial Implementation Approach

To keep things simple initially:

* We use **in-memory storage**:

```
Map<String, String>
```

Later, this will be replaced with:

* Database (PostgreSQL)
* Cache (Redis)

---

## 🚀 Future Enhancements

* Database persistence
* Redis caching
* Custom short URLs
* Analytics (click tracking)
* Rate limiting
* Scalable architecture

---

## 🧠 Key Takeaway

At its core, a URL shortener is just:

> A system that maps a short identifier to a long URL and redirects users accordingly.

Everything else (database, caching, scaling) is built around optimizing this simple idea.
