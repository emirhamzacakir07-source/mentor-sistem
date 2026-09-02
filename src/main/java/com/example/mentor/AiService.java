package com.example.mentor;

import org.springframework.stereotype.Service;
import java.util.List;
import java.util.Locale;

@Service
public class AiService {

    // 1. ANLIK DURUM ANALİZİ: Öğrenci cevap gönderdiğinde sistemin genel durum tespiti
    public String analyzeText(String text) {
        // GÜVENLİK: Eğer metin boş gelirse veya sadece şık seçildiyse (boş metin)
        if (text == null || text.trim().isEmpty()) {
            return "🔵 KARARLI: Görev odaklı yaklaşım (Sadece şık/görev seçimi yapıldı).";
        }

        // TÜRKÇE DESTEĞİ: Karakterlerin doğru küçülmesi
        String lowerText = text.toLowerCase(new Locale("tr", "TR"));

        if (lowerText.contains("yorgun") || lowerText.contains("kötü") || lowerText.contains("stres") || lowerText.contains("zor") || lowerText.contains("yapamadım")) {
            return "🔴 DİKKAT: Öğrenci zorlanıyor veya moralsiz olabilir. Motive edici geri bildirim şart.";
        } else if (lowerText.contains("mutlu") || lowerText.contains("iyi") || lowerText.contains("başardım") || lowerText.contains("kolay") || lowerText.contains("eğlenceli")) {
            return "🟢 POZİTİF: Özgüveni yüksek, görevi severek yapıyor.";
        } else {
            return "🔵 KARARLI: Göreve odaklanmış, stabil bir ilerleyiş sergiliyor.";
        }
    }

    // 2. YENİ: KARAKTER, ALIŞKANLIK VE ZİHNİYET ANALİZİ (3 Maddelik Mentör Raporu)
    // Mentör "AI Karne Çıkar" butonuna bastığında bu metot çalışır. Not veya puan yoktur.
    public String analyzeStudentPerformance(String studentName, List<String> answers) {
        if (answers == null || answers.isEmpty()) {
            return "⚠️ " + studentName + " henüz analiz edilecek hiçbir vazife/soru yanıtlamamıştır.";
        }

        int totalAnswers = answers.size();
        boolean isDetailed = false;
        boolean isAnalytic = false;
        boolean isShort = false;
        int totalLength = 0;

        // Öğrencinin tüm cevaplarındaki eğilimleri, alışkanlıkları ve kullandığı bağlaçları tarıyoruz
        for (String ans : answers) {
            if (ans != null) {
                totalLength += ans.trim().length();
                String lowerAns = ans.toLowerCase(new Locale("tr", "TR"));

                // Analitik zeka ve neden-sonuç kurma becerisi testi
                if (lowerAns.contains("çünkü") || lowerAns.contains("neden") || lowerAns.contains("bence") || lowerAns.contains("göre") || lowerAns.contains("mesela")) {
                    isAnalytic = true;
                }
            }
        }

        int avgLength = totalAnswers > 0 ? totalLength / totalAnswers : 0;

        // Karakter ve alışkanlık profili belirleme
        if (avgLength > 80) isDetailed = true;
        if (avgLength < 25) isShort = true;

        StringBuilder report = new StringBuilder();
        report.append("🧠 ").append(studentName).append(" - Karakter ve Davranış Analizi\n\n");
        report.append("Tamamlanan Görev/Soru Sayısı: ").append(totalAnswers).append("\n\n");
        report.append("📌 YAPAY ZEKA GELİŞİM RAPORU:\n");

        if (isAnalytic && isDetailed) {
            report.append("1. 🎯 Sorumluluk Bilinci: Verilen vazifeleri geçiştirmek yerine üzerine düşünerek yapıyor. Sorgulayıcı bir zihniyete sahip.\n");
            report.append("2. 💡 Analitik Düşünme: Olaylar arasında neden-sonuç ilişkisi kurabiliyor. Kendi fikirlerini (bence, çünkü) katmayı seviyor.\n");
            report.append("3. 🚀 Mentör Tavsiyesi: Bu öğrencinin potansiyeli yüksek. Ona kuralları dikte etmek yerine, 'Sen olsan ne yapardın?' diyerek liderlik vasfını tetikleyin.");
        }
        else if (isDetailed) {
            report.append("1. 📝 İfade Gücü: Kendini ifade etmeyi ve detaylandırmayı seviyor. İletişime çok açık.\n");
            report.append("2. 🎯 Odaklanma: Görevlere vakit ayırıyor ancak 'neden-sonuç' bağlamı (analitik düşünce) biraz daha desteklenebilir.\n");
            report.append("3. 🚀 Mentör Tavsiyesi: Yazmayı sevdiği için ona hislerini ve fikirlerini soran, yoruma dayalı hayat-beceri vazifeleri vermeye devam edin.");
        }
        else if (isShort) {
            report.append("1. ⚡ Hız ve Pratiklik: Görevleri hızlıca bitirme eğiliminde. Detaylarda boğulmayı sevmiyor, sonuç odaklı.\n");
            report.append("2. 📉 Derinlik İhtiyacı: Seçimlerini yaparken yüzeysel kalıyor veya uzun yazmaktan çabuk sıkılıyor.\n");
            report.append("3. 🚀 Mentör Tavsiyesi: Uzun metinler okutmak yerine; kısa, oyunlaştırılmış ve '1 ile 7 gün arası tamamla' gibi pratik vazifelerle motivasyonunu artırın.");
        }
        else {
            report.append("1. ⚖️ Dengeli Profil: Vazifeleri gerektiği kadar yapıyor. Ne çok abartıyor ne de görevlerini boşluyor.\n");
            report.append("2. 🔄 Rutin Uyum: Sisteme ve kurallara kolayca uyum sağlayan, stabil ve söz dinleyen bir karakteri var.\n");
            report.append("3. 🚀 Mentör Tavsiyesi: Öğrencinin konfor alanından çıkması için arada onu şaşırtacak, kendi sınırlarını zorlayacağı görevler verin.");
        }

        return report.toString();
    }
}