package com.careerpilot.service;

import com.careerpilot.model.InterviewQuestion;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;
import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

@Service
public class AiService {

    private static final Logger log = LoggerFactory.getLogger(AiService.class);
    private final PromptService promptService;
    private final SettingsService settingsService;
    private final ObjectMapper objectMapper = new ObjectMapper();
    private final HttpClient httpClient = HttpClient.newBuilder()
            .connectTimeout(Duration.ofSeconds(15))
            .build();

    public AiService(PromptService promptService, SettingsService settingsService) {
        this.promptService = promptService;
        this.settingsService = settingsService;
    }

    public static class ResumeAnalysisOutput {
        public int atsScore;
        public int formattingScore;
        public int skillsScore;
        public int impactScore;
        public int readabilityScore;
        public String summary;
        public List<String> pros = new ArrayList<>();
        public List<String> cons = new ArrayList<>();
        public List<String> improvements = new ArrayList<>();
    }

    public static class QuestionBlueprint {
        public int index;
        public String category;
        public String questionText;
        public String expectedRubric;

        public QuestionBlueprint(int index, String category, String questionText, String expectedRubric) {
            this.index = index;
            this.category = category;
            this.questionText = questionText;
            this.expectedRubric = expectedRubric;
        }
    }

    public static class AnswerEvaluationOutput {
        public int score;
        public String feedback;
        public String strengths;
        public String improvements;
        public String modelAnswer;
    }

    public static class FinalReportOutput {
        public int overallScore;
        public int technicalScore;
        public int communicationScore;
        public int problemSolvingScore;
        public int confidenceScore;
        public String executiveSummary;
        public List<String> topStrengths = new ArrayList<>();
        public List<String> improvementAreas = new ArrayList<>();
    }

    /**
     * Analyze Resume using active template & AI engine
     */
    public ResumeAnalysisOutput analyzeResume(String role, String experienceLevel, List<String> skills, String resumeText, String candidateName) {
        String template = promptService.getTemplate(PromptService.PROMPT_RESUME_ANALYSIS);
        String filledPrompt = template
                .replace("{role}", role != null ? role : "Software Engineer")
                .replace("{experienceLevel}", experienceLevel != null ? experienceLevel : "Fresher")
                .replace("{skills}", skills != null ? String.join(", ", skills) : "General Tech Skills")
                .replace("{resumeText}", resumeText != null ? resumeText.substring(0, Math.min(resumeText.length(), 4000)) : "")
                .replace("{candidateName}", candidateName != null ? candidateName : "Candidate");

        // Check if external LLM should be queried
        String provider = settingsService.getSetting(SettingsService.KEY_AI_PROVIDER, "BUILTIN");
        String geminiKey = settingsService.getSetting(SettingsService.KEY_GEMINI_API_KEY, "");
        String openaiKey = settingsService.getSetting(SettingsService.KEY_OPENAI_API_KEY, "");

        if ("GEMINI".equalsIgnoreCase(provider) && !geminiKey.isBlank()) {
            try {
                String llmResponse = callGemini(geminiKey, filledPrompt);
                ResumeAnalysisOutput parsed = parseLlmResumeAnalysis(llmResponse, role, skills);
                if (parsed != null) return parsed;
            } catch (Exception e) {
                log.warn("Gemini call failed, falling back to built-in AI engine: {}", e.getMessage());
            }
        } else if ("OPENAI".equalsIgnoreCase(provider) && !openaiKey.isBlank()) {
            try {
                String llmResponse = callOpenAi(openaiKey, filledPrompt);
                ResumeAnalysisOutput parsed = parseLlmResumeAnalysis(llmResponse, role, skills);
                if (parsed != null) return parsed;
            } catch (Exception e) {
                log.warn("OpenAI call failed, falling back to built-in AI engine: {}", e.getMessage());
            }
        }

        // Built-in Smart NLP Engine
        return analyzeResumeWithBuiltInEngine(role, experienceLevel, skills, resumeText, candidateName);
    }

    private ResumeAnalysisOutput analyzeResumeWithBuiltInEngine(String role, String experienceLevel, List<String> skills, String resumeText, String candidateName) {
        ResumeAnalysisOutput out = new ResumeAnalysisOutput();
        String lower = resumeText.toLowerCase();

        // 1. Scoring based on ATS criteria
        int skillsCount = skills != null ? skills.size() : 0;
        int wordCount = resumeText.split("\\s+").length;

        // Metric/Quantifiable occurrences
        int metricCount = 0;
        Matcher metricMatcher = Pattern.compile("(\\d+%|\\$\\d+|\\d+\\+?\\s*(users|clients|requests|ms|seconds|million|thousand|k|x))").matcher(lower);
        while (metricMatcher.find()) metricCount++;

        // Action verbs
        String[] actionVerbs = {"engineered", "architected", "developed", "spearheaded", "designed", "optimized", "streamlined", "deployed", "implemented", "resolved", "collaborated"};
        int verbCount = 0;
        for (String v : actionVerbs) {
            if (lower.contains(v)) verbCount++;
        }

        // Contact info check
        boolean hasEmail = lower.contains("@");
        boolean hasPhone = Pattern.compile("\\d{10}").matcher(resumeText).find();
        boolean hasGithubOrLinkedin = lower.contains("github") || lower.contains("linkedin") || lower.contains("portfolio");
        boolean hasProjects = lower.contains("project") || lower.contains("projects");
        boolean hasEducation = lower.contains("education") || lower.contains("degree") || lower.contains("university") || lower.contains("bachelor") || lower.contains("b.tech");

        // Sub scores
        int formatting = 70;
        if (hasEmail) formatting += 8;
        if (hasPhone) formatting += 7;
        if (hasGithubOrLinkedin) formatting += 10;
        if (hasEducation && hasProjects) formatting += 5;
        out.formattingScore = Math.min(98, formatting);

        int skillsScore = Math.min(95, Math.max(50, 45 + (skillsCount * 8)));
        out.skillsScore = skillsScore;

        int impact = Math.min(95, Math.max(45, 40 + (metricCount * 12) + (verbCount * 5)));
        out.impactScore = impact;

        int readability = 75;
        if (wordCount >= 250 && wordCount <= 750) readability = 92;
        else if (wordCount > 750 && wordCount <= 1200) readability = 84;
        else readability = 68;
        out.readabilityScore = readability;

        // Weighted Overall ATS Score
        out.atsScore = (int) Math.round((out.formattingScore * 0.25) + (out.skillsScore * 0.35) + (out.impactScore * 0.25) + (out.readabilityScore * 0.15));
        out.atsScore = Math.max(55, Math.min(96, out.atsScore));

        // Generate Pros
        out.pros.add("Demonstrates clear technical alignment for the " + role + " role with relevant modern tools.");
        if (skillsCount >= 4) {
            out.pros.add("Strong foundational technical vocabulary featuring: " + String.join(", ", skills.subList(0, Math.min(skills.size(), 4))) + ".");
        } else {
            out.pros.add("Clean structure outlining academic background and hands-on coursework.");
        }
        if (hasProjects) {
            out.pros.add("Includes dedicated project implementation experience demonstrating applied problem-solving.");
        }
        if (hasGithubOrLinkedin) {
            out.pros.add("Provides professional profile links (GitHub/LinkedIn), enabling direct code and credentials verification.");
        }
        if (verbCount >= 3) {
            out.pros.add("Utilizes proactive engineering action verbs to communicate responsibilities and initiatives.");
        }

        // Generate Cons
        if (metricCount < 2) {
            out.cons.add("Lacks quantifiable business or performance impact (e.g., latency reduction %, scale of users, efficiency gains).");
        }
        if (!hasGithubOrLinkedin) {
            out.cons.add("Missing direct hyperlinks to GitHub repositories or active portfolio for recruiters to review code quality.");
        }
        if (skillsCount < 5) {
            out.cons.add("Key industry-standard tools (testing frameworks, CI/CD, cloud deployment) are underrepresented for " + role + ".");
        }
        out.cons.add("Project descriptions focus heavily on tasks rather than technical challenges solved and architectures chosen.");
        if (wordCount < 300) {
            out.cons.add("Resume length is sparse; lacks sufficient depth in technical bullet points and system design details.");
        }

        // Actionable Improvements
        out.improvements.add("Adopt the Google 'XYZ Formula': Accomplished [X] as measured by [Y], by doing [Z] for all project bullet points.");
        out.improvements.add("Incorporate high-priority keywords: CI/CD Pipelines, Unit Testing (JUnit/Jest), Docker Containers, and System Monitoring.");
        out.improvements.add("Include direct live demo URLs and GitHub repository links with clean README files for top 2 projects.");
        out.improvements.add("Tailor your professional summary into a punchy 3-line elevator pitch highlighting your target role: " + role + ".");
        out.improvements.add("Group technical proficiencies into distinct categories: Languages, Frameworks, Cloud & Databases, and Developer Tools.");

        // Summary
        out.summary = "The resume exhibits strong potential for the '" + role + "' track. The applicant demonstrates core competency in " 
                + (skills != null && !skills.isEmpty() ? String.join(", ", skills) : "technical development") 
                + ". With targeted ATS keyword enhancements and quantifiable impact metrics, this resume will stand out to tier-1 enterprise recruiters.";

        return out;
    }

    /**
     * Generate Mock Interview Questions tailored to role
     */
    public List<QuestionBlueprint> generateQuestions(String role, String experienceLevel, List<String> skills, int count) {
        String safeRole = (role != null && !role.isBlank()) ? role : "Full Stack Java Developer";
        List<QuestionBlueprint> list = new ArrayList<>();

        if (safeRole.contains("Java") || safeRole.contains("Full Stack")) {
            list.add(new QuestionBlueprint(1, "Technical Architecture",
                    "Can you walk me through the architecture of a full-stack Java application you have built? Specifically, how did you handle state, authentication, and communication between Spring Boot and your database?",
                    "Looks for RESTful API design, MVC pattern, JWT/session handling, Spring Data JPA/Hibernate, and database indexing."));

            list.add(new QuestionBlueprint(2, "Core Frameworks & Concurrency",
                    "In Spring Boot, how does dependency injection work under the hood, and how would you manage thread safety when multiple concurrent requests access a singleton bean?",
                    "Evaluates understanding of Spring IoC container, ApplicationContext, Bean scopes (Singleton vs Prototype), volatile keyword, and synchronization."));

            list.add(new QuestionBlueprint(3, "Scalability & System Design",
                    "Suppose your application starts receiving 10,000 requests per second, causing database connection pool exhaustion and slow queries. What step-by-step diagnostic and caching strategies would you implement?",
                    "Checks knowledge of connection pooling (HikariCP), Redis caching, read replicas, database query optimization, indexing, and asynchronous execution."));

            list.add(new QuestionBlueprint(4, "Debugging & Resilience",
                    "Tell me about a complex bug, memory leak, or unhandled exception you encountered in Java. How did you isolate the root cause, and what logging or monitoring tools did you use?",
                    "Assesses debugging methodology, stack trace analysis, memory profiling (Heap dump, JConsole), SLF4J/Logback, and graceful error handling."));

            list.add(new QuestionBlueprint(5, "Behavioral & Engineering Collaboration",
                    "Describe a scenario where a project requirement was ambiguous or you disagreed with a teammate's architectural decision. How did you resolve the conflict to deliver on time?",
                    "STAR method response demonstrating communication, active listening, constructive debate, trade-off analysis, and team commitment."));
        } else if (safeRole.contains("Python") || safeRole.contains("Backend")) {
            list.add(new QuestionBlueprint(1, "Technical Architecture",
                    "How do you design scalable RESTful APIs using Python frameworks like FastAPI or Django? How do you organize service layers, serialization, and ORM migrations?",
                    "Looks for clean architecture, Pydantic/DRF serializers, dependency injection, and database schema migrations."));

            list.add(new QuestionBlueprint(2, "Python Internals & Asynchronous Programming",
                    "Explain the Python GIL (Global Interpreter Lock). In what scenarios would you choose asyncio / coroutines over multiprocessing or multi-threading?",
                    "Evaluates understanding of CPU-bound vs IO-bound tasks, GIL implications, event loops, and worker processes (Gunicorn/Celery)."));

            list.add(new QuestionBlueprint(3, "Database Optimization & Caching",
                    "How do you prevent the N+1 query problem in Django ORM or SQLAlchemy, and how do you leverage Redis for caching and session management?",
                    "Checks select_related, prefetch_related, indexing, TTL caching patterns, and cache invalidation."));

            list.add(new QuestionBlueprint(4, "Troubleshooting & Security",
                    "How do you secure Python backend APIs against OWASP Top 10 vulnerabilities like SQL injection, CSRF, and broken authorization?",
                    "Assesses parameterized queries, JWT validation, role-based access control (RBAC), and rate limiting."));

            list.add(new QuestionBlueprint(5, "Behavioral & Prioritization",
                    "When working under tight deadlines, how do you balance writing comprehensive unit tests against shipping features quickly?",
                    "Assesses pragmatism, automated CI pipelines, test coverage strategy, and technical debt management."));
        } else if (safeRole.contains("Frontend") || safeRole.contains("React")) {
            list.add(new QuestionBlueprint(1, "Component Architecture",
                    "Can you walk me through your approach to designing reusable component libraries in React or Next.js, and how you manage global state across the app?",
                    "Looks for props drilling mitigation, Context API vs Redux/Zustand, component modularity, and TypeScript typing."));

            list.add(new QuestionBlueprint(2, "Rendering Performance & Virtual DOM",
                    "How does the React Virtual DOM diffing algorithm work, and what techniques do you use to diagnose and prevent unnecessary component re-renders?",
                    "Checks understanding of keys, useMemo, useCallback, React.memo, Suspense, and React DevTools profiler."));

            list.add(new QuestionBlueprint(3, "Web Vitals & Network Optimization",
                    "How do you optimize Core Web Vitals (LCP, FID/INP, CLS) for a customer-facing web app? What role do code splitting, lazy loading, and SSR play?",
                    "Assesses image optimization, dynamic imports, CDN caching, SSR/SSG with Next.js, and bundle analysis."));

            list.add(new QuestionBlueprint(4, "State & Asynchronous Data Fetching",
                    "How do you handle asynchronous data fetching, client-side caching, race conditions, and optimistic UI updates (e.g. using TanStack Query or SWR)?",
                    "Checks query invalidation, caching strategies, error boundaries, and debounce/throttle techniques."));

            list.add(new QuestionBlueprint(5, "Collaboration & Accessibility",
                    "How do you collaborate with UI/UX designers to translate Figma mockups into accessible, cross-browser, responsive interfaces?",
                    "Assesses semantic HTML, ARIA standards, WCAG compliance, responsive CSS/Tailwind, and design system fidelity."));
        } else if (safeRole.contains("Data") || safeRole.contains("Machine Learning")) {
            list.add(new QuestionBlueprint(1, "Data Pipeline & Feature Engineering",
                    "Walk me through your end-to-end process of cleaning messy datasets, handling missing values, encoding categorical variables, and selecting features for training.",
                    "Checks Pandas, imputation techniques, one-hot vs target encoding, scaling, and feature correlation analysis."));

            list.add(new QuestionBlueprint(2, "Model Evaluation & Bias-Variance Tradeoff",
                    "How do you determine if a model is overfitting vs underfitting? Why is Accuracy alone misleading on imbalanced classification datasets, and what metrics would you track instead?",
                    "Evaluates Precision, Recall, F1-Score, ROC-AUC, PR-AUC, cross-validation, and regularization (L1/L2)."));

            list.add(new QuestionBlueprint(3, "MLOps & Model Deployment",
                    "Once an ML model is trained, how do you serve it in production? How do you monitor for data drift and concept drift over time?",
                    "Assesses Docker containerization, FastAPI serving, MLflow/Weights & Biases, pipeline retraining, and Prometheus monitoring."));

            list.add(new QuestionBlueprint(4, "Algorithm Mechanics",
                    "Explain the mathematical intuition behind Gradient Boosting (XGBoost/LightGBM) or Transformer Attention mechanisms in simple terms.",
                    "Evaluates depth of understanding beyond black-box library usage."));

            list.add(new QuestionBlueprint(5, "Stakeholder Communication",
                    "How do you present complex statistical model results to non-technical business stakeholders to build trust in your predictions?",
                    "Focuses on clarity, storytelling, business ROI, and SHAP/LIME model interpretability."));
        } else {
            // General High-Impact Tech Interview Blueprint
            list.add(new QuestionBlueprint(1, "Project Architecture & Role Overview",
                    "Welcome! To start our interview for the " + safeRole + " position, could you walk me through your proudest technical project, your role in it, and the architecture you designed?",
                    "Looking for clear communication, problem context, architectural choices, and candidate's specific contributions."));

            list.add(new QuestionBlueprint(2, "Core Technical Principles",
                    "In the context of " + safeRole + ", what core engineering standards, design patterns, and best practices do you follow to ensure code quality and maintainability?",
                    "Evaluates SOLID principles, clean code practices, modularity, and automated testing."));

            list.add(new QuestionBlueprint(3, "Scalability & Resilience",
                    "How do you approach designing a system that must handle sudden spikes in traffic, failures in downstream services, and ensure 99.9% availability?",
                    "Assesses circuit breakers, caching, rate limiting, horizontal scaling, and asynchronous messaging."));

            list.add(new QuestionBlueprint(4, "Troubleshooting & Problem Solving",
                    "Can you describe a real-world scenario where something broke in production or your code behaved unexpectedly? How did you systematically locate and resolve the issue?",
                    "Checks root cause analysis, logging, metrics, rollback strategies, and post-mortem mindset."));

            list.add(new QuestionBlueprint(5, "Engineering Culture & Team Dynamics",
                    "Tell me about a time you received constructive criticism on a code review or had to negotiate a deadline with a stakeholder. How did you handle it?",
                    "Looks for emotional intelligence, receptiveness to feedback, effective communication, and collaboration."));
        }

        // Return up to requested count
        return list.subList(0, Math.min(count, list.size()));
    }

    /**
     * Evaluate Candidate Answer in Real Time
     */
    public AnswerEvaluationOutput evaluateAnswer(String role, String category, String question, String expectedKeyPoints, String candidateAnswer) {
        String template = promptService.getTemplate(PromptService.PROMPT_ANSWER_EVALUATION);
        String filledPrompt = template
                .replace("{role}", role != null ? role : "Software Engineer")
                .replace("{category}", category != null ? category : "Technical")
                .replace("{question}", question != null ? question : "")
                .replace("{expectedKeyPoints}", expectedKeyPoints != null ? expectedKeyPoints : "")
                .replace("{candidateAnswer}", candidateAnswer != null ? candidateAnswer : "");

        String provider = settingsService.getSetting(SettingsService.KEY_AI_PROVIDER, "BUILTIN");
        String geminiKey = settingsService.getSetting(SettingsService.KEY_GEMINI_API_KEY, "");
        String openaiKey = settingsService.getSetting(SettingsService.KEY_OPENAI_API_KEY, "");

        if ("GEMINI".equalsIgnoreCase(provider) && !geminiKey.isBlank()) {
            try {
                String llmResponse = callGemini(geminiKey, filledPrompt);
                AnswerEvaluationOutput parsed = parseLlmAnswerEval(llmResponse);
                if (parsed != null) return parsed;
            } catch (Exception e) {
                log.warn("Gemini answer eval failed, falling back: {}", e.getMessage());
            }
        } else if ("OPENAI".equalsIgnoreCase(provider) && !openaiKey.isBlank()) {
            try {
                String llmResponse = callOpenAi(openaiKey, filledPrompt);
                AnswerEvaluationOutput parsed = parseLlmAnswerEval(llmResponse);
                if (parsed != null) return parsed;
            } catch (Exception e) {
                log.warn("OpenAI answer eval failed, falling back: {}", e.getMessage());
            }
        }

        // Built-in Smart NLP Evaluation Engine
        return evaluateAnswerWithBuiltInEngine(role, category, question, expectedKeyPoints, candidateAnswer);
    }

    private AnswerEvaluationOutput evaluateAnswerWithBuiltInEngine(String role, String category, String question, String expectedKeyPoints, String candidateAnswer) {
        AnswerEvaluationOutput out = new AnswerEvaluationOutput();
        if (candidateAnswer == null || candidateAnswer.trim().length() < 10) {
            out.score = 25;
            out.strengths = "Attempted the question.";
            out.improvements = "Your response was too brief. Expand on technical architecture, design choices, and real-world examples.";
            out.modelAnswer = "A strong answer should define the concept, provide an architectural walk-through, and discuss performance trade-offs.";
            out.feedback = "Response lacked technical depth. Try to structure answers using the STAR method (Situation, Task, Action, Result) with specific frameworks.";
            return out;
        }

        String lowerAns = candidateAnswer.toLowerCase();
        int words = candidateAnswer.split("\\s+").length;

        // Keyword matching with question and expected key points
        int matchedKeywords = 0;
        if (expectedKeyPoints != null) {
            for (String kw : expectedKeyPoints.split("[,\\s/]+")) {
                if (kw.length() > 3 && lowerAns.contains(kw.toLowerCase())) {
                    matchedKeywords++;
                }
            }
        }

        // Check for technical vocabulary & structure words
        String[] structureWords = {"because", "specifically", "for example", "architecture", "implemented", "optimized", "trade-off", "performance", "result"};
        int structureHits = 0;
        for (String sw : structureWords) {
            if (lowerAns.contains(sw)) structureHits++;
        }

        // Calculate score
        int baseScore = 55;
        if (words >= 30) baseScore += 10;
        if (words >= 70) baseScore += 10;
        if (words >= 130) baseScore += 5;
        baseScore += Math.min(15, matchedKeywords * 4);
        baseScore += Math.min(10, structureHits * 2);

        out.score = Math.min(96, Math.max(40, baseScore));

        if (out.score >= 80) {
            out.strengths = "Articulate, well-structured response with concrete technical insights and clear rationale.";
            out.improvements = "To make it exceptional, quantify performance impact (e.g., latency numbers, throughput metrics, or team velocity).";
        } else if (out.score >= 65) {
            out.strengths = "Good fundamental grasp of the core concepts and relevant terminology.";
            out.improvements = "Deepen your explanation by discussing edge cases, failure recovery, or alternate architectural designs.";
        } else {
            out.strengths = "Addressed the core topic with clear intent.";
            out.improvements = "Provide deeper technical details, name specific design patterns or libraries, and outline concrete implementation steps.";
        }

        out.modelAnswer = "A lead engineer would frame this with: (1) High-level architectural decision and trade-offs, (2) Concrete framework implementation details, (3) Edge-case resilience and monitoring.";
        out.feedback = out.strengths + " " + out.improvements;
        return out;
    }

    /**
     * Synthesize Final Report
     */
    public FinalReportOutput generateFinalReport(String role, String candidateName, int atsScore, int interviewScore, int techScore, int commScore) {
        FinalReportOutput out = new FinalReportOutput();
        out.overallScore = (int) Math.round((atsScore * 0.4) + (interviewScore * 0.6));
        out.technicalScore = techScore > 0 ? techScore : Math.min(95, interviewScore + 2);
        out.communicationScore = commScore > 0 ? commScore : Math.min(92, interviewScore - 3);
        out.problemSolvingScore = Math.min(96, (out.technicalScore + out.communicationScore) / 2 + 3);
        out.confidenceScore = Math.min(95, Math.max(60, commScore + 4));

        out.topStrengths.add("Technical domain knowledge for " + role + " with clear ability to discuss system architecture.");
        out.topStrengths.add("Strong structured thinking when breaking down complex scenario-based questions.");
        out.topStrengths.add("Clear communication style and enthusiasm for engineering best practices.");

        out.improvementAreas.add("Incorporate more quantifiable metrics when describing previous engineering impact.");
        out.improvementAreas.add("Deepen knowledge of distributed systems patterns (caching strategies, circuit breakers, event-driven queues).");
        out.improvementAreas.add("Practice articulating failure scenarios and post-mortem debugging steps under time constraints.");

        out.executiveSummary = candidateName + " demonstrates solid readiness for the '" + role + "' position. " 
                + "With a Resume ATS Score of " + atsScore + "/100 and an Interview Performance Score of " + interviewScore + "/100, "
                + "the candidate shows strong potential to succeed in technical rounds with focused preparation on system scalability and metrics.";

        return out;
    }

    // --- External LLM HTTP Invocation Helpers ---

    private String callGemini(String apiKey, String prompt) throws Exception {
        String endpoint = "https://generativelanguage.googleapis.com/v1beta/models/gemini-1.5-flash:generateContent?key=" + apiKey;
        Map<String, Object> body = Map.of(
                "contents", List.of(
                        Map.of("parts", List.of(Map.of("text", prompt)))
                )
        );
        String jsonPayload = objectMapper.writeValueAsString(body);

        HttpRequest req = HttpRequest.newBuilder()
                .uri(URI.create(endpoint))
                .header("Content-Type", "application/json")
                .POST(HttpRequest.BodyPublishers.ofString(jsonPayload))
                .timeout(Duration.ofSeconds(20))
                .build();

        HttpResponse<String> resp = httpClient.send(req, HttpResponse.BodyHandlers.ofString());
        if (resp.statusCode() == 200) {
            JsonNode root = objectMapper.readTree(resp.body());
            return root.path("candidates").get(0).path("content").path("parts").get(0).path("text").asText();
        }
        throw new RuntimeException("Gemini API error status: " + resp.statusCode() + " body: " + resp.body());
    }

    private String callOpenAi(String apiKey, String prompt) throws Exception {
        String endpoint = "https://api.openai.com/v1/chat/completions";
        Map<String, Object> body = Map.of(
                "model", "gpt-4o-mini",
                "messages", List.of(
                        Map.of("role", "system", "content", "You are an expert technical interviewer and ATS recruiter."),
                        Map.of("role", "user", "content", prompt)
                )
        );
        String jsonPayload = objectMapper.writeValueAsString(body);

        HttpRequest req = HttpRequest.newBuilder()
                .uri(URI.create(endpoint))
                .header("Content-Type", "application/json")
                .header("Authorization", "Bearer " + apiKey)
                .POST(HttpRequest.BodyPublishers.ofString(jsonPayload))
                .timeout(Duration.ofSeconds(20))
                .build();

        HttpResponse<String> resp = httpClient.send(req, HttpResponse.BodyHandlers.ofString());
        if (resp.statusCode() == 200) {
            JsonNode root = objectMapper.readTree(resp.body());
            return root.path("choices").get(0).path("message").path("content").asText();
        }
        throw new RuntimeException("OpenAI API error status: " + resp.statusCode() + " body: " + resp.body());
    }

    private ResumeAnalysisOutput parseLlmResumeAnalysis(String llmText, String role, List<String> skills) {
        if (llmText == null || llmText.isBlank()) return null;
        ResumeAnalysisOutput out = new ResumeAnalysisOutput();
        out.summary = llmText.length() > 600 ? llmText.substring(0, 600) + "..." : llmText;
        out.atsScore = 82;
        out.formattingScore = 85;
        out.skillsScore = 80;
        out.impactScore = 78;
        out.readabilityScore = 85;

        // Parse bullet points
        String[] lines = llmText.split("\n");
        boolean inPros = false, inCons = false, inImprovements = false;
        for (String line : lines) {
            String trimmed = line.trim();
            String lower = trimmed.toLowerCase();
            if (lower.contains("strength") || lower.contains("pros")) {
                inPros = true; inCons = false; inImprovements = false;
                continue;
            } else if (lower.contains("weakness") || lower.contains("cons") || lower.contains("rejection")) {
                inPros = false; inCons = true; inImprovements = false;
                continue;
            } else if (lower.contains("improvement") || lower.contains("actionable") || lower.contains("recommendation")) {
                inPros = false; inCons = false; inImprovements = true;
                continue;
            }

            if ((trimmed.startsWith("-") || trimmed.startsWith("*") || trimmed.matches("^\\d+\\..*")) && trimmed.length() > 10) {
                String clean = trimmed.replaceAll("^[-*\\d.]+\\s*", "");
                if (inPros && out.pros.size() < 5) out.pros.add(clean);
                else if (inCons && out.cons.size() < 5) out.cons.add(clean);
                else if (inImprovements && out.improvements.size() < 5) out.improvements.add(clean);
            }
        }

        if (out.pros.isEmpty()) out.pros.add("Demonstrates foundational competency for " + role);
        if (out.cons.isEmpty()) out.cons.add("Could benefit from more quantifiable metrics in project descriptions");
        if (out.improvements.isEmpty()) out.improvements.add("Adopt the XYZ formula to quantify results across all resume bullet points");

        return out;
    }

    private AnswerEvaluationOutput parseLlmAnswerEval(String llmText) {
        if (llmText == null || llmText.isBlank()) return null;
        AnswerEvaluationOutput out = new AnswerEvaluationOutput();
        out.score = 80;
        Matcher scoreMatcher = Pattern.compile("(?i)score\\s*[:=]?\\s*(\\d{1,3})").matcher(llmText);
        if (scoreMatcher.find()) {
            try {
                int s = Integer.parseInt(scoreMatcher.group(1));
                if (s >= 0 && s <= 100) out.score = s;
            } catch (Exception ignored) {}
        }
        out.feedback = llmText.length() > 500 ? llmText.substring(0, 500) + "..." : llmText;
        out.strengths = "Good technical understanding demonstrated in response.";
        out.improvements = "Refine structure and elaborate on design trade-offs.";
        out.modelAnswer = "Consider framing your answer around architecture, latency trade-offs, and monitoring.";
        return out;
    }
}
