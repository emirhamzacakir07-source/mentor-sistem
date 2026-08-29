package com.example.mentor;

import jakarta.persistence.*;
import java.time.LocalDateTime;

@Entity
@Table(name = "users")
public class User {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private String fullName;
    private String username;
    private String email;
    private String password;

    private String personalPhone;
    private String parentPhone;
    private String parentEmail;
    private String school;
    private String gradeClass;

    private String role; // ADMIN, STUDENT veya MENTOR olacak

    // YENİ EKLENDİ: Mentörlerin uzmanlık alanını (Örn: Java, Ekonomi) tutmak için
    private String category;

    private LocalDateTime lastLoginDate;
    private boolean inactiveWarningSent;

    // --- GETTER VE SETTER METOTLARI ---

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }

    public String getFullName() { return fullName; }
    public void setFullName(String fullName) { this.fullName = fullName; }

    public String getUsername() { return username; }
    public void setUsername(String username) { this.username = username; }

    public String getEmail() { return email; }
    public void setEmail(String email) { this.email = email; }

    public String getPassword() { return password; }
    public void setPassword(String password) { this.password = password; }

    public String getPersonalPhone() { return personalPhone; }
    public void setPersonalPhone(String personalPhone) { this.personalPhone = personalPhone; }

    public String getParentPhone() { return parentPhone; }
    public void setParentPhone(String parentPhone) { this.parentPhone = parentPhone; }

    public String getParentEmail() { return parentEmail; }
    public void setParentEmail(String parentEmail) { this.parentEmail = parentEmail; }

    public String getSchool() { return school; }
    public void setSchool(String school) { this.school = school; }

    public String getGradeClass() { return gradeClass; }
    public void setGradeClass(String gradeClass) { this.gradeClass = gradeClass; }

    public String getRole() { return role; }
    public void setRole(String role) { this.role = role; }

    // Kategori için Getter ve Setter
    public String getCategory() { return category; }
    public void setCategory(String category) { this.category = category; }

    public LocalDateTime getLastLoginDate() { return lastLoginDate; }
    public void setLastLoginDate(LocalDateTime lastLoginDate) { this.lastLoginDate = lastLoginDate; }

    public boolean isInactiveWarningSent() { return inactiveWarningSent; }
    public void setInactiveWarningSent(boolean inactiveWarningSent) { this.inactiveWarningSent = inactiveWarningSent; }
}