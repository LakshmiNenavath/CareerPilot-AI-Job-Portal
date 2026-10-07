package com.careerpilot.controller;

import com.careerpilot.model.Candidate;
import com.careerpilot.model.InterviewQuestion;
import com.careerpilot.model.InterviewSession;
import com.careerpilot.service.AiService.AnswerEvaluationOutput;
import com.careerpilot.service.CandidateService;
import com.careerpilot.service.MockInterviewService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api")
public class ApiController {

    private final CandidateService candidateService;
    private final MockInterviewService mockInterviewService;

    public ApiController(CandidateService candidateService, MockInterviewService mockInterviewService) {
        this.candidateService = candidateService;
        this.mockInterviewService = mockInterviewService;
    }

    @PostMapping("/resume/upload-file")
    public ResponseEntity<?> uploadResumeFile(@RequestParam("file") MultipartFile file) {
        try {
            if (file.isEmpty()) {
                return ResponseEntity.badRequest().body(Map.of("error", "Please select a resume file to upload."));
            }
            Candidate candidate = candidateService.processResumeFile(file);
            return ResponseEntity.ok(Map.of(
                    "success", true,
                    "candidateId", candidate.getId(),
                    "detectedRole", candidate.getDetectedRole(),
                    "atsScore", candidate.getAtsScore(),
                    "redirectUrl", "/analysis/" + candidate.getId()
            ));
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(Map.of("error", "Failed to process resume: " + e.getMessage()));
        }
    }

    @PostMapping("/resume/upload-text")
    public ResponseEntity<?> uploadResumeText(@RequestBody Map<String, String> payload) {
        try {
            String text = payload.get("text");
            String title = payload.getOrDefault("title", "Pasted Resume");
            if (text == null || text.trim().length() < 30) {
                return ResponseEntity.badRequest().body(Map.of("error", "Please provide a valid resume with sufficient text (at least 30 characters)."));
            }
            Candidate candidate = candidateService.processResumeText(text, title);
            return ResponseEntity.ok(Map.of(
                    "success", true,
                    "candidateId", candidate.getId(),
                    "detectedRole", candidate.getDetectedRole(),
                    "atsScore", candidate.getAtsScore(),
                    "redirectUrl", "/analysis/" + candidate.getId()
            ));
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(Map.of("error", "Failed to process resume text: " + e.getMessage()));
        }
    }

    @PostMapping("/interview/start")
    public ResponseEntity<?> startInterview(@RequestBody Map<String, Long> payload) {
        try {
            Long candidateId = payload.get("candidateId");
            if (candidateId == null) {
                return ResponseEntity.badRequest().body(Map.of("error", "candidateId is required"));
            }
            InterviewSession session = mockInterviewService.startInterview(candidateId);
            return ResponseEntity.ok(Map.of(
                    "success", true,
                    "sessionId", session.getId(),
                    "role", session.getRole(),
                    "questionsCount", session.getQuestions().size(),
                    "questions", session.getQuestions()
            ));
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(Map.of("error", e.getMessage()));
        }
    }

    @PostMapping("/interview/submit-answer")
    public ResponseEntity<?> submitAnswer(@RequestBody Map<String, Object> payload) {
        try {
            Long questionId = Long.valueOf(payload.get("questionId").toString());
            String answer = payload.getOrDefault("answer", "").toString();

            AnswerEvaluationOutput eval = mockInterviewService.submitAnswer(questionId, answer);
            return ResponseEntity.ok(Map.of(
                    "success", true,
                    "score", eval.score,
                    "feedback", eval.feedback,
                    "strengths", eval.strengths,
                    "improvements", eval.improvements,
                    "modelAnswer", eval.modelAnswer
            ));
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(Map.of("error", e.getMessage()));
        }
    }

    @PostMapping("/interview/finish")
    public ResponseEntity<?> finishInterview(@RequestBody Map<String, Long> payload) {
        try {
            Long sessionId = payload.get("sessionId");
            InterviewSession session = mockInterviewService.finishInterview(sessionId);
            return ResponseEntity.ok(Map.of(
                    "success", true,
                    "sessionId", session.getId(),
                    "candidateId", session.getCandidateId(),
                    "overallScore", session.getOverallScore(),
                    "technicalScore", session.getTechnicalScore(),
                    "communicationScore", session.getCommunicationScore(),
                    "redirectUrl", "/interview-report/" + session.getCandidateId()
            ));
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(Map.of("error", e.getMessage()));
        }
    }
}
