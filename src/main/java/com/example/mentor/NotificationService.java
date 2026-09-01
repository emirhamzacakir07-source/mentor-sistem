package com.example.mentor;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import java.time.LocalDateTime;
import java.util.List;

@Service
public class NotificationService {

    @Autowired private UserRepository userRepository;
    @Autowired private NotificationRepository notificationRepository;

    // AI Yedekleme sistemi için gerekli Repository ve Servisler eklendi
    @Autowired private AnswerRepository answerRepository;
    @Autowired private QuestionRepository questionRepository;
    @Autowired private AiService aiService;

    // Her 60.000 milisaniyede (1 dakikada) bir bu metodu otomatik çalıştırır
    @Scheduled(fixedRate = 60000)
    public void checkInactiveUsers() {
        List<User> users = userRepository.findAll();

        for (User u : users) {
            // Sadece öğrencileri kontrol et
            if ("STUDENT".equals(u.getRole())) {

                // Öğrenci en son 2 günden daha önce giriş yaptıysa ve henüz uyarı gönderilmediyse
                if (u.getLastLoginDate() != null && u.getLastLoginDate().isBefore(LocalDateTime.now().minusDays(2)) && !u.isInactiveWarningSent()) {

                    // Admine bildirim oluştur
                    Notification n = new Notification();
                    n.setMessage("⚠️ GİRİŞ UYARISI: " + u.getFullName() + " adlı öğrenci 2 günden uzun süredir sisteme giriş yapmıyor!");
                    n.setCreatedAt(LocalDateTime.now());
                    notificationRepository.save(n);

                    // Aynı uyarıyı defalarca atmamak için bayrağı (flag) true yapıyoruz
                    u.setInactiveWarningSent(true);
                    userRepository.save(u);
                }
            }
        }
    }

    // YENİ KAYIT 21 (Zaman Aşımı / Yapay Zeka Yedekleme): Her 1 saatte bir (3600000 ms) çalışır.
    @Scheduled(fixedRate = 3600000)
    public void processUnattendedAnswers() {
        // 48 saat (2 gün) öncesini sınır olarak belirliyoruz
        LocalDateTime cutoffTime = LocalDateTime.now().minusHours(48);

        // Mentörü tarafından 48 saattir puanlanmamış (ve AI'ın henüz dokunmadığı) cevapları bul
        List<Answer> unattendedAnswers = answerRepository.findByMentorScoreIsNullAndCreatedAtBefore(cutoffTime);

        for (Answer ans : unattendedAnswers) {
            // Sorunun maksimum puanını bul (AI'ın üzerinden hesaplama yapabilmesi için)
            Question q = questionRepository.findById(ans.getQuestionId()).orElse(null);
            Integer maxPoints = (q != null && q.getMaxPoints() != null) ? q.getMaxPoints() : 100;

            // AiService üzerinden değerlendirme ve puan al
            AiService.AiEvaluationResult result = aiService.evaluateAndScoreAnswer(ans.getAnswerText(), maxPoints);

            // AI sonuçlarını cevaba kaydet
            ans.setMentorScore(result.getScore());
            ans.setMentorFeedback("🤖 [Sistem Otomatik Değerlendirmesi]: " + result.getFeedback());
            ans.setAiScored(true); // AI tarafından puanlandığını işaretle

            answerRepository.save(ans);

            // İşlem yapıldığında sisteme bildirim düşsün
            Notification n = new Notification();
            n.setMessage("🤖 AI DEVREDE: Geciken bir cevap yapay zeka tarafından otomatik olarak değerlendirildi (Cevap ID: " + ans.getId() + ").");
            n.setCreatedAt(LocalDateTime.now());
            notificationRepository.save(n);
        }
    }
}