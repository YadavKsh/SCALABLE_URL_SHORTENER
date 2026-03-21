# 🔗 Step 2: Core Functionality Design

## 🎯 Goal

Define how the URL shortener should work before implementing database or scaling.

---

## 🧠 Why Not Start with Database?

Before storing data, we must understand:

* What data to store?
* How it flows through the system?

---

## 🔄 Core Flow

### 🔹 Shorten URL (Creation Flow)

1. User sends long URL
2. System generates short code
3. Store mapping
4. Return short URL

---

### 📊 URL Creation Flow

![URL Creation Flow](images/CREATION_LOGIC.jpg)

---

### 🔹 Redirect (Redirection Flow)

1. User hits short URL
2. System finds original URL
3. Redirect user

---

### 📊 URL Redirection Flow

![URL Redirection Flow](images/REDIRECTION_LOGIC.jpg)

---

## 🧠 Understanding the Flows

### 🔹 URL Shortening (Creation Phase)

* Triggered via:

  ```
  POST /shorten
  ```
* Usually comes from frontend (button click)

#### What happens:

* Long URL is received
* Short code is generated
* Mapping is stored

```
shortCode → original URL
```

* Short URL is returned to the user

---

### 🔹 URL Redirection (Usage Phase)

* Triggered via:

  ```
  GET /{shortCode}
  ```
* Happens when user enters short URL in browser

#### What happens:

* Backend extracts shortCode
* Looks up mapping
* Finds original URL
* Sends HTTP 302 redirect

👉 Browser automatically follows redirect and loads original site

---

## 🧪 Initial Approach

Use in-memory storage:

```java
Map<String, String>
```

---

## 📌 APIs

### POST /shorten

**Input:** Long URL
**Output:** Short URL

---

### GET /{shortCode}

**Behavior:** Redirect to original URL

---

## 🧠 Key Takeaway

The entire system revolves around a simple idea:

> **Mapping a short code to a long URL**

Everything else (database, caching, scaling) is built on top of this core concept.
