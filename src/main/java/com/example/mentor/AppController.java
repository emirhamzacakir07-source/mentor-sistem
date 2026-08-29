package com.example.mentor;

import jakarta.servlet.http.HttpSession;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import java.time.LocalDateTime;

@Controller
public class AppController {

    @Autowired private UserRepository userRepository;
    @Autowired private QuestionRepository questionRepository;
    @Autowired private AnswerRepository answerRepository;
    @Autowired private AiService aiService;
    @Autowired private NotificationRepository notificationRepository;

    @GetMapping("/")
    public String loginPage() { return "index"; }

    @GetMapping("/register")
    public String registerPage() { return "register"; }

    @PostMapping("/register")
    public String registerUser(@ModelAttribute User user, Model model) {
        if (userRepository.findByUsername(user.getUsername()) != null) {
            model.addAttribute("error", "Bu kullanıcı adı zaten alınmış!");
            return "register";
        }
        user.setRole("STUDENT"); // Varsayılan kayıt olanlar öğrenci
        user.setLastLoginDate(LocalDateTime.now());
        userRepository.save(user);
        return "redirect:/?success=true";
    }

    @PostMapping("/login")
    public String login(@RequestParam String username, @RequestParam String password, HttpSession session, Model model) {
        User user = userRepository.findByUsernameAndPassword(username, password);
        if (user != null) {
            user.setLastLoginDate(LocalDateTime.now());
            user.setInactiveWarningSent(false);
            userRepository.save(user);

            // GÜVENLİK: Giriş yapan kişinin kimliğini oturuma (session) kaydediyoruz
            session.setAttribute("loggedInUserId", user.getId());
            session.setAttribute("loggedInUserRole", user.getRole());
            session.setAttribute("loggedInUsername", user.getUsername());

            // Yönlendirmeler
            if (user.getRole().equals("ADMIN")) {
                return "redirect:/admin";
            } else if (user.getRole().equals("MENTOR")) {
                return "redirect:/mentor"; // Mentör paneline yönlendirme eklendi
            } else {
                return "redirect:/student";
            }
        }
        model.addAttribute("error", "Kullanıcı adı veya şifre hatalı!");
        return "index";
    }

    // YENİ: Çıkış Yapma Metodu (Oturumu Kapatır)
    @GetMapping("/logout")
    public String logout(HttpSession session) {
        session.invalidate(); // Kullanıcının tüm oturum bilgilerini temizler
        return "redirect:/"; // Giriş ekranına geri yollar
    }

    @GetMapping("/forgot-password")
    public String forgotPasswordPage() { return "forgot-password"; }

    @PostMapping("/forgot-password")
    public String processForgotPassword(@RequestParam String username, Model model) {
        User user = userRepository.findByUsername(username);
        if (user != null) {
            Notification n = new Notification();
            n.setMessage("🔴 ŞİFRE SIFIRLAMA TALEBİ: " + user.getFullName() + " (" + user.getUsername() + ") şifresini unuttu.");
            n.setCreatedAt(LocalDateTime.now());
            notificationRepository.save(n);

            model.addAttribute("success", "Talebiniz yöneticiye iletildi. Lütfen şifrenizi sıfırlamak için şu numarayla irtibata geçin: 0551 011 86 74");
        } else {
            model.addAttribute("error", "Bu kullanıcı adıyla bir hesap bulunamadı!");
        }
        return "forgot-password";
    }

    @GetMapping("/admin")
    public String adminPanel(HttpSession session, Model model) {
        // Güvenlik kontrolü: Sadece admin girebilir
        if (!"ADMIN".equals(session.getAttribute("loggedInUserRole"))) return "redirect:/";

        model.addAttribute("notifications", notificationRepository.findAll());
        model.addAttribute("questions", questionRepository.findAll());
        model.addAttribute("answers", answerRepository.findAll());
        model.addAttribute("students", userRepository.findAll());
        return "admin";
    }

    @PostMapping("/add-question")
    public String addQuestion(@RequestParam String content, @RequestParam String type, HttpSession session) {
        // Güvenlik kontrolü
        if (!"ADMIN".equals(session.getAttribute("loggedInUserRole"))) return "redirect:/";

        Question q = new Question();
        q.setContent(content);
        q.setType(type);
        questionRepository.save(q);
        return "redirect:/admin";
    }

    @GetMapping("/student")
    public String studentPanel(HttpSession session, Model model) {
        // Güvenlik kontrolü: Sadece öğrenci girebilir
        if (!"STUDENT".equals(session.getAttribute("loggedInUserRole"))) return "redirect:/";

        Long studentId = (Long) session.getAttribute("loggedInUserId");

        model.addAttribute("questions", questionRepository.findAll());

        // ÖNEMLİ DÜZELTME: Öğrenci artık SADECE KENDİ cevaplarını görecek (findAll yerine findByStudentId)
        model.addAttribute("answers", answerRepository.findByStudentId(studentId));
        return "student";
    }

    @PostMapping("/submit-answer")
    public String submitAnswer(@RequestParam Long questionId, @RequestParam String answerText, HttpSession session) {
        // Güvenlik kontrolü
        if (!"STUDENT".equals(session.getAttribute("loggedInUserRole"))) return "redirect:/";

        Long studentId = (Long) session.getAttribute("loggedInUserId");

        Answer a = new Answer();
        a.setQuestionId(questionId);
        a.setAnswerText(answerText);

        // KRİTİK DÜZELTME: Eskiden burada '2L' yazıyordu, veriler karışıyordu. Şimdi gerçek ID alınıyor.
        a.setStudentId(studentId);

        a.setAiNote(aiService.analyzeText(answerText));
        answerRepository.save(a);

        return "redirect:/student";
    }

    // YENİ: Mentör paneli için temel iskelet
    @GetMapping("/mentor")
    public String mentorPanel(HttpSession session, Model model) {
        if (!"MENTOR".equals(session.getAttribute("loggedInUserRole"))) return "redirect:/";

        // İleride buraya mentörün göreceği verileri ekleyeceğiz
        return "mentor";
    }
}