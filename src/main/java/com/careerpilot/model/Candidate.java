package com.careerpilot.model;

import jakarta.persistence.*;
import java.time.LocalDateTime;

@Entity
@Table(name = "candidates")
public class Candidate {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private String fullName = "Aspiring Candidate";

    private String email;
    private String phone;

    @Column(nullable = false)
    private String detectedRole = "Software Engineer";

    private Integer roleConfidence = 85;

    private String experienceLevel = "Entry-Level / Fresher";

    private Integer atsScore = 75;

    @Lob
    @Column(columnDefinition = "CLOB")
    private String resumeSummary;

    @Lob
    @Column(columnDefinition = "CLOB")
    private String prosJson; // JSON array of strengths

    @Lob
    @Column(columnDefinition = "CLOB")
    private String consJson; // JSON array of weaknesses

    @Lob
    @Column(columnDefinition = "CLOB")
    private String improvementSuggestionsJson; // JSON array of improvements

    @Lob
    @Column(columnDefinition = "CLOB")
    private String skillsFoundJson; // JSON array of extracted skills

    @Lob
    @Column(columnDefinition = "CLOB")
    private String missingKeywordsJson; // JSON array of missing role keywords

    @Lob
    @Column(columnDefinition = "CLOB")
    private String breakdownScoresJson; // JSON object with sub scores

    @Lob
    @Column(columnDefinition = "CLOB")
    private String extractedResumeText;

    private String resumeFileName;

    private LocalDateTime createdAt = LocalDateTime.now();

    private Integer interviewScore;
    private Integer technicalScore;
    private Integer communicationScore;
    private Integer problemSolvingScore;

    private Boolean interviewCompleted = false;

    public Candidate() {}

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }

    public String getFullName() { return fullName; }
    public void setFullName(String fullName) { this.fullName = fullName; }

    public String getEmail() { return email; }
    public void setEmail(String email) { this.email = email; }

    public String getPhone() { return phone; }
    public void setPhone(String phone) { this.phone = phone; }

    public String getDetectedRole() { return detectedRole; }
    public void setDetectedRole(String detectedRole) { this.detectedRole = detectedRole; }

    public Integer getRoleConfidence() { return roleConfidence; }
    public void setRoleConfidence(Integer roleConfidence) { this.roleConfidence = roleConfidence; }

    public String getExperienceLevel() { return experienceLevel; }
    public void setExperienceLevel(String experienceLevel) { this.experienceLevel = experienceLevel; }

    public Integer getAtsScore() { return atsScore; }
    public void setAtsScore(Integer atsScore) { this.atsScore = atsScore; }

    public String getResumeSummary() { return resumeSummary; }
    public void setResumeSummary(String resumeSummary) { this.resumeSummary = resumeSummary; }

    public String getProsJson() { return prosJson; }
    public void setProsJson(String prosJson) { this.prosJson = prosJson; }

    public String getConsJson() { return consJson; }
    public void setConsJson(String consJson) { this.consJson = consJson; }

    public String getImprovementSuggestionsJson() { return improvementSuggestionsJson; }
    public void setImprovementSuggestionsJson(String improvementSuggestionsJson) { this.improvementSuggestionsJson = improvementSuggestionsJson; }

    public String getSkillsFoundJson() { return skillsFoundJson; }
    public void setSkillsFoundJson(String skillsFoundJson) { this.skillsFoundJson = skillsFoundJson; }

    public String getMissingKeywordsJson() { return missingKeywordsJson; }
    public void setMissingKeywordsJson(String missingKeywordsJson) { this.missingKeywordsJson = missingKeywordsJson; }

    public String getBreakdownScoresJson() { return breakdownScoresJson; }
    public void setBreakdownScoresJson(String breakdownScoresJson) { this.breakdownScoresJson = breakdownScoresJson; }

    public String getExtractedResumeText() { return extractedResumeText; }
    public void setExtractedResumeText(String extractedResumeText) { this.extractedResumeText = extractedResumeText; }

    public String getResumeFileName() { return resumeFileName; }
    public void setResumeFileName(String resumeFileName) { this.resumeFileName = resumeFileName; }

    public LocalDateTime getCreatedAt() { return createdAt; }
    public void setCreatedAt(LocalDateTime createdAt) { this.createdAt = createdAt; }

    public Integer getInterviewScore() { return interviewScore; }
    public void setInterviewScore(Integer interviewScore) { this.interviewScore = interviewScore; }

    public Integer getTechnicalScore() { return technicalScore; }
    public void setTechnicalScore(Integer technicalScore) { this.technicalScore = technicalScore; }

    public Integer getCommunicationScore() { return communicationScore; }
    public void setCommunicationScore(Integer communicationScore) { this.communicationScore = communicationScore; }

    public Integer getProblemSolvingScore() { return problemSolvingScore; }
    public void setProblemSolvingScore(Integer problemSolvingScore) { this.problemSolvingScore = problemSolvingScore; }

    public Boolean getInterviewCompleted() { return interviewCompleted; }
    public void setInterviewCompleted(Boolean interviewCompleted) { this.interviewCompleted = interviewCompleted; }
}
