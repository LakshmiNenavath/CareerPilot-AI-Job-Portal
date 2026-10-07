package com.careerpilot.model;

import com.fasterxml.jackson.annotation.JsonIgnore;
import jakarta.persistence.*;
import java.time.LocalDateTime;

@Entity
@Table(name = "interview_questions")
public class InterviewQuestion {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "interview_session_id", nullable = false)
    @JsonIgnore
    private InterviewSession interviewSession;

    private Integer questionIndex;

    private String category; // Technical, Problem Solving, System Design, Behavioral

    @Lob
    @Column(columnDefinition = "CLOB")
    private String questionText;

    @Lob
    @Column(columnDefinition = "CLOB")
    private String expectedKeyPoints;

    @Lob
    @Column(columnDefinition = "CLOB")
    private String candidateAnswer;

    private Integer answerScore;

    @Lob
    @Column(columnDefinition = "CLOB")
    private String aiFeedback;

    private LocalDateTime answeredAt;

    public InterviewQuestion() {}

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }

    public InterviewSession getInterviewSession() { return interviewSession; }
    public void setInterviewSession(InterviewSession interviewSession) { this.interviewSession = interviewSession; }

    public Integer getQuestionIndex() { return questionIndex; }
    public void setQuestionIndex(Integer questionIndex) { this.questionIndex = questionIndex; }

    public String getCategory() { return category; }
    public void setCategory(String category) { this.category = category; }

    public String getQuestionText() { return questionText; }
    public void setQuestionText(String questionText) { this.questionText = questionText; }

    public String getExpectedKeyPoints() { return expectedKeyPoints; }
    public void setExpectedKeyPoints(String expectedKeyPoints) { this.expectedKeyPoints = expectedKeyPoints; }

    public String getCandidateAnswer() { return candidateAnswer; }
    public void setCandidateAnswer(String candidateAnswer) { this.candidateAnswer = candidateAnswer; }

    public Integer getAnswerScore() { return answerScore; }
    public void setAnswerScore(Integer answerScore) { this.answerScore = answerScore; }

    public String getAiFeedback() { return aiFeedback; }
    public void setAiFeedback(String aiFeedback) { this.aiFeedback = aiFeedback; }

    public LocalDateTime getAnsweredAt() { return answeredAt; }
    public void setAnsweredAt(LocalDateTime answeredAt) { this.answeredAt = answeredAt; }
}
