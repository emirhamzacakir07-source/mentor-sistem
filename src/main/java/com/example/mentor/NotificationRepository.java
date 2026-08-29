package com.example.mentor;

import org.springframework.data.jpa.repository.JpaRepository;

public interface NotificationRepository extends JpaRepository<Notification, Long> {
    // Şimdilik standart CRUD (Kaydet, Sil, Bul) işlemleri yeterli.
    // İleride "sadece okunmamış bildirimleri getir" gibi özel filtreler buraya eklenebilir.
}