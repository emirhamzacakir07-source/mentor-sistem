package com.example.mentor;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.repository.CrudRepository;
import java.util.List;

public interface MessageRepository extends CrudRepository<Message, Long> {

    // Bir kişinin gönderdiği veya aldığı tüm mesajları tarihe göre sıralayarak getirir
    List<Message> findBySenderIdOrReceiverIdOrderBySentAtAsc(Long senderId, Long receiverId);

    // YENİ: Admin paneli için sistemdeki mesajları SAYFALARA BÖLEREK getirir (Performans için)
    Page<Message> findAllByOrderBySentAtDesc(Pageable pageable);
}