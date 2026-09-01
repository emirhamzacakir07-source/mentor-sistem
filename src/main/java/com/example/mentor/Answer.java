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

    // YENİ EKLENEN: Öğrencinin seçtiği şıklar (Örn: "A", "A,C" gibi virgülle ayrılarak tutulacak)
    private String selectedOptions;

    // Uzun cevaplarda veritabanının hata vermemesi için TEXT tipi belirlendi.
    @Column(columnDefinition = "TEXT")
    private String answerText;

    private String aiNote;

    // --- MENTÖR VE SİSTEM DEĞERLENDİRME ALANLARI ---
    // Şıklı sorularda sistem buraya otomatik puan yazacak. Klasiklerde mentör elle girecek.
    private Integer mentorScore;

    @Column(columnDefinition = "TEXT")
    private String mentorFeedback;

    // YENİ EKLENEN KAYIT 21 (Zaman Aşımı / Yapay Zeka Yedekleme) İÇİN GEREKLİ ALANLAR:
    // Cevabın ne zaman verildiğini tutarak zaman aşımı (örn. 48 saat) kuralını işleteceğiz.
    private LocalDateTime createdAt;

    // Mentör mü puanladı yoksa süresi geçince AI mı puanladı ayrımını yapabilmek için:
    private boolean isAiScored = false;

    // Cevap veritabanına ilk kaydedildiğinde zamanı otomatik atar
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
}