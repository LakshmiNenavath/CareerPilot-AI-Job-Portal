package com.careerpilot.model;

import jakarta.persistence.*;
import java.time.LocalDateTime;

@Entity
@Table(name = "ai_prompt_configs")
public class AiPromptConfig {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, unique = true)
    private String promptKey; // RESUME_ANALYSIS, INTERVIEW_QUESTIONS, ANSWER_EVALUATION, INTERVIEW_FINAL_REPORT

    @Column(nullable = false)
    private String title;

    private String description;

    private String variablesHelp;

    @Lob
    @Column(nullable = false, columnDefinition = "CLOB")
    private String templateText;

    private LocalDateTime updatedAt = LocalDateTime.now();

    public AiPromptConfig() {}

    public AiPromptConfig(String promptKey, String title, String description, String variablesHelp, String templateText) {
        this.promptKey = promptKey;
        this.title = title;
        this.description = description;
        this.variablesHelp = variablesHelp;
        this.templateText = templateText;
        this.updatedAt = LocalDateTime.now();
    }

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }

    public String getPromptKey() { return promptKey; }
    public void setPromptKey(String promptKey) { this.promptKey = promptKey; }

    public String getTitle() { return title; }
    public void setTitle(String title) { this.title = title; }

    public String getDescription() { return description; }
    public void setDescription(String description) { this.description = description; }

    public String getVariablesHelp() { return variablesHelp; }
    public void setVariablesHelp(String variablesHelp) { this.variablesHelp = variablesHelp; }

    public String getTemplateText() { return templateText; }
    public void setTemplateText(String templateText) { 
        this.templateText = templateText; 
        this.updatedAt = LocalDateTime.now();
    }

    public LocalDateTime getUpdatedAt() { return updatedAt; }
    public void setUpdatedAt(LocalDateTime updatedAt) { this.updatedAt = updatedAt; }
}
