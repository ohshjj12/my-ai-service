# My AI Service — Baby Gender Prediction from Ultrasound Images

A Spring Boot 3.4 REST API that accepts ultrasound image uploads and returns a simulated AI-based baby gender prediction with a confidence score.

---

## Tech Stack

| Layer | Technology |
|---|---|
| Language | Java 21 (virtual threads) |
| Framework | Spring Boot 3.4 |
| Security | Spring Security — HTTP Basic Auth |
| Persistence | Spring Data JPA + Hibernate |
| Database | H2 (in-memory) |
| Boilerplate | Lombok |
| Build | Gradle 8.11.1 |

---

## Prerequisites

- Java 21 (`JAVA_HOME=/usr/lib/jvm/temurin-21-jdk-amd64` or equivalent)
- Internet access for Gradle dependency download (first run only)

---

## Build & Run

```bash
# Build (includes tests)
JAVA_HOME=/usr/lib/jvm/temurin-21-jdk-amd64 ./gradlew build

# Run the application
JAVA_HOME=/usr/lib/jvm/temurin-21-jdk-amd64 ./gradlew bootRun
```

The server starts on **http://localhost:8080**.

---

## API Endpoints

### 1. Register a new user

```
POST /api/auth/signup
Content-Type: application/json
```

**Request body:**
```json
{
  "username": "alice",
  "password": "secret123",
  "email": "alice@example.com"
}
```

**Response (201 Created):**
```json
{
  "message": "User registered successfully",
  "username": "alice"
}
```

**curl example:**
```bash
curl -s -X POST http://localhost:8080/api/auth/signup \
  -H "Content-Type: application/json" \
  -d '{"username":"alice","password":"secret123","email":"alice@example.com"}'
```

---

### 2. Analyze an ultrasound image

```
POST /api/predictions/analyze
Authorization: Basic <base64(username:password)>
Content-Type: multipart/form-data
```

**curl example:**
```bash
curl -s -X POST http://localhost:8080/api/predictions/analyze \
  -u alice:secret123 \
  -F "file=@/path/to/ultrasound.jpg"
```

**Response (200 OK):**
```json
{
  "predictionId": 1,
  "imageId": 1,
  "originalFileName": "ultrasound.jpg",
  "predictedGender": "FEMALE",
  "confidenceScore": 0.8734,
  "analyzedAt": "2024-01-15T10:30:00",
  "uploadedBy": "alice"
}
```

> **Note:** The analysis runs on a Java 21 virtual thread and simulates a 3-second AI processing delay.

---

### 3. Get my prediction history

```
GET /api/predictions/my-results
Authorization: Basic <base64(username:password)>
```

**curl example:**
```bash
curl -s http://localhost:8080/api/predictions/my-results \
  -u alice:secret123
```

**Response (200 OK):**
```json
[
  {
    "predictionId": 1,
    "imageId": 1,
    "originalFileName": "ultrasound.jpg",
    "predictedGender": "FEMALE",
    "confidenceScore": 0.8734,
    "analyzedAt": "2024-01-15T10:30:00",
    "uploadedBy": "alice"
  }
]
```

---

## Postman Quick Start

1. **Sign up** — `POST http://localhost:8080/api/auth/signup` with JSON body.
2. **Analyze image** — `POST http://localhost:8080/api/predictions/analyze`
   - Set **Authorization** tab → Type: `Basic Auth`, fill username/password.
   - Set **Body** tab → `form-data`, add key `file` (type: File), select your image.
3. **View history** — `GET http://localhost:8080/api/predictions/my-results` with Basic Auth.

---

## H2 Console

Access the in-memory database browser at:

```
http://localhost:8080/h2-console
```

| Setting | Value |
|---|---|
| JDBC URL | `jdbc:h2:mem:aiservicedb` |
| Username | `sa` |
| Password | *(leave blank)* |

---

## Project Structure

```
src/main/java/com/example/aiservice/
├── AiServiceApplication.java
├── config/
│   ├── PasswordEncoderConfig.java   # BCrypt bean (avoids circular dependency)
│   └── SecurityConfig.java          # HTTP Basic, stateless sessions
├── controller/
│   ├── AuthController.java          # POST /api/auth/signup
│   └── PredictionController.java    # POST /api/predictions/analyze, GET /api/predictions/my-results
├── dto/
│   ├── AuthResponse.java
│   ├── LoginRequest.java
│   ├── PredictionRequest.java
│   ├── PredictionResponse.java
│   └── SignupRequest.java
├── entity/
│   ├── PredictionResult.java
│   ├── UltrasoundImage.java
│   └── User.java
├── repository/
│   ├── PredictionResultRepository.java
│   ├── UltrasoundImageRepository.java
│   └── UserRepository.java
└── service/
    ├── PredictionService.java       # Virtual-thread-based image analysis
    └── UserService.java             # UserDetailsService implementation
```

---

## Notes

- Predictions are **simulated** (random gender + confidence 60–99%). Replace `performPrediction()` in `PredictionService` with a real ML model call.
- The database is **in-memory** and resets on every restart.
- File storage is **not persisted** — only metadata (filename, size, content type) is stored in H2.
