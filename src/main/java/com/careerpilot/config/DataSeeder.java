package com.careerpilot.config;

import com.careerpilot.model.Candidate;
import com.careerpilot.model.InterviewQuestion;
import com.careerpilot.model.InterviewSession;
import com.careerpilot.repository.CandidateRepository;
import com.careerpilot.repository.InterviewQuestionRepository;
import com.careerpilot.repository.InterviewSessionRepository;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.boot.CommandLineRunner;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

@Component
public class DataSeeder implements CommandLineRunner {

    private final CandidateRepository candidateRepo;
    private final InterviewSessionRepository sessionRepo;
    private final InterviewQuestionRepository questionRepo;
    private final ObjectMapper objectMapper = new ObjectMapper();

    public DataSeeder(CandidateRepository candidateRepo,
                      InterviewSessionRepository sessionRepo,
                      InterviewQuestionRepository questionRepo) {
        this.candidateRepo = candidateRepo;
        this.sessionRepo = sessionRepo;
        this.questionRepo = questionRepo;
    }

    @Override
    public void run(String... args) throws Exception {
        if (candidateRepo.count() > 0) {
            return;
        }

        // Seed Sample Candidate 1: Full Stack Java Developer
        Candidate c1 = new Candidate();
        c1.setFullName("Aarav Sharma");
        c1.setEmail("aarav.sharma@example.com");
        c1.setPhone("+91 98765 43210");
        c1.setDetectedRole("Full Stack Java Developer");
        c1.setRoleConfidence(94);
        c1.setExperienceLevel("Entry-Level / Fresher (0-1 years)");
        c1.setAtsScore(86);
        c1.setResumeSummary("Strong foundational knowledge in Core Java, Spring Boot microservices, and React.js frontend architecture. Experience with MySQL and RESTful API development.");
        c1.setProsJson(objectMapper.writeValueAsString(List.of(
                "Excellent technical skill coverage for enterprise Java development (Spring Boot, Hibernate, SQL).",
                "Demonstrated project experience implementing JWT security and REST API endpoints.",
                "Clear project bullet points with active engineering action verbs.",
                "Includes active GitHub profile link with verified code repositories."
        )));
        c1.setConsJson(objectMapper.writeValueAsString(List.of(
                "Lacks quantifiable metrics (% latency improvement, query optimization percentages).",
                "Unit testing frameworks (JUnit 5, Mockito) are not explicitly highlighted in project descriptions.",
                "Resume summary is generic and could be more role-targeted."
        )));
        c1.setImprovementSuggestionsJson(objectMapper.writeValueAsString(List.of(
                "Add performance numbers: e.g., 'Optimized Hibernate SQL queries resulting in 40% faster response times'.",
                "List Docker and CI/CD tools to demonstrate modern DevOps readiness.",
                "Highlight automated testing coverage percentage for core microservices."
        )));
        c1.setSkillsFoundJson(objectMapper.writeValueAsString(List.of("Java", "Spring Boot", "React", "MySQL", "Hibernate", "REST API", "Git", "Maven")));
        c1.setMissingKeywordsJson(objectMapper.writeValueAsString(List.of("JUnit", "Docker", "Microservices", "Redis", "Kafka", "AWS")));
        c1.setBreakdownScoresJson(objectMapper.writeValueAsString(Map.of(
                "formatting", 90,
                "skills", 88,
                "impact", 80,
                "readability", 88
        )));
        c1.setExtractedResumeText("Aarav Sharma\nEmail: aarav.sharma@example.com | Phone: +91 9876543210\nLinkedIn: linkedin.com/in/aarav-sharma | GitHub: github.com/aaravsharma\n\nOBJECTIVE:\nPassionate Software Engineering graduate seeking a Full Stack Java Developer position.\n\nTECHNICAL SKILLS:\nLanguages: Java 17, JavaScript, SQL, HTML/CSS\nFrameworks: Spring Boot, Spring Data JPA, Hibernate, React.js\nDatabases: MySQL, PostgreSQL\nTools: Git, Maven, Postman, VS Code, IntelliJ IDEA\n\nPROJECTS:\n1. E-Commerce Microservices Platform\n- Developed robust REST APIs using Spring Boot and Spring Data JPA.\n- Integrated React.js frontend with Redux for seamless cart state management.\n- Configured JWT authentication and role-based access control.\n\n2. Real-Time Task Management Dashboard\n- Built an interactive kanban board using React and Spring Boot WebSockets.\n- Designed normalized relational database schema in MySQL.");
        c1.setResumeFileName("Aarav_Sharma_Resume.pdf");
        c1.setCreatedAt(LocalDateTime.now().minusHours(3));
        c1.setInterviewScore(88);
        c1.setTechnicalScore(90);
        c1.setCommunicationScore(85);
        c1.setProblemSolvingScore(89);
        c1.setInterviewCompleted(true);
        c1 = candidateRepo.save(c1);

        // Seed Interview Session for Candidate 1
        InterviewSession s1 = new InterviewSession();
        s1.setCandidateId(c1.getId());
        s1.setRole(c1.getDetectedRole());
        s1.setStartedAt(LocalDateTime.now().minusHours(2).minusMinutes(50));
        s1.setCompletedAt(LocalDateTime.now().minusHours(2).minusMinutes(42));
        s1.setDurationSeconds(480);
        s1.setStatus("COMPLETED");
        s1.setOverallScore(88);
        s1.setTechnicalScore(90);
        s1.setCommunicationScore(85);
        s1.setProblemSolvingScore(89);
        s1.setConfidenceScore(87);
        s1.setOverallFeedback("Aarav demonstrated impressive technical command over Spring Boot fundamentals and database design. Answered architectural questions with clear reasoning and structured flow.");
        s1.setStrengthsJson(objectMapper.writeValueAsString(List.of(
                "Precise explanation of Spring IoC container and thread safety considerations.",
                "Clear trade-off analysis between monolithic and microservice database transactions.",
                "Confident verbal delivery and structured answers."
        )));
        s1.setImprovementAreasJson(objectMapper.writeValueAsString(List.of(
                "Incorporate more details on distributed caching with Redis.",
                "Practice answering production outage scenarios under time pressure."
        )));
        s1 = sessionRepo.save(s1);

        // Seed Questions for Candidate 1
        createSampleQuestion(s1, 1, "Technical Architecture",
                "Can you walk me through the architecture of a full-stack Java application you have built? Specifically, how did you handle state, authentication, and communication between Spring Boot and your database?",
                "I designed a 3-tier architecture with React on the frontend and Spring Boot on the backend. For authentication, I generated JWT tokens upon login, stored them in secure HttpOnly cookies, and validated them with a custom Spring Security filter. For persistence, Spring Data JPA repositories handled CRUD operations with HikariCP connection pooling.",
                92,
                "Outstanding answer! Clearly covered client-server separation, secure token handling, and connection pooling.");

        createSampleQuestion(s1, 2, "Core Frameworks & Concurrency",
                "In Spring Boot, how does dependency injection work under the hood, and how would you manage thread safety when multiple concurrent requests access a singleton bean?",
                "Spring's IoC container scans classes annotated with @Component, creates bean instances, and injects dependencies via reflection. Because Spring beans are singleton by default, I ensure they are stateless by not storing request-specific data in instance fields, using local method variables instead.",
                88,
                "Strong response! Accurately explained the Singleton bean lifecycle and why statelessness ensures thread safety.");

        createSampleQuestion(s1, 3, "Scalability & System Design",
                "Suppose your application starts receiving 10,000 requests per second, causing database connection pool exhaustion and slow queries. What step-by-step diagnostic and caching strategies would you implement?",
                "First, I would examine HikariCP metrics and slow query logs. I'd add database indexes on frequently queried columns and introduce Redis cache-aside caching for high-read endpoints to reduce DB hits by 80%.",
                85,
                "Great practical thinking. To elevate further, mention read replicas and asynchronous message queues.");

        // Seed Sample Candidate 2: Frontend Web Developer
        Candidate c2 = new Candidate();
        c2.setFullName("Priya Patel");
        c2.setEmail("priya.patel@example.com");
        c2.setPhone("+91 91234 56789");
        c2.setDetectedRole("Frontend Web Developer");
        c2.setRoleConfidence(91);
        c2.setExperienceLevel("Entry-Level / Fresher (0-1 years)");
        c2.setAtsScore(82);
        c2.setResumeSummary("Frontend specialist proficient in modern React, TypeScript, Tailwind CSS, and Next.js. Passionate about web performance, responsive UI design, and accessible client experiences.");
        c2.setProsJson(objectMapper.writeValueAsString(List.of(
                "Modern frontend tech stack (React 18, TypeScript, Next.js, Tailwind CSS).",
                "Strong portfolio of interactive responsive web applications.",
                "Good grasp of state management and component lifecycle."
        )));
        c2.setConsJson(objectMapper.writeValueAsString(List.of(
                "Missing automated frontend testing frameworks (Jest, React Testing Library, Cypress).",
                "Could provide more specifics on Core Web Vitals and Lighthouse performance metrics."
        )));
        c2.setImprovementSuggestionsJson(objectMapper.writeValueAsString(List.of(
                "Add unit tests using React Testing Library to GitHub projects.",
                "Mention Lighthouse score achievements (e.g., 'Achieved 98+ Lighthouse score on desktop')."
        )));
        c2.setSkillsFoundJson(objectMapper.writeValueAsString(List.of("React", "TypeScript", "JavaScript", "HTML5", "CSS3", "Tailwind", "Next.js", "Git")));
        c2.setMissingKeywordsJson(objectMapper.writeValueAsString(List.of("Jest", "Cypress", "GraphQL", "Redux", "Webpack", "Vite")));
        c2.setBreakdownScoresJson(objectMapper.writeValueAsString(Map.of(
                "formatting", 88,
                "skills", 85,
                "impact", 75,
                "readability", 84
        )));
        c2.setExtractedResumeText("Priya Patel | Frontend Web Developer | priya.patel@example.com\nSkills: React, Next.js, TypeScript, Tailwind CSS, JavaScript ES6+\nProjects: Modern SaaS Landing Page, Recipe Discovery Web App.");
        c2.setResumeFileName("Priya_Patel_CV.docx");
        c2.setCreatedAt(LocalDateTime.now().minusDays(1));
        c2.setInterviewCompleted(false);
        candidateRepo.save(c2);
    }

    private void createSampleQuestion(InterviewSession session, int index, String category, String text, String answer, int score, String feedback) {
        InterviewQuestion q = new InterviewQuestion();
        q.setInterviewSession(session);
        q.setQuestionIndex(index);
        q.setCategory(category);
        q.setQuestionText(text);
        q.setExpectedKeyPoints("Key concepts, architecture, design trade-offs.");
        q.setCandidateAnswer(answer);
        q.setAnswerScore(score);
        q.setAiFeedback(feedback);
        q.setAnsweredAt(LocalDateTime.now().minusMinutes(5 * (4 - index)));
        questionRepo.save(q);
    }
}
