package com.example.mentor;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import java.time.DayOfWeek;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Service
public class NotificationService {

    @Autowired private UserRepository userRepository;
    @Autowired private NotificationRepository notificationRepository;
    @Autowired private AnswerRepository answerRepository;

    // 1. MEVCUT KONTROL: Her 1 dakikada bir, 7 gündür sisteme GİRMEYENLERİ kontrol eder
    @Scheduled(fixedRate = 60000)
    public void checkInactiveUsers() {
        List<User> users = userRepository.findAll();
        for (User u : users) {
            if ("STUDENT".equals(u.getRole())) {
                if (u.getLastLoginDate() != null && u.getLastLoginDate().isBefore(LocalDateTime.now().minusDays(7)) && !u.isInactiveWarningSent()) {
                    Notification n = new Notification();
                    n.setMessage("⚠️ GİRİŞ UYARISI: " + u.getFullName() + " adlı öğrenci 1 haftadır sisteme giriş yapmıyor!");
                    n.setCreatedAt(LocalDateTime.now());
                    notificationRepository.save(n);
                    u.setInactiveWarningSent(true);
                    userRepository.save(u);
                }
            }
        }
    }

    // 2. YENİ KONTROL: SADECE CUMA GÜNLERİ SAAT 18:00'DA ÇALIŞIR (Son Çağrı Raporu)
    // Cron formatı: saniye dakika saat gün ay haftanın_günü (SUN-SAT)
    @Scheduled(cron = "0 0 18 * * FRI")
    public void generateFridayMissingTasksReport() {
        List<User> allUsers = userRepository.findAll();
        List<String> missingStudents = new ArrayList<>();

        // Perşembe 18:00'dan şu anki Cuma 18:00'a kadar olan süreyi referans al
        LocalDateTime sistemAcilis = LocalDateTime.now().minusDays(1).withHour(18).withMinute(0);

        for (User u : allUsers) {
            if ("STUDENT".equals(u.getRole())) {
                // Bu öğrencinin bu hafta gönderdiği cevapları bul
                List<Answer> recentAnswers = answerRepository.findByStudentId(u.getId());
                boolean hasAnsweredThisWeek = false;

                for(Answer ans : recentAnswers) {
                    if(ans.getCreatedAt().isAfter(sistemAcilis)) {
                        hasAnsweredThisWeek = true;
                        break;
                    }
                }

                if (!hasAnsweredThisWeek) {
                    missingStudents.add(u.getFullName());
                }
            }
        }

        if (!missingStudents.isEmpty()) {
            Notification n = new Notification();
            String namesList = String.join(", ", missingStudents);
            n.setMessage("🚨 CUMA 18:00 RAPORU: Sistemin kapanmasına 2 saat kaldı. Henüz vazife/soru göndermeyen öğrenciler: " + namesList);
            n.setCreatedAt(LocalDateTime.now());
            notificationRepository.save(n);
        } else {
            Notification n = new Notification();
            n.setMessage("✅ CUMA 18:00 RAPORU: Harika! Sisteme kayıtlı tüm aktif öğrenciler bu haftanın görevlerini teslim etti.");
            n.setCreatedAt(LocalDateTime.now());
            notificationRepository.save(n);
        }
    }
}