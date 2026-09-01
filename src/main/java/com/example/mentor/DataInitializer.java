package com.example.mentor;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.CommandLineRunner;
import org.springframework.stereotype.Component;
import java.time.LocalDateTime;

@Component
public class DataInitializer implements CommandLineRunner {

    @Autowired
    private UserRepository userRepository;

    // YENİ KAYIT 5 (Güvenlik / Ortam Değişkenleri): Şifreler koddan çıkarıldı.
    // Artık application.properties dosyasından (veya sunucu değişkenlerinden) okunacak.
    @Value("${admin.default.username:A.D.M.İ.N}")
    private String adminUsername;

    @Value("${admin.default.password:hayrat14531299.,}")
    private String adminPassword;

    @Override
    public void run(String... args) throws Exception {
        // Sistem açıldığında veritabanında yönetici var mı diye kontrol ediyoruz
        if (userRepository.findByUsername(adminUsername) == null) {

            User admin = new User();
            admin.setFullName("Sistem Yöneticisi");
            admin.setUsername(adminUsername);
            admin.setPassword(adminPassword);
            admin.setRole("ADMIN"); // Rolü Admin olarak atanıyor
            admin.setApproved(true); // GÜVENLİK GÜNCELLEMESİ: Admin otomatik olarak tam onaylı kaydedilir.
            admin.setInactiveWarningSent(false);
            admin.setLastLoginDate(LocalDateTime.now());

            userRepository.save(admin);

            // Konsola başarı mesajı yazdırıyoruz (Güvenlik için şifre maskelendi)
            System.out.println("==================================================");
            System.out.println("✅ OTOMATİK ADMİN HESABI BAŞARIYLA OLUŞTURULDU!");
            System.out.println("Kullanıcı Adı: " + adminUsername + " | Şifre: ***[GİZLENDİ]***");
            System.out.println("==================================================");
        }
    }
}