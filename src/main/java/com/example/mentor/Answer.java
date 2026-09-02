package com.example.mentor;

import jakarta.persistence.*;
import java.time.LocalDateTime;

@Entity
@Table(name = "answers")
public class Answer {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private Long studentId;
    private Long questionId;

    private String selectedOptions;

    @Column(columnDefinition = "TEXT")
    private String answerText;

    private String aiNote;

    private Integer mentorScore;

    @Column(columnDefinition = "TEXT")
    private String mentorFeedback;

    private LocalDateTime createdAt;
    private boolean isAiScored = false;

    @Column(columnDefinition = "boolean default false")
    private Boolean isCompleted;

    // YENİ EKLENEN: Ay sonu puan sıfırlama kuralı için arşiv bayrağı
    @Column(columnDefinition = "boolean default false")
    private Boolean isMonthlyReset;

    // YENİ EKLENEN: Öğrencinin vazifeyi kaç gün uyguladığı (0-7 arası)
    private Integer completedDays;

    @PrePersist
    protected void onCreate() {
        createdAt = LocalDateTime.now();
    }

    // --- GETTER VE SETTER METOTLARI ---

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }

    public Long getStudentId() { return studentId; }
    public void setStudentId(Long studentId) { this.studentId = studentId; }

    public Long getQuestionId() { return questionId; }
    public void setQuestionId(Long questionId) { this.questionId = questionId; }

    public String getSelectedOptions() { return selectedOptions; }
    public void setSelectedOptions(String selectedOptions) { this.selectedOptions = selectedOptions; }

    public String getAnswerText() { return answerText; }
    public void setAnswerText(String answerText) { this.answerText = answerText; }

    public String getAiNote() { return aiNote; }
    public void setAiNote(String aiNote) { this.aiNote = aiNote; }

    public Integer getMentorScore() { return mentorScore; }
    public void setMentorScore(Integer mentorScore) { this.mentorScore = mentorScore; }

    public String getMentorFeedback() { return mentorFeedback; }
    public void setMentorFeedback(String mentorFeedback) { this.mentorFeedback = mentorFeedback; }

    public LocalDateTime getCreatedAt() { return createdAt; }
    public void setCreatedAt(LocalDateTime createdAt) { this.createdAt = createdAt; }

    public boolean isAiScored() { return isAiScored; }
    public void setAiScored(boolean aiScored) { this.isAiScored = aiScored; }

    public Boolean isCompleted() { return isCompleted != null ? isCompleted : false; }
    public void setCompleted(Boolean completed) { this.isCompleted = completed; }

    public Boolean isMonthlyReset() { return isMonthlyReset != null ? isMonthlyReset : false; }
    public void setMonthlyReset(Boolean monthlyReset) { this.isMonthlyReset = monthlyReset; }

    // YENİ GETTER/SETTER (Günlük vazife seçimi için)
    public Integer getCompletedDays() { return completedDays; }
    public void setCompletedDays(Integer completedDays) { this.completedDays = completedDays; }
}