package com.example.mentor;

import org.springframework.data.repository.CrudRepository;
import java.util.List;

public interface MessageRepository extends CrudRepository<Message, Long> {

    // Bir kişinin gönderdiği veya aldığı tüm mesajları tarihe göre sıralayarak getirir (Öğrenci/Mentör Sohbeti için)
    List<Message> findBySenderIdOrReceiverIdOrderBySentAtAsc(Long senderId, Long receiverId);

    // Admin paneli için sistemdeki BÜTÜN mesajları en yeniden eskiye doğru getirir
    List<Message> findAllByOrderBySentAtDesc();
}