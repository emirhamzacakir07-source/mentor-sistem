package com.example.mentor;

import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;

public interface QuestionRepository extends JpaRepository<Question, Long> {

    // MENTÖR FİLTRESİ: Belirli bir kategoriye (Örn: "Java") ait olan tüm soruları getirir
    List<Question> findByCategory(String category);

}