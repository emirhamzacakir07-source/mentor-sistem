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

    @Autowired private AnswerRepository answerRepository;
    @Autowired private QuestionRepository questionRepository;
    @Autowired private AiService aiService;

    // Her 60.000 milisaniyede (1 dakikada) bir aktif olmayan öğrencileri kontrol eder
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
}