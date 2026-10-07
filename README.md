# ⚡ CareerPilot AI - Job Portal & Interview Preparation

> **An AI-powered recruitment readiness platform built with Java 17 and Spring Boot 3 to help students prepare for job interviews, check resume ATS scores, and detect suitable career roles.**

---

## 🌟 Key Highlights & Features

- **No Account Required for Students:** Students and job seekers can immediately use the platform without creating an account or signing in.
- **Instant Role Detection:** Automatically analyzes the candidate's resume, matches technical competencies against 10+ industry tech roles, and identifies their best-fit job track with a confidence match score.
- **ATS Score & Deep Resume Critique:**
  - 0–100 ATS Readiness Score calculated across Formatting, Skill Relevance, Quantifiable Impact, and Readability.
  - Detailed list of **Strengths (Pros)**.
  - Detailed list of **Weaknesses (Cons)** & ATS filtering risks.
  - Actionable improvement suggestions (Google XYZ formula, metrics, portfolio links).
  - Detected technical skills vs. high-priority missing keywords for the target role.
- **5 to 10 Minute AI Mock Interview:**
  - AI technical questions dynamically synthesized based on the detected job role and student profile.
  - **AI Voice Speaking (TTS):** The AI interviewer reads each question out loud using Web Speech Synthesis.
  - **Speech-to-Text Voice Recording:** Students can dictate their answers using their microphone via Web Speech Recognition or type manually.
  - **Webcam Video Preview:** Real-time camera toggle to simulate a live video technical interview.
  - **Countdown Timer:** 5–10 minute real-time clock tracking session progress.
  - **Instant Question Coaching:** Real-time AI evaluation, scoring (0–100), and senior-level feedback for each answer.
- **Dual Scorecard & 30-Day Preparation Plan:**
  - Dual scores: **Resume ATS Score** + **Interview Performance Score**.
  - Combined Career Readiness Index.
  - Detailed sub-scores (Technical Depth, Articulation, Problem Solving, Confidence).
  - Complete question-by-question transcript with candidate answers and AI critiques.
  - One-click **Print / Download PDF Report**.
- **Admin Section (`/admin`):**
  - Protected with secure login credentials (`admin` / `admin123`).
  - View all student sessions and resumes stored in the database.
  - Candidate search & filter, detailed view, deletion, and **Export to CSV**.
  - **Live AI Prompt Management:** Admin can modify AI prompt templates for Resume Analysis, Interview Questions, Answer Evaluation, and Final Reports directly from the UI without changing any Java code or restarting the server.
  - **System Settings:** Switch between Built-in Smart NLP Engine, Google Gemini API, and OpenAI GPT-4o, and configure interview parameters.
- **Impressive & Classic Design:**
  - Executive Navy & Slate aesthetic with glassmorphism, animated SVG score gauges, responsive tables, and typography.

---

## 🛠️ Technology Stack

- **Backend:** Java 17, Spring Boot 3.2.5, Spring MVC, Spring Data JPA, Hibernate ORM
- **Database:** H2 Database (File-based persistent storage at `./data/careerpilotdb`)
- **Document Parsers:** Apache PDFBox 2.0.31 (PDF extraction), Apache POI 5.2.5 (DOCX extraction)
- **Frontend / Templating:** Thymeleaf 3, Modern HTML5, Custom CSS3 (Glassmorphism & animations)
- **Voice & Speech APIs:** Web Speech API (`SpeechSynthesisUtterance` for AI Voice, `webkitSpeechRecognition` for microphone dictation)
- **Build Tool:** Apache Maven 3.9+

---

## 🚀 How to Run Locally

### Prerequisites
- **Java 17** (e.g., Eclipse Adoptium Temurin 17 or OpenJDK 17)
- **Apache Maven 3.8+**

### 1. Build & Run
Open terminal in the project directory and run:

```bash
# Compile and run unit/integration tests
mvn test

# Package the application
mvn package -DskipTests

# Run the Spring Boot application
java -jar target/careerpilot-ai-1.0.0.jar
```

Alternatively, run directly with Spring Boot plugin:
```bash
mvn spring-boot:run
```

### 2. Access the Application
- **Student Homepage:** [http://localhost:8082](http://localhost:8082)
- **Resume Upload Page:** [http://localhost:8082/upload](http://localhost:8082/upload)
- **Admin Panel:** [http://localhost:8082/admin](http://localhost:8082/admin)
  - **Default Username:** `admin`
  - **Default Password:** `admin123`

---

## 🧭 User Flow Walkthrough

```
  [ Home Page ] 
        │
        ▼ (Click "Get Started")
  [ Resume Upload Page ] ──(Upload PDF / Word or Paste Text)
        │
        ▼ (AI Scans Resume)
  [ Role Detection & ATS Analysis ]
     - Detected Job Role (e.g. Full Stack Java Developer)
     - ATS Score (0-100 Gauge)
     - Standout Pros & ATS Cons
     - Actionable Improvement Roadmap
     - Skills Found & Missing Role Keywords
        │
        ▼ (Click "Start AI Mock Interview")
  [ 5-10 Min Interactive AI Mock Interview ]
     - AI Voice reads questions aloud
     - Optional webcam video preview
     - Dictate answer via microphone or type
     - Real-time AI scoring & feedback per question
     - Countdown timer
        │
        ▼ (Finish Interview)
  [ Dual Scorecard & Comprehensive Report ]
     - Resume ATS Score + Interview Technical Score
     - Combined Career Readiness Index
     - Full transcript & AI model answers
     - 30-Day Preparation Roadmap
     - Print / Save as PDF
```

---

## 🔐 Admin Panel Walkthrough (`/admin`)

1. Navigate to `/admin` in your browser.
2. Log in using `admin` / `admin123`.
3. **Dashboard:** Live analytics, average ATS score, total interviews, and recent candidates.
4. **All Users (`/admin/users`):** View all candidates, filter by name/role, inspect detailed resumes, and download CSV export.
5. **AI Prompt Management (`/admin/prompts`):** Edit prompt templates dynamically:
   - `RESUME_ANALYSIS`: Resume ATS & Role Suitability Analysis Prompt
   - `INTERVIEW_QUESTIONS`: Mock Interview Questions Generator
   - `ANSWER_EVALUATION`: Real-time Interview Answer Evaluation
   - `INTERVIEW_FINAL_REPORT`: Post-Interview Scorecard & Feedback
6. **System Settings (`/admin/settings`):** Configure Gemini / OpenAI API keys, interview duration (5 to 10 minutes), and question count.

---

## 🧪 Automated Test Verification

All 8 integration tests pass with 0 failures:
```
[INFO] Running com.careerpilot.CareerPilotApplicationTests
[INFO] Tests run: 8, Failures: 0, Errors: 0, Skipped: 0
[INFO] BUILD SUCCESS
```

---

*Designed and Developed as a Capstone Major Project.*
