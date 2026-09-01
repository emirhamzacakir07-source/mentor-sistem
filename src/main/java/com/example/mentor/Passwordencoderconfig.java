package com.example.mentor;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;

/**
 * Bu sınıf, Spring Security'nin TAMAMINI eklemeden (filtre zinciri, login
 * sayfası zorlaması vb. olmadan) sadece şifre hash'leme aracını (BCrypt)
 * uygulamaya kazandırır. pom.xml'e eklenen "spring-security-crypto"
 * bağımlılığı bu sınıfın çalışması için gereklidir.
 */
@Configuration
public class Passwordencoderconfig {

    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }
}
