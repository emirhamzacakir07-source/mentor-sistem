package com.example.mentor;
import jakarta.persistence.*;

@Entity
@Table(name = "questions")
public class Question {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(columnDefinition = "TEXT")
    private String content;

    private String category;
    private String type; // KLASIK veya COKTAN_SECMELI
    private boolean isTask; // true = Vazife, false = Soru

    // Hafta (weekNumber) silindi!

    private Integer maxPoints; // Öğrenci GÖRMEZ, sadece Admin/Mentör görür

    private String optionA;
    private String optionB;
    private String optionC;
    private String optionD;

    // --- GETTER VE SETTER METOTLARI ---
    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }

    public String getContent() { return content; }
    public void setContent(String content) { this.content = content; }

    public String getCategory() { return category; }
    public void setCategory(String category) { this.category = category; }

    public String getType() { return type; }
    public void setType(String type) { this.type = type; }

    public boolean isTask() { return isTask; }
    public void setTask(boolean task) { this.isTask = task; }

    public Integer getMaxPoints() { return maxPoints; }
    public void setMaxPoints(Integer maxPoints) { this.maxPoints = maxPoints; }

    public String getOptionA() { return optionA; }
    public void setOptionA(String optionA) { this.optionA = optionA; }

    public String getOptionB() { return optionB; }
    public void setOptionB(String optionB) { this.optionB = optionB; }

    public String getOptionC() { return optionC; }
    public void setOptionC(String optionC) { this.optionC = optionC; }

    public String getOptionD() { return optionD; }
    public void setOptionD(String optionD) { this.optionD = optionD; }
}