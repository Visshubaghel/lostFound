---
title: Lost And Found
emoji: 🔎
colorFrom: indigo
colorTo: purple
sdk: docker
pinned: false
app_port: 7860
---

# AI-Powered Lost & Found System

## 1. Problem Definition
**Objective:** To solve the problem of lost and found items on a campus or community scale by leveraging AI image recognition for automated matching, reducing manual effort and false claims.
*   **Inputs:** Users upload a photo and brief text description of an item they either lost or found.
*   **Processing:** The system uses the Google Gemini Vision API to convert the image and text into a high-dimensional mathematical vector (embedding). 
*   **Outputs:** When a finder uploads an item, the system calculates the Cosine Similarity against all lost items. If the similarity exceeds 85%, a match notification is triggered.
*   **Success Metrics:** Accurate automated matching (cosine similarity > 0.85), secure and concurrent handling of user requests, and a robust persistence layer.

## 2. Java OOP Design & Advanced Features
This project strictly adheres to Object-Oriented Programming (OOP) principles and implements several advanced Java features from the syllabus:

### OOP Principles Demonstrated
*   **Encapsulation:** Model classes like `Item.java` use private fields (e.g., `id`, `embeddingVector`) with public getters/setters to protect data integrity.
*   **Abstraction:** The `DatabaseManager.java` hides the complex details of the MongoDB cluster connection behind a simple `getInstance().getDatabase()` interface.
*   **Polymorphism:** Multiple HTTP handlers (`RegisterItemHandler`, `FoundItemHandler`) implement the `HttpHandler` interface and override the `handle()` method differently based on the route.

### Advanced Java Features (Syllabus Integration)
*   **Generics (`Match<T>`):** The `Match.java` class is implemented using Java Generics (`public class Match<T>`), allowing the match logic to flexibly rank and sort `Item`, `User`, or any future entity type safely.
*   **Collections Framework:** 
    *   `PriorityQueue` is used in `ItemRegistry.java` to automatically sort matches by their similarity score (highest first).
    *   `ConcurrentHashMap` is used for thread-safe in-memory caching.
*   **Multithreading & Concurrency:** `CompletableFuture` and an `ExecutorService` thread pool are used to handle heavy AI processing tasks asynchronously without blocking the main HTTP server threads.
*   **Exception Handling:** Custom exception classes like `ItemNotRegisteredException` and `SimilarityBelowThresholdException` provide precise error tracking and clean HTTP 400/500 responses.
*   **Design Patterns (Singleton):** `DatabaseManager.java` is implemented as a strict Singleton to guarantee only a single database connection pool exists across all threads, preventing memory leaks.

## 3. Integration & Simulated Environment
The system exposes a fully functional REST API architecture to integrate the backend with the frontend UI.
*   **Mock REST APIs & File Exchange:** The frontend (`public/app.js`) simulates integration by sending JSON payloads via `POST /api/register-item`.
*   **Database Integration:** It securely connects via a JDBC-style driver (`mongo-java-driver`) to a live shared database (MongoDB Atlas) to persist registered items and notifications.

## 4. Execution & Testing Instructions

### How to Run the Unit Tests
Comprehensive unit tests have been written using **JUnit 5** to prove algorithmic correctness of the matching logic and object encapsulation.
1. Open a terminal in the project root.
2. Run the test script:
   `.\test.bat`

### How to Run the Application Locally
1. Ensure Java 11 or 17 is installed.
2. Ensure you have the `MONGODB_URI` and `GEMINI_API_KEY` set as environment variables.
3. Compile:
   `javac -cp "lib/*" src/api/*.java src/db/*.java src/exceptions/*.java src/models/*.java src/service/*.java src/utils/*.java -d bin`
4. Run:
   `java -cp "bin;lib/*" api.AppServer`
5. The frontend is accessible via `http://localhost:8080/index.html`.

### Cloud Deployment
This project is fully containerized and currently live on **Hugging Face Spaces**. It uses a custom `Dockerfile` to build the Java source and dynamically link dependencies.
