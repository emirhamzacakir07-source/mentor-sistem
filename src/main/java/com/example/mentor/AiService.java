package com.example.mentor;

import org.springframework.stereotype.Service;
import java.util.List;
import java.util.Locale;

@Service
public class AiService {

    // MEVCUT KODUN (Korundu) - Duygu ve Stres Analizi
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

    // YENİ KAYIT 4 & 12: Yapay Zeka Toplu Öğrenci Analizi (Veli/Mentör Karnesi)
    public String analyzeStudentPerformance(String studentName, List<String> answers) {
        if (answers == null || answers.isEmpty()) {
            return studentName + " henüz analiz edilecek yeterli cevap göndermemiştir.";
        }

        int totalAnswers = answers.size();
        int lengthScore = 0;

        for (String ans : answers) {
            if (ans != null) {
                lengthScore += ans.trim().length();
            }
        }

        int avgLength = totalAnswers > 0 ? lengthScore / totalAnswers : 0;
        String performanceLevel;
        String advice;

        if (avgLength > 100) {
            performanceLevel = "Çok İyi / İleri Düzey";
            advice = "Öğrenci sorulara detaylı ve analitik yaklaşabiliyor. Düşüncelerini aktarma konusunda çok başarılı. Bu seviyeyi koruması için ekstra araştırmaya dayalı zor sorular verilebilir.";
        } else if (avgLength > 40) {
            performanceLevel = "Orta / Gelişime Açık";
            advice = "Öğrenci temel düzeyde doğru cevaplar veriyor, ancak detaylandırma ve neden-sonuç ilişkisi kurma yeteneği geliştirilebilir. Cevaplarını biraz daha açması teşvik edilmeli.";
        } else {
            performanceLevel = "Desteğe İhtiyacı Var";
            advice = "Öğrenci cevapları çok kısa tutuyor veya konuya odaklanmakta zorlanıyor. Konuyu anladığından emin olmak için birebir görüşme veya daha yönlendirici sorular sorulması önerilir.";
        }

        return "📊 " + studentName + " - Gelişim ve Performans Analizi:\n\n" +
                "Toplam Değerlendirilen Soru: " + totalAnswers + "\n" +
                "Genel Performans Seviyesi: " + performanceLevel + "\n" +
                "Mentör/Veli İçin Tavsiye: " + advice;
    }

    // YENİ KAYIT 20 & 21: Mentör Yokluğunda AI Geri Dönüşü ve Puanlaması (Zaman Aşımı Sonrası)
    public AiEvaluationResult evaluateAndScoreAnswer(String answerContent, Integer maxPoints) {
        if (answerContent == null || answerContent.trim().isEmpty()) {
            return new AiEvaluationResult(0, "Cevap gönderilmediği için değerlendirme yapılamadı.");
        }

        String lowerText = answerContent.toLowerCase(new Locale("tr", "TR"));
        int length = answerContent.trim().length();
        int score = 0;
        String feedback = "";

        // Uzunluğa ve içeriğe dayalı otomatik puanlama mantığı
        if (length > 150) {
            score = maxPoints != null ? maxPoints : 100;
            feedback = "Harika bir cevap! Konuyu çok detaylı ve güzel açıklamışsın, emek verdiğin çok belli. Bu analitik yaklaşımını korumalısın.";
        } else if (length > 50) {
            score = maxPoints != null ? (maxPoints * 75 / 100) : 75;
            feedback = "Cevabın doğru yönde ve konuyu anladığını gösteriyor. Ancak biraz daha detaylandırarak düşüncelerini daha net ifade edebilirsin.";
        } else {
            score = maxPoints != null ? (maxPoints * 40 / 100) : 40;
            feedback = "Cevabın oldukça kısa kalmış. Bu konuda daha fazla pratik yapmaya ve fikirlerini biraz daha genişletmeye ne dersin?";
        }

        // Anahtar kelimelere göre ekstra motive edici geri bildirimler
        if (lowerText.contains("çünkü") || lowerText.contains("neden") || lowerText.contains("dolayı")) {
            feedback += " Ayrıca cevaplarında neden-sonuç ilişkisi kurmaya çalışman çok değerli bir özellik!";
        }
        if (lowerText.contains("örneğin") || lowerText.contains("mesela")) {
            feedback += " Konuyu örneklerle desteklemen anlatımını çok güçlendirmiş.";
        }

        return new AiEvaluationResult(score, feedback);
    }

    // AiService için yardımcı sonuç sınıfı (Puan ve Geri Bildirimi aynı anda döndürmek için)
    public static class AiEvaluationResult {
        private int score;
        private String feedback;

        public AiEvaluationResult(int score, String feedback) {
            this.score = score;
            this.feedback = feedback;
        }

        public int getScore() { return score; }
        public String getFeedback() { return feedback; }
    }
}