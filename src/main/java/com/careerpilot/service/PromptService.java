package com.careerpilot.service;

import com.careerpilot.model.AiPromptConfig;
import com.careerpilot.repository.AiPromptConfigRepository;
import jakarta.annotation.PostConstruct;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

@Service
public class PromptService {

    public static final String PROMPT_RESUME_ANALYSIS = "RESUME_ANALYSIS";
    public static final String PROMPT_INTERVIEW_QUESTIONS = "INTERVIEW_QUESTIONS";
    public static final String PROMPT_ANSWER_EVALUATION = "ANSWER_EVALUATION";
    public static final String PROMPT_FINAL_REPORT = "INTERVIEW_FINAL_REPORT";

    private final AiPromptConfigRepository promptRepo;

    public PromptService(AiPromptConfigRepository promptRepo) {
        this.promptRepo = promptRepo;
    }

    @PostConstruct
    public void initDefaults() {
        if (promptRepo.count() == 0) {
            resetToDefaults();
        }
    }

    public List<AiPromptConfig> getAllPrompts() {
        return promptRepo.findAll();
    }

    public Optional<AiPromptConfig> getPromptByKey(String key) {
        return promptRepo.findByPromptKey(key);
    }

    public String getTemplate(String key) {
        return promptRepo.findByPromptKey(key)
                .map(AiPromptConfig::getTemplateText)
                .orElse(getDefaultTemplate(key));
    }

    @Transactional
    public AiPromptConfig updatePrompt(String key, String title, String description, String templateText) {
        AiPromptConfig prompt = promptRepo.findByPromptKey(key)
                .orElseGet(() -> new AiPromptConfig(key, title, description, "", templateText));
        prompt.setTitle(title);
        prompt.setDescription(description);
        prompt.setTemplateText(templateText);
        prompt.setUpdatedAt(LocalDateTime.now());
        return promptRepo.save(prompt);
    }

    @Transactional
    public void resetToDefaults() {
        saveOrUpdatePrompt(
                PROMPT_RESUME_ANALYSIS,
                "Resume ATS & Role Suitability Analysis Prompt",
                "Controls how the AI analyzes the candidate's resume, computes the ATS score, detects role suitability, and generates pros, cons, and recommendations.",
                "Available Variables: {role}, {experienceLevel}, {skills}, {resumeText}, {candidateName}",
                """
                You are a Principal Technical Recruiter and Senior Engineering Hiring Lead at a top technology company.
                Analyze the following resume for suitability for the role of '{role}'.
                Candidate Experience Level: {experienceLevel}
                Extracted Skills: {skills}

                Candidate Resume Text:
                \"\"\"
                {resumeText}
                \"\"\"

                Provide a thorough ATS evaluation:
                1. Strengths (Pros): Identify 4-5 standout qualifications, project accomplishments, and technical proficiencies.
                2. Weaknesses (Cons): Identify 4-5 missing areas, lack of quantification, or red flags that could cause ATS filtering or recruiter rejection.
                3. Actionable Improvements: Provide concrete, high-impact suggestions to elevate the resume's ATS score and recruiter appeal.
                4. ATS Score & Sub-scores: Score between 0-100 on Formatting, Skills Match, Impact/Metrics, and Clarity.
                """
        );

        saveOrUpdatePrompt(
                PROMPT_INTERVIEW_QUESTIONS,
                "Mock Interview Questions Generator Prompt",
                "Controls how questions are synthesized for the 5-10 minute mock technical interview based on the detected role and candidate's experience.",
                "Available Variables: {role}, {experienceLevel}, {skills}, {candidateName}",
                """
                You are an Engineering Director conducting a rigorous yet encouraging 5-10 minute mock technical interview for a '{role}' candidate.
                Candidate Level: {experienceLevel}
                Key Skills: {skills}

                Generate 5 curated questions spanning the interview arc:
                - Question 1: System Walkthrough & Project Architecture (Based on skills)
                - Question 2: Core Engineering Principles & Best Practices
                - Question 3: Scalability, Performance & Data Consistency Challenge
                - Question 4: Production Incident Debugging & Troubleshooting
                - Question 5: Behavioral & Engineering Culture (Prioritization, trade-offs)

                Ensure each question provides clear criteria for what an exceptional answer entails.
                """
        );

        saveOrUpdatePrompt(
                PROMPT_ANSWER_EVALUATION,
                "Real-time Interview Answer Evaluation Prompt",
                "Controls how each candidate answer is evaluated during the mock interview, generating instant scores and constructive feedback.",
                "Available Variables: {role}, {category}, {question}, {expectedKeyPoints}, {candidateAnswer}",
                """
                You are an Expert Technical Interviewer evaluating a candidate's answer for the role of '{role}'.
                Category: {category}
                Question Asked: {question}
                Expected Evaluation Rubric: {expectedKeyPoints}

                Candidate's Answer:
                \"\"\"
                {candidateAnswer}
                \"\"\"

                Evaluate the response:
                - Score (0-100) based on technical accuracy, structure (STAR / situation-action-result), and depth.
                - Strengths in their answer.
                - Growth areas: What was omitted, vague, or could be improved.
                - Senior Takeaway: A concise model answer showing how a senior engineer would respond.
                """
        );

        saveOrUpdatePrompt(
                PROMPT_FINAL_REPORT,
                "Post-Interview Scorecard & Final Feedback Prompt",
                "Controls the synthesis of the dual score (Resume ATS + Interview Performance) and final career readiness recommendation.",
                "Available Variables: {role}, {candidateName}, {atsScore}, {interviewScore}, {technicalScore}, {communicationScore}",
                """
                You are the Hiring Committee Chairperson delivering the Final Candidate Performance Assessment for '{candidateName}'.
                Role: {role}
                Resume ATS Score: {atsScore}/100
                Mock Interview Score: {interviewScore}/100
                Technical Knowledge: {technicalScore}/100
                Communication & Clarity: {communicationScore}/100

                Deliver an executive summary of candidate readiness, top 3 technical superpowers, top 3 targeted growth areas, and a personalized 30-day interview prep roadmap.
                """
        );
    }

    private void saveOrUpdatePrompt(String key, String title, String description, String variablesHelp, String templateText) {
        AiPromptConfig prompt = promptRepo.findByPromptKey(key)
                .orElseGet(() -> new AiPromptConfig(key, title, description, variablesHelp, templateText));
        prompt.setTitle(title);
        prompt.setDescription(description);
        prompt.setVariablesHelp(variablesHelp);
        prompt.setTemplateText(templateText);
        prompt.setUpdatedAt(LocalDateTime.now());
        promptRepo.saveAndFlush(prompt);
    }

    private String getDefaultTemplate(String key) {
        return switch (key) {
            case PROMPT_RESUME_ANALYSIS -> "Evaluate resume for {role}. Resume: {resumeText}";
            case PROMPT_INTERVIEW_QUESTIONS -> "Generate 5 interview questions for {role} candidate with skills: {skills}";
            case PROMPT_ANSWER_EVALUATION -> "Evaluate answer for question: {question}. Answer: {candidateAnswer}";
            case PROMPT_FINAL_REPORT -> "Synthesize final scorecard for {role} with score {interviewScore}";
            default -> "AI Prompt Template";
        };
    }
}
