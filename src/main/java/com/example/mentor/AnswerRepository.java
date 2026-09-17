package com.example.mentor;

import org.springframework.data.jpa.repository.JpaRepository;
import java.time.LocalDateTime;
import java.util.List;

public interface AnswerRepository extends JpaRepository<Answer, Long> {

    List<Answer> findByStudentId(Long studentId);
    List<Answer> findByQuestionId(Long questionId);
    List<Answer> findTop5ByStudentIdOrderByCreatedAtDesc(Long studentId);
    List<Answer> findByMentorScoreIsNullAndCreatedAtBefore(LocalDateTime cutoffTime);
}