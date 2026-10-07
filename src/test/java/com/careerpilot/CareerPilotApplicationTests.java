package com.careerpilot;

import com.careerpilot.model.Candidate;
import com.careerpilot.model.InterviewQuestion;
import com.careerpilot.model.InterviewSession;
import com.careerpilot.repository.CandidateRepository;
import com.careerpilot.service.*;
import com.careerpilot.service.AiService.AnswerEvaluationOutput;
import com.careerpilot.service.RoleDetectionService.RoleDetectionResult;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

@SpringBootTest
class CareerPilotApplicationTests {

    @Autowired
    private RoleDetectionService roleDetectionService;

    @Autowired
    private ResumeParserService resumeParserService;

    @Autowired
    private CandidateService candidateService;

    @Autowired
    private MockInterviewService mockInterviewService;

    @Autowired
    private PromptService promptService;

    @Autowired
    private AdminAuthService adminAuthService;

    @Autowired
    private CandidateRepository candidateRepo;

    @Test
    @DisplayName("Context Loads Successfully")
    void contextLoads() {
        assertNotNull(roleDetectionService);
        assertNotNull(candidateService);
        assertNotNull(mockInterviewService);
    }

    @Test
    @DisplayName("Role Detection matches Full Stack Java Developer")
    void testJavaRoleDetection() {
        String sampleResume = """
                Aarav Sharma
                Email: aarav@example.com
                Skills: Java, Spring Boot, Hibernate, MySQL, REST API, React, Git, Microservices
                Experience: Developed full stack web applications with Spring Boot backend and React UI.
                """;

        RoleDetectionResult result = roleDetectionService.detectRole(sampleResume);
        assertNotNull(result);
        assertEquals("Full Stack Java Developer", result.getBestRole());
        assertTrue(result.getConfidenceScore() >= 70);
        assertTrue(result.getFoundSkills().contains("Java"));
        assertTrue(result.getFoundSkills().contains("Spring Boot"));
    }

    @Test
    @DisplayName("Role Detection matches Frontend Web Developer")
    void testFrontendRoleDetection() {
        String sampleResume = """
                Priya Verma
                Email: priya@frontend.dev
                Technical Proficiencies: React, TypeScript, Next.js, HTML5, CSS3, Tailwind CSS, Redux
                Projects: Developed modern responsive web applications and component design systems.
                """;

        RoleDetectionResult result = roleDetectionService.detectRole(sampleResume);
        assertNotNull(result);
        assertEquals("Frontend Web Developer", result.getBestRole());
        assertTrue(result.getFoundSkills().contains("React"));
        assertTrue(result.getFoundSkills().contains("Typescript"));
    }

    @Test
    @DisplayName("Resume Parsing extracts email, phone, and name")
    void testResumeMetadataExtraction() {
        String text = """
                Rahul Nair
                Email: rahul.nair@example.com | Phone: 9876543210
                Skills: Python, Django, PostgreSQL
                """;

        assertEquals("rahul.nair@example.com", resumeParserService.extractEmail(text));
        assertNotNull(resumeParserService.extractPhone(text));
        assertEquals("Rahul Nair", resumeParserService.extractCandidateName(text, "Fallback"));
    }

    @Test
    @DisplayName("Full Candidate Processing and Database Persistence")
    void testCandidateCreationAndPersistence() throws Exception {
        String resume = """
                Devanshi Gupta
                Email: devanshi.gupta@example.com
                Skills: Python, Machine Learning, Pandas, NumPy, Scikit-Learn, TensorFlow, SQL
                Projects: Built customer churn prediction model with 92% accuracy.
                """;

        Candidate candidate = candidateService.processResumeText(resume, "Devanshi_Resume.txt");
        assertNotNull(candidate.getId());
        assertEquals("Data Scientist & Machine Learning Engineer", candidate.getDetectedRole());
        assertTrue(candidate.getAtsScore() > 60);
        assertNotNull(candidate.getResumeSummary());

        // Verify retrieval from DB
        Candidate retrieved = candidateRepo.findById(candidate.getId()).orElse(null);
        assertNotNull(retrieved);
        assertEquals(candidate.getDetectedRole(), retrieved.getDetectedRole());
    }

    @Test
    @DisplayName("Mock Interview flow: Start session, evaluate answer, finish session")
    void testMockInterviewFlow() throws Exception {
        // Create candidate
        String resume = """
                Sameer Khan
                Email: sameer@example.com
                Skills: Java, Spring Boot, Hibernate, MySQL, REST API
                """;
        Candidate candidate = candidateService.processResumeText(resume, "Sameer_Resume.txt");

        // 1. Start Interview
        InterviewSession session = mockInterviewService.startInterview(candidate.getId());
        assertNotNull(session);
        assertNotNull(session.getId());
        assertFalse(session.getQuestions().isEmpty());
        assertEquals("IN_PROGRESS", session.getStatus());

        // 2. Submit Answer to Question 1
        InterviewQuestion q1 = session.getQuestions().get(0);
        AnswerEvaluationOutput eval = mockInterviewService.submitAnswer(
                q1.getId(),
                "In my Spring Boot application, I structured the architecture into Controller, Service, and Repository layers. I handled state statelessly using JWT tokens validated in a Spring Security filter."
        );
        assertNotNull(eval);
        assertTrue(eval.score >= 50);
        assertNotNull(eval.feedback);

        // 3. Finish Interview
        InterviewSession finished = mockInterviewService.finishInterview(session.getId());
        assertEquals("COMPLETED", finished.getStatus());
        assertNotNull(finished.getOverallScore());
        assertNotNull(finished.getTechnicalScore());

        // Verify candidate updated
        Candidate updatedCandidate = candidateRepo.findById(candidate.getId()).orElseThrow();
        assertTrue(updatedCandidate.getInterviewCompleted());
        assertNotNull(updatedCandidate.getInterviewScore());
    }

    @Test
    @DisplayName("Admin Authentication and Security")
    void testAdminAuthentication() {
        assertTrue(adminAuthService.authenticate("admin", "admin123"));
        assertFalse(adminAuthService.authenticate("admin", "wrongpassword"));
        assertFalse(adminAuthService.authenticate("unknown_user", "admin123"));
    }

    @Test
    @DisplayName("AI Prompts can be customized and reset without code changes")
    void testPromptCustomization() {
        String originalTemplate = promptService.getTemplate(PromptService.PROMPT_RESUME_ANALYSIS);
        assertNotNull(originalTemplate);

        // Update prompt
        String customized = "CUSTOM PROMPT: Analyze resume for role {role}.";
        promptService.updatePrompt(PromptService.PROMPT_RESUME_ANALYSIS, "Custom Title", "Custom Desc", customized);

        assertEquals(customized, promptService.getTemplate(PromptService.PROMPT_RESUME_ANALYSIS));

        // Reset to default
        promptService.resetToDefaults();
        assertNotEquals(customized, promptService.getTemplate(PromptService.PROMPT_RESUME_ANALYSIS));
    }
}
