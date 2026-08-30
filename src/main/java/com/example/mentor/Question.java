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

    private String type; // KLASIK veya COKTAN_SECMELI
    private boolean isTask; // Vazife mi, soru mu?
    private String category;
    private Integer maxPoints;

    private String optionA;
    private Integer optionAPoint;
    private String optionB;
    private Integer optionBPoint;
    private String optionC;
    private Integer optionCPoint;
    private String optionD;
    private Integer optionDPoint;

    // YENİ EKLENEN: Öğrenci birden fazla şık (I, II ve III) seçebilsin mi?
    private boolean allowMultipleSelections;

    // --- GETTER VE SETTER METOTLARI ---

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }

    public String getContent() { return content; }
    public void setContent(String content) { this.content = content; }

    public String getType() { return type; }
    public void setType(String type) { this.type = type; }

    public boolean isTask() { return isTask; }
    public void setTask(boolean task) { isTask = task; }

    public String getCategory() { return category; }
    public void setCategory(String category) { this.category = category; }

    public Integer getMaxPoints() { return maxPoints; }
    public void setMaxPoints(Integer maxPoints) { this.maxPoints = maxPoints; }

    public String getOptionA() { return optionA; }
    public void setOptionA(String optionA) { this.optionA = optionA; }

    public Integer getOptionAPoint() { return optionAPoint; }
    public void setOptionAPoint(Integer optionAPoint) { this.optionAPoint = optionAPoint; }

    public String getOptionB() { return optionB; }
    public void setOptionB(String optionB) { this.optionB = optionB; }

    public Integer getOptionBPoint() { return optionBPoint; }
    public void setOptionBPoint(Integer optionBPoint) { this.optionBPoint = optionBPoint; }

    public String getOptionC() { return optionC; }
    public void setOptionC(String optionC) { this.optionC = optionC; }

    public Integer getOptionCPoint() { return optionCPoint; }
    public void setOptionCPoint(Integer optionCPoint) { this.optionCPoint = optionCPoint; }

    public String getOptionD() { return optionD; }
    public void setOptionD(String optionD) { this.optionD = optionD; }

    public Integer getOptionDPoint() { return optionDPoint; }
    public void setOptionDPoint(Integer optionDPoint) { this.optionDPoint = optionDPoint; }

    // Yeni Özelliğin Getter ve Setter'ı
    public boolean isAllowMultipleSelections() { return allowMultipleSelections; }
    public void setAllowMultipleSelections(boolean allowMultipleSelections) { this.allowMultipleSelections = allowMultipleSelections; }
}