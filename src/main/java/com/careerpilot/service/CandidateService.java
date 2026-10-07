package com.careerpilot.service;

import com.careerpilot.model.Candidate;
import com.careerpilot.repository.CandidateRepository;
import com.careerpilot.service.AiService.ResumeAnalysisOutput;
import com.careerpilot.service.RoleDetectionService.RoleDetectionResult;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.*;

@Service
public class CandidateService {

    private final CandidateRepository candidateRepo;
    private final ResumeParserService parserService;
    private final RoleDetectionService roleDetectionService;
    private final AiService aiService;
    private final ObjectMapper objectMapper = new ObjectMapper();

    public CandidateService(CandidateRepository candidateRepo,
                            ResumeParserService parserService,
                            RoleDetectionService roleDetectionService,
                            AiService aiService) {
        this.candidateRepo = candidateRepo;
        this.parserService = parserService;
        this.roleDetectionService = roleDetectionService;
        this.aiService = aiService;
    }

    @Transactional
    public Candidate processResumeFile(MultipartFile file) throws Exception {
        String filename = file.getOriginalFilename();
        String extractedText = parserService.extractText(file);
        return processResumeContent(extractedText, filename);
    }

    @Transactional
    public Candidate processResumeText(String text, String title) throws Exception {
        if (text == null || text.trim().isEmpty()) {
            throw new IllegalArgumentException("Resume text cannot be blank.");
        }
        return processResumeContent(text, title != null ? title : "Pasted Resume Content");
    }

    private Candidate processResumeContent(String resumeText, String sourceName) throws JsonProcessingException {
        // Extract basic metadata
        String email = parserService.extractEmail(resumeText);
        String phone = parserService.extractPhone(resumeText);
        String name = parserService.extractCandidateName(resumeText, "Candidate " + (candidateRepo.count() + 1));

        // Detect Role & Skills
        RoleDetectionResult roleResult = roleDetectionService.detectRole(resumeText);

        // Run AI Analysis
        ResumeAnalysisOutput analysis = aiService.analyzeResume(
                roleResult.getBestRole(),
                roleResult.getExperienceLevel(),
                roleResult.getFoundSkills(),
                resumeText,
                name
        );

        // Subscores JSON
        Map<String, Integer> subScores = Map.of(
                "formatting", analysis.formattingScore,
                "skills", analysis.skillsScore,
                "impact", analysis.impactScore,
                "readability", analysis.readabilityScore
        );

        Candidate candidate = new Candidate();
        candidate.setFullName(name);
        candidate.setEmail(email);
        candidate.setPhone(phone);
        candidate.setDetectedRole(roleResult.getBestRole());
        candidate.setRoleConfidence(roleResult.getConfidenceScore());
        candidate.setExperienceLevel(roleResult.getExperienceLevel());
        candidate.setAtsScore(analysis.atsScore);
        candidate.setResumeSummary(analysis.summary);
        candidate.setProsJson(objectMapper.writeValueAsString(analysis.pros));
        candidate.setConsJson(objectMapper.writeValueAsString(analysis.cons));
        candidate.setImprovementSuggestionsJson(objectMapper.writeValueAsString(analysis.improvements));
        candidate.setSkillsFoundJson(objectMapper.writeValueAsString(roleResult.getFoundSkills()));
        candidate.setMissingKeywordsJson(objectMapper.writeValueAsString(roleResult.getMissingKeywords()));
        candidate.setBreakdownScoresJson(objectMapper.writeValueAsString(subScores));
        candidate.setExtractedResumeText(resumeText);
        candidate.setResumeFileName(sourceName);
        candidate.setCreatedAt(LocalDateTime.now());
        candidate.setInterviewCompleted(false);

        return candidateRepo.save(candidate);
    }

    public Optional<Candidate> findById(Long id) {
        return candidateRepo.findById(id);
    }

    public List<Candidate> getAllCandidates() {
        return candidateRepo.findAllByOrderByCreatedAtDesc();
    }

    public List<Candidate> searchCandidates(String keyword) {
        if (keyword == null || keyword.trim().isEmpty()) {
            return getAllCandidates();
        }
        return candidateRepo.searchCandidates(keyword.trim());
    }

    @Transactional
    public void deleteCandidate(Long id) {
        candidateRepo.deleteById(id);
    }

    public long getTotalCandidatesCount() {
        return candidateRepo.count();
    }

    public long getCompletedInterviewsCount() {
        return candidateRepo.countByInterviewCompleted(true);
    }

    public double getAverageAtsScore() {
        Double avg = candidateRepo.getAverageAtsScore();
        return avg != null ? Math.round(avg * 10.0) / 10.0 : 0.0;
    }

    public double getAverageInterviewScore() {
        Double avg = candidateRepo.getAverageInterviewScore();
        return avg != null ? Math.round(avg * 10.0) / 10.0 : 0.0;
    }

    public List<String> deserializeList(String json) {
        if (json == null || json.isBlank()) return Collections.emptyList();
        try {
            return objectMapper.readValue(json, new TypeReference<List<String>>() {});
        } catch (Exception e) {
            return Collections.emptyList();
        }
    }

    public Map<String, Integer> deserializeMap(String json) {
        if (json == null || json.isBlank()) return Collections.emptyMap();
        try {
            return objectMapper.readValue(json, new TypeReference<Map<String, Integer>>() {});
        } catch (Exception e) {
            return Collections.emptyMap();
        }
    }

    public String exportCandidatesToCsv() {
        List<Candidate> candidates = getAllCandidates();
        StringBuilder sb = new StringBuilder();
        sb.append("ID,Full Name,Email,Phone,Detected Role,Experience Level,ATS Score,Interview Score,Interview Completed,Created At\n");
        DateTimeFormatter dtf = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");

        for (Candidate c : candidates) {
            sb.append(c.getId()).append(",")
              .append("\"").append(escapeCsv(c.getFullName())).append("\",")
              .append("\"").append(escapeCsv(c.getEmail())).append("\",")
              .append("\"").append(escapeCsv(c.getPhone())).append("\",")
              .append("\"").append(escapeCsv(c.getDetectedRole())).append("\",")
              .append("\"").append(escapeCsv(c.getExperienceLevel())).append("\",")
              .append(c.getAtsScore()).append(",")
              .append(c.getInterviewScore() != null ? c.getInterviewScore() : "N/A").append(",")
              .append(Boolean.TRUE.equals(c.getInterviewCompleted()) ? "YES" : "NO").append(",")
              .append(c.getCreatedAt() != null ? c.getCreatedAt().format(dtf) : "").append("\n");
        }
        return sb.toString();
    }

    private String escapeCsv(String str) {
        if (str == null) return "";
        return str.replace("\"", "\"\"");
    }
}
