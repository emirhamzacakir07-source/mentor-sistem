package com.example.mentor;

import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;

public interface AnswerRepository extends JpaRepository<Answer, Long> {

    // ÖĞRENCİ FİLTRESİ: Belirli bir öğrencinin (studentId) verdiği tüm cevapları getirir
    List<Answer> findByStudentId(Long studentId);

    // SORU FİLTRESİ: Belirli bir soruya (questionId) verilmiş tüm cevapları getirir
    List<Answer> findByQuestionId(Long questionId);
}