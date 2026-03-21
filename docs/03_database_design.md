# 🗄️ Step 3: Database Design (Coming Next)

## 🎯 Goal

Design how URL mappings will be stored persistently using a database.

---

## 🧠 Why Do We Need a Database?

Currently, we are using:

```java
Map<String, String>
```

### ❌ Problem:

* Data is lost when server restarts
* Not scalable
* Not shared across instances

---

## ✅ Solution

Use a database to store mappings:

```text
shortCode → original URL
```

---

## 📌 What Will Be Covered in This Step

In the next phase, we will:

* Install PostgreSQL
* Connect Spring Boot to database
* Create table for URL mappings
* Replace in-memory storage with database

---

## 🧠 Expected Schema (Preview)

We will design a table like:

| Column       | Type      | Description             |
| ------------ | --------- | ----------------------- |
| id           | Long      | Primary Key             |
| short_code   | String    | Unique short identifier |
| original_url | String    | Full long URL           |
| created_at   | Timestamp | Creation time           |

---

## 🧠 Key Takeaway

In-memory storage helps us understand logic.

Database helps us make the system **persistent and scalable**.

---

## ⏭️ Next Step

Implement core logic first, then integrate database.
