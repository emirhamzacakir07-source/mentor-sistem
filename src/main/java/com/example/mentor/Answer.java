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

    // UYARI EKLENDİ: Uzun cevaplarda veritabanının hata vermemesi için TEXT tipi belirlendi.
    @Column(columnDefinition = "TEXT")
    private String answerText;

    private String aiNote;

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
}