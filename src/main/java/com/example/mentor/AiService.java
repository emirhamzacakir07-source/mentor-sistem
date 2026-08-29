package com.example.mentor;
import org.springframework.stereotype.Service;
import java.util.Locale;

@Service
public class AiService {

    public String analyzeText(String text) {
        // GÜVENLİK: Eğer metin boş gelirse sistem çökmesin, direkt nötr dönsün
        if (text == null || text.trim().isEmpty()) {
            return "🟡 NÖTR: Belirgin bir uç duygu tespit edilemedi. (Metin boş)";
        }

        // TÜRKÇE DESTEĞİ: Karakterlerin doğru küçülmesi için Locale eklendi
        String lowerText = text.toLowerCase(new Locale("tr", "TR"));

        if (lowerText.contains("yorgun") || lowerText.contains("kötü") || lowerText.contains("stres") || lowerText.contains("canım sıkkın")) {
            return "🔴 DİKKAT: Öğrenci stresli veya mutsuz olabilir. Ailesiyle veya birebir iletişim önerilir.";
        } else if (lowerText.contains("mutlu") || lowerText.contains("iyi") || lowerText.contains("güzel") || lowerText.contains("harika")) {
            return "🟢 POZİTİF: Öğrencinin motivasyonu yüksek ve durumu iyi görünüyor.";
        } else {
            return "🟡 NÖTR: Belirgin bir uç duygu tespit edilemedi. (Rutin durum)";
        }
    }
}