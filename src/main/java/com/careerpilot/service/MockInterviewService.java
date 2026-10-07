package com.careerpilot.service;

import com.careerpilot.model.Candidate;
import com.careerpilot.model.InterviewQuestion;
import com.careerpilot.model.InterviewSession;
import com.careerpilot.repository.CandidateRepository;
import com.careerpilot.repository.InterviewQuestionRepository;
import com.careerpilot.repository.InterviewSessionRepository;
import com.careerpilot.service.AiService.AnswerEvaluationOutput;
import com.careerpilot.service.AiService.FinalReportOutput;
import com.careerpilot.service.AiService.QuestionBlueprint;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Duration;
import java.time.LocalDateTime;
import java.util.*;

@Service
public class MockInterviewService {

    private final InterviewSessionRepository sessionRepo;
    private final InterviewQuestionRepository questionRepo;
    private final CandidateRepository candidateRepo;
    private final CandidateService candidateService;
    private final AiService aiService;
    private final SettingsService settingsService;
    private final ObjectMapper objectMapper = new ObjectMapper();

    public MockInterviewService(InterviewSessionRepository sessionRepo,
                                InterviewQuestionRepository questionRepo,
                                CandidateRepository candidateRepo,
                                CandidateService candidateService,
                                AiService aiService,
                                SettingsService settingsService) {
        this.sessionRepo = sessionRepo;
        this.questionRepo = questionRepo;
        this.candidateRepo = candidateRepo;
        this.candidateService = candidateService;
        this.aiService = aiService;
        this.settingsService = settingsService;
    }

    @Transactional
    public InterviewSession startInterview(Long candidateId) {
        Candidate candidate = candidateRepo.findById(candidateId)
                .orElseThrow(() -> new IllegalArgumentException("Candidate not found: " + candidateId));

        // Create new interview session
        InterviewSession session = new InterviewSession();
        session.setCandidateId(candidateId);
        session.setRole(candidate.getDetectedRole());
        session.setStartedAt(LocalDateTime.now());
        session.setStatus("IN_PROGRESS");

        InterviewSession savedSession = sessionRepo.save(session);

        // Fetch skills
        List<String> skills = candidateService.deserializeList(candidate.getSkillsFoundJson());

        // Get question count from settings (default 5)
        int questionCount = 5;
        try {
            questionCount = Integer.parseInt(settingsService.getSetting(SettingsService.KEY_QUESTION_COUNT, "5"));
        } catch (Exception ignored) {}

        List<QuestionBlueprint> blueprints = aiService.generateQuestions(
                candidate.getDetectedRole(),
                candidate.getExperienceLevel(),
                skills,
                questionCount
        );

        List<InterviewQuestion> questions = new ArrayList<>();
        for (QuestionBlueprint bp : blueprints) {
            InterviewQuestion q = new InterviewQuestion();
            q.setInterviewSession(savedSession);
            q.setQuestionIndex(bp.index);
            q.setCategory(bp.category);
            q.setQuestionText(bp.questionText);
            q.setExpectedKeyPoints(bp.expectedRubric);
            questions.add(questionRepo.save(q));
        }

        savedSession.setQuestions(questions);
        return savedSession;
    }

    @Transactional
    public AnswerEvaluationOutput submitAnswer(Long questionId, String answerText) {
        InterviewQuestion question = questionRepo.findById(questionId)
                .orElseThrow(() -> new IllegalArgumentException("Question not found: " + questionId));

        InterviewSession session = question.getInterviewSession();
        String role = session.getRole();

        // Evaluate Answer with AI engine
        AnswerEvaluationOutput eval = aiService.evaluateAnswer(
                role,
                question.getCategory(),
                question.getQuestionText(),
                question.getExpectedKeyPoints(),
                answerText
        );

        question.setCandidateAnswer(answerText);
        question.setAnswerScore(eval.score);
        question.setAiFeedback(eval.feedback);
        question.setAnsweredAt(LocalDateTime.now());
        questionRepo.save(question);

        return eval;
    }

    @Transactional
    public InterviewSession finishInterview(Long sessionId) {
        InterviewSession session = sessionRepo.findById(sessionId)
                .orElseThrow(() -> new IllegalArgumentException("Session not found: " + sessionId));

        Candidate candidate = candidateRepo.findById(session.getCandidateId())
                .orElseThrow(() -> new IllegalArgumentException("Candidate not found: " + session.getCandidateId()));

        List<InterviewQuestion> questions = questionRepo.findByInterviewSessionIdOrderByQuestionIndexAsc(sessionId);

        // Compute scores
        int totalScore = 0;
        int answeredCount = 0;
        int techScoreSum = 0;
        int techCount = 0;
        int nonTechSum = 0;
        int nonTechCount = 0;

        for (InterviewQuestion q : questions) {
            if (q.getAnswerScore() != null) {
                totalScore += q.getAnswerScore();
                answeredCount++;

                if ("Technical".equalsIgnoreCase(q.getCategory()) || "Scalability & Resilience".equalsIgnoreCase(q.getCategory())) {
                    techScoreSum += q.getAnswerScore();
                    techCount++;
                } else {
                    nonTechSum += q.getAnswerScore();
                    nonTechCount++;
                }
            }
        }

        int avgScore = answeredCount > 0 ? (totalScore / answeredCount) : 70;
        int techScore = techCount > 0 ? (techScoreSum / techCount) : avgScore;
        int commScore = nonTechCount > 0 ? (nonTechSum / nonTechCount) : Math.max(50, avgScore - 5);

        // Generate AI Final Report
        FinalReportOutput report = aiService.generateFinalReport(
                session.getRole(),
                candidate.getFullName(),
                candidate.getAtsScore(),
                avgScore,
                techScore,
                commScore
        );

        session.setCompletedAt(LocalDateTime.now());
        if (session.getStartedAt() != null) {
            long seconds = Duration.between(session.getStartedAt(), session.getCompletedAt()).getSeconds();
            session.setDurationSeconds((int) seconds);
        }
        session.setStatus("COMPLETED");
        session.setOverallScore(report.overallScore);
        session.setTechnicalScore(report.technicalScore);
        session.setCommunicationScore(report.communicationScore);
        session.setProblemSolvingScore(report.problemSolvingScore);
        session.setConfidenceScore(report.confidenceScore);
        session.setOverallFeedback(report.executiveSummary);

        try {
            session.setStrengthsJson(objectMapper.writeValueAsString(report.topStrengths));
            session.setImprovementAreasJson(objectMapper.writeValueAsString(report.improvementAreas));
        } catch (Exception ignored) {}

        InterviewSession savedSession = sessionRepo.save(session);

        // Update Candidate record
        candidate.setInterviewScore(report.overallScore);
        candidate.setTechnicalScore(report.technicalScore);
        candidate.setCommunicationScore(report.communicationScore);
        candidate.setProblemSolvingScore(report.problemSolvingScore);
        candidate.setInterviewCompleted(true);
        candidateRepo.save(candidate);

        return savedSession;
    }

    public Optional<InterviewSession> getSessionByCandidateId(Long candidateId) {
        return sessionRepo.findFirstByCandidateIdOrderByStartedAtDesc(candidateId);
    }

    public Optional<InterviewSession> getSessionById(Long sessionId) {
        return sessionRepo.findById(sessionId);
    }
}
