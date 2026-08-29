package com.example.mentor;

import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;

public interface UserRepository extends JpaRepository<User, Long> {

    // Giriş yaparken kullanıcı adı ve şifre kontrolü için
    User findByUsernameAndPassword(String username, String password);

    // Kayıt olurken veya şifre sıfırlarken kullanıcı adının varlığını kontrol etmek için
    User findByUsername(String username);

    // YENİ EKLENDİ: Admin panelinde sadece Mentörleri veya sadece Öğrencileri listelemek için
    List<User> findByRole(String role);
}