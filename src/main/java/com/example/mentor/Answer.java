package com.example.mentor;
import jakarta.persistence.*;

@Entity
@Table(name = "answers")
public class Answer {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private Long studentId;
    private Long questionId;

    // Uzun cevaplarda veritabanının hata vermemesi için TEXT tipi belirlendi.
    @Column(columnDefinition = "TEXT")
    private String answerText;

    private String aiNote;

    // --- YENİ EKLENEN: MENTÖR DEĞERLENDİRME ALANLARI ---
    private Integer mentorScore; // Mentörün vereceği 0-100 arası puan

    @Column(columnDefinition = "TEXT")
    private String mentorFeedback; // Mentörün öğrenciye yazacağı mesaj (Uzun olabileceği için TEXT yapıldı)

    // --- GETTER VE SETTER METOTLARI ---

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }

    public Long getStudentId() { return studentId; }
    public void setStudentId(Long studentId) { this.studentId = studentId; }

    public Long getQuestionId() { return questionId; }
    public void setQuestionId(Long questionId) { this.questionId = questionId; }

    public String getAnswerText() { return answerText; }
    public void setAnswerText(String answerText) { this.answerText = answerText; }

    public String getAiNote() { return aiNote; }
    public void setAiNote(String aiNote) { this.aiNote = aiNote; }

    // Yeni Eklenenlerin Getter/Setter'ları
    public Integer getMentorScore() { return mentorScore; }
    public void setMentorScore(Integer mentorScore) { this.mentorScore = mentorScore; }

    public String getMentorFeedback() { return mentorFeedback; }
    public void setMentorFeedback(String mentorFeedback) { this.mentorFeedback = mentorFeedback; }
}