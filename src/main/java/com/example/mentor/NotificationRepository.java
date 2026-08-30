package com.example.mentor;

import org.springframework.data.repository.CrudRepository; // veya JpaRepository

public interface NotificationRepository extends org.springframework.data.jpa.repository.JpaRepository<Notification, Long> {
    // Şimdilik standart CRUD (Kaydet, Sil, Bul) işlemleri yeterli.
}