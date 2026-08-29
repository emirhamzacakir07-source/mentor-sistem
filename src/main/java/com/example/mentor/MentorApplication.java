package com.example.mentor;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableScheduling;

@SpringBootApplication
@EnableScheduling // KRİTİK: Otomatik bildirim servisinin (Scheduled) çalışmasını sağlar
public class MentorApplication {

    public static void main(String[] args) {
        SpringApplication.run(MentorApplication.class, args);
    }

}