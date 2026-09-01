package com.example.mentor;

import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;

public interface UserRepository extends JpaRepository<User, Long> {

    // GÜVENLİK NOTU: Eskiden burada findByUsernameAndPassword(username, password)
    // diye bir metot vardı; şifreyi düz metin karşılaştırıyordu. Artık şifreler
    // hash'lendiği için düz metin karşılaştırma anlamsız/güvensiz. Bunun yerine
    // sadece kullanıcı adına göre bulup, PasswordEncoder.matches() ile
    // AppController içinde karşılaştırıyoruz.
    User findByUsername(String username);

    // Admin panelinde sadece Mentörleri veya sadece Öğrencileri listelemek için
    List<User> findByRole(String role);
}
