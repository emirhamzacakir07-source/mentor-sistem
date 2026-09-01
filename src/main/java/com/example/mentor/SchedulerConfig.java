package com.example.mentor;

import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.annotation.EnableScheduling;

@Configuration
@EnableScheduling // Zamanlayıcı (bildirim ve AI otomatik puanlama) ayarları artık bu profesyonel sınıftan yönetilecek
public class SchedulerConfig {
    // Bu sınıf Spring'in arka plan görevlerini (NotificationService içindeki @Scheduled metotları)
    // aktif hale getiren merkezi bir yapılandırma dosyasıdır.
    // İçine ekstra bir kod yazmamıza gerek yok, Spring bu anotasyonlarla sistemi otonom hale getiriyor!
}