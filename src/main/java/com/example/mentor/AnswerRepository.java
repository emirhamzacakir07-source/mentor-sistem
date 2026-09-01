package com.example.mentor;

import org.springframework.data.jpa.repository.JpaRepository;
import java.time.LocalDateTime;
import java.util.List;

public interface AnswerRepository extends JpaRepository<Answer, Long> {

    // ÖĞRENCİ FİLTRESİ: Belirli bir öğrencinin (studentId) verdiği tüm cevapları getirir
    List<Answer> findByStudentId(Long studentId);

    // SORU FİLTRESİ: Belirli bir soruya (questionId) verilmiş tüm cevapları getirir
    List<Answer> findByQuestionId(Long questionId);

    // YENİ KAYIT 4 & 12 (Yapay Zeka Toplu Rapor): Bir öğrencinin en son gönderdiği 5 cevabı tarihe göre azalan sırayla getirir
    List<Answer> findTop5ByStudentIdOrderByCreatedAtDesc(Long studentId);

    // YENİ KAYIT 21 (Zaman Aşımı / Yapay Zeka Yedekleme): Mentör tarafından henüz puanlanmamış
    // ve belirlenen zaman sınırını (örn. 48 saat öncesi) geçmiş cevapları bulur
    List<Answer> findByMentorScoreIsNullAndCreatedAtBefore(LocalDateTime cutoffTime);
}