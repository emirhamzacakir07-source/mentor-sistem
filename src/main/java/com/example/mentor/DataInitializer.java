package com.example.mentor;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.CommandLineRunner;
import org.springframework.stereotype.Component;
import java.time.LocalDateTime;

@Component
public class DataInitializer implements CommandLineRunner {

    @Autowired
    private UserRepository userRepository;

    @Override
    public void run(String... args) throws Exception {
        // Sistem açıldığında veritabanında "A.D.M.İ.N" adında bir yönetici var mı diye kontrol ediyoruz
        if (userRepository.findByUsername("A.D.M.İ.N") == null) {

            User admin = new User();
            admin.setFullName("Sistem Yöneticisi");
            admin.setUsername("A.D.M.İ.N");
            admin.setPassword("hayrat14531299.,"); // Senin belirlediğin özel yönetici şifresi
            admin.setRole("ADMIN"); // Rolü Admin olarak atanıyor
            admin.setInactiveWarningSent(false);
            admin.setLastLoginDate(LocalDateTime.now());

            userRepository.save(admin);

            // Konsola başarı mesajı yazdırıyoruz
            System.out.println("==================================================");
            System.out.println("✅ OTOMATİK ADMİN HESABI BAŞARIYLA OLUŞTURULDU!");
            System.out.println("Kullanıcı Adı: A.D.M.İ.N | Şifre: hayrat14531299.,");
            System.out.println("==================================================");
        }
    }
}