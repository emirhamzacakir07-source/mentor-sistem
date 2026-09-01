package com.example.mentor;

import jakarta.servlet.http.HttpSession;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.time.DayOfWeek;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Controller
public class AppController {

    @Autowired private UserRepository userRepository;
    @Autowired private QuestionRepository questionRepository;
    @Autowired private AnswerRepository answerRepository;
    @Autowired private AiService aiService;
    @Autowired private NotificationRepository notificationRepository;
    @Autowired private MessageRepository messageRepository;
    @Autowired private PasswordEncoder passwordEncoder;

    // --- GENEL GİRİŞ (Öğrenci ve Admin) ---
    @GetMapping("/")
    public String loginPage() { return "index"; }

    @PostMapping("/login")
    public String login(@RequestParam String username, @RequestParam String password, HttpSession session, RedirectAttributes redirectAttributes) {
        User user = userRepository.findByUsername(username);
        if (user != null && passwordEncoder.matches(password, user.getPassword())) {
            if ("MENTOR".equals(user.getRole())) {
                redirectAttributes.addFlashAttribute("error", "Mentör girişleri özel sayfadan yapılmaktadır. Lütfen 'Mentör Girişi' bağlantısını kullanın.");
                return "redirect:/"; // PRG Kuralı
            }

            user.setLastLoginDate(LocalDateTime.now());
            user.setInactiveWarningSent(false);
            userRepository.save(user);

            session.setAttribute("loggedInUserId", user.getId());
            session.setAttribute("loggedInUserRole", user.getRole());
            session.setAttribute("loggedInUsername", user.getUsername());

            if (user.getRole().equals("ADMIN")) return "redirect:/admin";
            else return "redirect:/student";
        }
        redirectAttributes.addFlashAttribute("error", "Kullanıcı adı veya şifre hatalı!");
        return "redirect:/";
    }

    // --- MENTÖR ÖZEL GİRİŞİ ---
    @GetMapping("/mentor-login")
    public String mentorLoginPage() {
        return "mentor-login";
    }

    @PostMapping("/mentor-login")
    public String processMentorLogin(@RequestParam String username, @RequestParam String password, HttpSession session, RedirectAttributes redirectAttributes) {
        User user = userRepository.findByUsername(username);

        if (user != null && "MENTOR".equals(user.getRole()) && passwordEncoder.matches(password, user.getPassword())) {
            if (!user.isApproved()) {
                redirectAttributes.addFlashAttribute("error", "Hesabınız henüz onaylanmamış. Lütfen yönetici onayını bekleyiniz.");
                return "redirect:/mentor-login";
            }
            user.setLastLoginDate(LocalDateTime.now());
            user.setInactiveWarningSent(false);
            userRepository.save(user);

            session.setAttribute("loggedInUserId", user.getId());
            session.setAttribute("loggedInUserRole", user.getRole());
            session.setAttribute("loggedInUsername", user.getUsername());

            return "redirect:/mentor";
        }

        redirectAttributes.addFlashAttribute("error", "Kayıtlı mentör bulunamadı veya şifre hatalı!");
        return "redirect:/mentor-login";
    }

    // --- KAYIT İŞLEMLERİ ---
    @GetMapping("/register")
    public String registerPage() { return "register"; }

    @PostMapping("/register")
    public String registerUser(@ModelAttribute User user, RedirectAttributes redirectAttributes) {
        if (userRepository.findByUsername(user.getUsername()) != null) {
            redirectAttributes.addFlashAttribute("error", "Bu kullanıcı adı zaten alınmış!");
            return "redirect:/register";
        }
        user.setRole("STUDENT");
        user.setApproved(true); // Öğrenciler direkt onaylı
        user.setLastLoginDate(LocalDateTime.now());
        user.setPassword(passwordEncoder.encode(user.getPassword())); // GÜVENLİK: Şifre artık hash'lenerek saklanıyor
        userRepository.save(user);

        redirectAttributes.addFlashAttribute("success", "Kayıt başarılı! Lütfen giriş yapınız.");
        return "redirect:/";
    }

    @GetMapping("/mentor-register")
    public String mentorRegisterPage() { return "mentor-register"; }

    @PostMapping("/mentor-register")
    public String registerMentor(@ModelAttribute User user, RedirectAttributes redirectAttributes) {
        if (userRepository.findByUsername(user.getUsername()) != null) {
            redirectAttributes.addFlashAttribute("error", "Bu kullanıcı adı zaten alınmış!");
            return "redirect:/mentor-register";
        }
        user.setRole("MENTOR");
        user.setApproved(false); // Admin onayı gerekecek
        user.setLastLoginDate(LocalDateTime.now());
        user.setPassword(passwordEncoder.encode(user.getPassword())); // GÜVENLİK: Şifre artık hash'lenerek saklanıyor
        userRepository.save(user);

        redirectAttributes.addFlashAttribute("success", "Başvurunuz alındı. Yönetici onayından sonra giriş yapabilirsiniz.");
        return "redirect:/mentor-register";
    }

    @GetMapping("/logout")
    public String logout(HttpSession session) {
        session.invalidate();
        return "redirect:/";
    }

    @GetMapping("/forgot-password")
    public String forgotPasswordPage() { return "forgot-password"; }

    @PostMapping("/forgot-password")
    public String processForgotPassword(@RequestParam String username, RedirectAttributes redirectAttributes) {
        User user = userRepository.findByUsername(username);
        if (user != null) {
            Notification n = new Notification();
            n.setMessage("🔴 ŞİFRE SIFIRLAMA TALEBİ: " + user.getFullName());
            n.setCreatedAt(LocalDateTime.now());
            notificationRepository.save(n);
            redirectAttributes.addFlashAttribute("success", "Talebiniz yöneticiye iletildi.");
        } else {
            redirectAttributes.addFlashAttribute("error", "Kullanıcı bulunamadı!");
        }
        return "redirect:/forgot-password";
    }

    @PostMapping("/send-message")
    public String sendMessage(@RequestParam Long receiverId, @RequestParam String content, HttpSession session, RedirectAttributes redirectAttributes) {
        Long senderId = (Long) session.getAttribute("loggedInUserId");
        String role = (String) session.getAttribute("loggedInUserRole");

        if (senderId == null) return "redirect:/";

        // GÜVENLİK DÜZELTMESİ: Gönderen kişi sadece KENDİ mentörüne (öğrenciyse)
        // veya KENDİ öğrencisine (mentörse) mesaj atabilir. Bu kontrol olmadan,
        // receiverId form alanı tarayıcıdan değiştirilerek ilgisi olmayan bir
        // kullanıcıya mesaj gönderilebiliyordu.
        boolean allowed = false;
        if ("STUDENT".equals(role)) {
            User student = userRepository.findById(senderId).orElse(null);
            allowed = student != null && receiverId.equals(student.getAssignedMentorId());
        } else if ("MENTOR".equals(role)) {
            User receiver = userRepository.findById(receiverId).orElse(null);
            allowed = receiver != null && "STUDENT".equals(receiver.getRole()) && senderId.equals(receiver.getAssignedMentorId());
        }

        if (!allowed) {
            redirectAttributes.addFlashAttribute("error", "Bu kişiye mesaj gönderme yetkiniz yok.");
            return "MENTOR".equals(role) ? "redirect:/mentor" : "redirect:/student";
        }

        Message msg = new Message();
        msg.setSenderId(senderId);
        msg.setReceiverId(receiverId);
        msg.setContent(content);
        msg.setSentAt(LocalDateTime.now());
        msg.setRead(false);
        messageRepository.save(msg);

        redirectAttributes.addFlashAttribute("successMessage", "Mesaj başarıyla gönderildi!");

        if ("MENTOR".equals(role)) return "redirect:/mentor";
        else return "redirect:/student";
    }

    // --- ADMİN PANELİ VE İŞLEMLERİ ---
    @GetMapping("/admin")
    public String adminPanel(HttpSession session, Model model) {
        if (!"ADMIN".equals(session.getAttribute("loggedInUserRole"))) return "redirect:/";

        model.addAttribute("notifications", notificationRepository.findAll());
        model.addAttribute("questions", questionRepository.findAll());
        model.addAttribute("users", userRepository.findAll());

        // DÜZELTME: findAll() teorik olarak Iterable döndürür; (List) diye zorla
        // çevirmek yerine güvenli şekilde listeye topluyoruz. Böylece repository
        // implementasyonu değişse bile ClassCastException riski kalmıyor.
        List<Answer> allAnswers = new ArrayList<>();
        answerRepository.findAll().forEach(allAnswers::add);

        List<Map<String, Object>> adminAnswers = new ArrayList<>();
        for (Answer ans : allAnswers) {
            Map<String, Object> map = new HashMap<>();
            map.put("id", ans.getId());
            User std = userRepository.findById(ans.getStudentId()).orElse(null);
            map.put("studentName", std != null ? std.getFullName() : "Silinmiş Öğrenci");
            Question q = questionRepository.findById(ans.getQuestionId()).orElse(null);
            map.put("questionContent", q != null ? q.getContent() : "Silinmiş Soru");
            map.put("answerText", ans.getAnswerText());
            map.put("aiNote", ans.getAiNote());
            map.put("mentorScore", ans.getMentorScore());
            adminAnswers.add(map);
        }
        model.addAttribute("adminAnswers", adminAnswers);

        List<Message> allMessages = messageRepository.findAllByOrderBySentAtDesc();
        List<Map<String, Object>> adminMessages = new ArrayList<>();
        for (Message m : allMessages) {
            Map<String, Object> map = new HashMap<>();
            User sender = userRepository.findById(m.getSenderId()).orElse(null);
            User receiver = userRepository.findById(m.getReceiverId()).orElse(null);
            map.put("senderName", sender != null ? sender.getFullName() + " (" + sender.getRole() + ")" : "Bilinmeyen");
            map.put("receiverName", receiver != null ? receiver.getFullName() + " (" + receiver.getRole() + ")" : "Bilinmeyen");
            map.put("content", m.getContent());
            map.put("sentAt", m.getSentAt());
            adminMessages.add(map);
        }
        model.addAttribute("adminMessages", adminMessages);

        return "admin";
    }

    @PostMapping("/add-question")
    public String addQuestion(
            @RequestParam(required = false) Long id,
            @RequestParam String content, @RequestParam String type,
            @RequestParam(required = false, defaultValue = "false") boolean isTask,
            @RequestParam(required = false) String category,
            @RequestParam(required = false) String maxPoints,
            @RequestParam(required = false) String optionA_text, @RequestParam(required = false) String optionA_point,
            @RequestParam(required = false) String optionB_text, @RequestParam(required = false) String optionB_point,
            @RequestParam(required = false) String optionC_text, @RequestParam(required = false) String optionC_point,
            @RequestParam(required = false) String optionD_text, @RequestParam(required = false) String optionD_point,
            @RequestParam(required = false, defaultValue = "false") boolean allowMultipleSelections,
            HttpSession session, RedirectAttributes redirectAttributes) {

        if (!"ADMIN".equals(session.getAttribute("loggedInUserRole"))) return "redirect:/";

        Question q = (id != null) ? questionRepository.findById(id).orElse(new Question()) : new Question();

        q.setContent(content); q.setType(type); q.setTask(isTask); q.setCategory(category);
        q.setAllowMultipleSelections(allowMultipleSelections);

        try { q.setMaxPoints((maxPoints != null && !maxPoints.trim().isEmpty()) ? Integer.parseInt(maxPoints) : 0); } catch (Exception e) { q.setMaxPoints(0); }

        if ("COKTAN_SECMELI".equals(type)) {
            q.setOptionA(optionA_text);
            try { q.setOptionAPoint((optionA_point != null && !optionA_point.trim().isEmpty()) ? Integer.parseInt(optionA_point) : 0); } catch (Exception e) { q.setOptionAPoint(0); }
            q.setOptionB(optionB_text);
            try { q.setOptionBPoint((optionB_point != null && !optionB_point.trim().isEmpty()) ? Integer.parseInt(optionB_point) : 0); } catch (Exception e) { q.setOptionBPoint(0); }
            q.setOptionC(optionC_text);
            try { q.setOptionCPoint((optionC_point != null && !optionC_point.trim().isEmpty()) ? Integer.parseInt(optionC_point) : 0); } catch (Exception e) { q.setOptionCPoint(0); }
            q.setOptionD(optionD_text);
            try { q.setOptionDPoint((optionD_point != null && !optionD_point.trim().isEmpty()) ? Integer.parseInt(optionD_point) : 0); } catch (Exception e) { q.setOptionDPoint(0); }
        } else {
            q.setOptionA(null); q.setOptionAPoint(0);
            q.setOptionB(null); q.setOptionBPoint(0);
            q.setOptionC(null); q.setOptionCPoint(0);
            q.setOptionD(null); q.setOptionDPoint(0);
        }

        questionRepository.save(q);
        redirectAttributes.addFlashAttribute("successMessage", "Soru başarıyla kaydedildi.");
        return "redirect:/admin";
    }

    @PostMapping("/admin/delete-question")
    public String deleteQuestion(@RequestParam Long id, HttpSession session, RedirectAttributes redirectAttributes) {
        if (!"ADMIN".equals(session.getAttribute("loggedInUserRole"))) return "redirect:/";
        List<Answer> answers = answerRepository.findByQuestionId(id);
        answerRepository.deleteAll(answers);
        questionRepository.deleteById(id);
        redirectAttributes.addFlashAttribute("successMessage", "Soru ve bağlı cevaplar silindi.");
        return "redirect:/admin";
    }

    @PostMapping("/admin/assign-mentor")
    public String assignMentor(@RequestParam Long mentor_id, @RequestParam Long student_id, HttpSession session, RedirectAttributes redirectAttributes) {
        if (!"ADMIN".equals(session.getAttribute("loggedInUserRole"))) return "redirect:/";
        User student = userRepository.findById(student_id).orElse(null);
        if (student != null && "STUDENT".equals(student.getRole())) {
            student.setAssignedMentorId(mentor_id);
            userRepository.save(student);
            redirectAttributes.addFlashAttribute("successMessage", "Mentör başarıyla atandı.");
        }
        return "redirect:/admin";
    }

    @PostMapping("/admin/approve-mentor")
    public String approveMentor(@RequestParam Long mentor_id, HttpSession session, RedirectAttributes redirectAttributes) {
        if (!"ADMIN".equals(session.getAttribute("loggedInUserRole"))) return "redirect:/";
        User mentor = userRepository.findById(mentor_id).orElse(null);
        if (mentor != null && "MENTOR".equals(mentor.getRole())) {
            mentor.setApproved(true);
            userRepository.save(mentor);
            redirectAttributes.addFlashAttribute("successMessage", "Mentör hesabı onaylandı.");
        }
        return "redirect:/admin";
    }

    // --- MENTÖR PANELİ ---
    @GetMapping("/mentor")
    public String mentorPanel(HttpSession session, Model model) {
        if (!"MENTOR".equals(session.getAttribute("loggedInUserRole"))) return "redirect:/";
        Long mentorId = (Long) session.getAttribute("loggedInUserId");

        // DÜZELTME: (List) cast riski kaldırıldı, güvenli şekilde listeye topluyoruz.
        List<User> allUsers = new ArrayList<>();
        userRepository.findAll().forEach(allUsers::add);

        List<User> myStudents = allUsers.stream().filter(u -> "STUDENT".equals(u.getRole()) && mentorId.equals(u.getAssignedMentorId())).collect(Collectors.toList());
        model.addAttribute("myStudents", myStudents);

        List<Map<String, Object>> studentAnswers = new ArrayList<>();
        for (User student : myStudents) {
            List<Answer> answers = answerRepository.findByStudentId(student.getId());
            for (Answer ans : answers) {
                Map<String, Object> map = new HashMap<>();
                map.put("id", ans.getId());
                map.put("studentId", student.getId()); // Toplu Rapor için ID eklendi
                map.put("studentName", student.getFullName());
                map.put("answerText", ans.getAnswerText());
                map.put("aiNote", ans.getAiNote());
                map.put("mentorFeedback", ans.getMentorFeedback());
                Question q = questionRepository.findById(ans.getQuestionId()).orElse(null);
                map.put("questionContent", q != null ? q.getContent() : "Soru Silinmiş");
                studentAnswers.add(map);
            }
        }
        model.addAttribute("studentAnswers", studentAnswers);

        List<Message> myMessages = messageRepository.findBySenderIdOrReceiverIdOrderBySentAtAsc(mentorId, mentorId);
        List<Map<String, Object>> chatMessages = new ArrayList<>();
        for (Message m : myMessages) {
            Map<String, Object> map = new HashMap<>();
            map.put("content", m.getContent());
            map.put("sentAt", m.getSentAt());
            map.put("isMine", m.getSenderId().equals(mentorId));

            Long otherUserId = m.getSenderId().equals(mentorId) ? m.getReceiverId() : m.getSenderId();
            User otherUser = userRepository.findById(otherUserId).orElse(null);
            map.put("otherUserName", otherUser != null ? otherUser.getFullName() : "Bilinmeyen");

            chatMessages.add(map);
        }
        model.addAttribute("chatMessages", chatMessages);

        return "mentor";
    }

    @PostMapping("/mentor/submit-feedback")
    public String submitFeedback(@RequestParam Long answerId, @RequestParam Integer mentorScore, @RequestParam String mentorFeedback, HttpSession session, RedirectAttributes redirectAttributes) {
        if (!"MENTOR".equals(session.getAttribute("loggedInUserRole"))) return "redirect:/";
        Long mentorId = (Long) session.getAttribute("loggedInUserId");

        Answer ans = answerRepository.findById(answerId).orElse(null);
        if (ans == null) {
            redirectAttributes.addFlashAttribute("error", "Cevap bulunamadı.");
            return "redirect:/mentor";
        }

        // GÜVENLİK DÜZELTMESİ (IDOR): Bu cevap gerçekten bu mentöre atanmış bir
        // öğrenciye mi ait, kontrol ediliyor. Kontrol olmadan, bir mentör
        // answerId'yi değiştirerek başka mentörlerin öğrencilerini puanlayabilirdi.
        User student = userRepository.findById(ans.getStudentId()).orElse(null);
        if (student == null || !mentorId.equals(student.getAssignedMentorId())) {
            redirectAttributes.addFlashAttribute("error", "Bu cevabı değerlendirme yetkiniz yok.");
            return "redirect:/mentor";
        }

        // GÜVENLİK/VERİ DÜZELTMESİ: Puan 0-100 aralığına sabitlendi.
        int clampedScore = Math.max(0, Math.min(100, mentorScore));

        ans.setMentorScore(clampedScore);
        ans.setMentorFeedback(mentorFeedback);
        answerRepository.save(ans);
        redirectAttributes.addFlashAttribute("successMessage", "Değerlendirme kaydedildi.");
        return "redirect:/mentor";
    }

    @PostMapping("/mentor/send-announcement")
    public String sendAnnouncement(@RequestParam String message, HttpSession session, RedirectAttributes redirectAttributes) {
        if (!"MENTOR".equals(session.getAttribute("loggedInUserRole"))) return "redirect:/";
        String mentorName = (String) session.getAttribute("loggedInUsername");
        Notification n = new Notification();
        n.setMessage("📢 DUYURU (" + mentorName + "): " + message);
        n.setCreatedAt(LocalDateTime.now());
        notificationRepository.save(n);
        redirectAttributes.addFlashAttribute("successMessage", "Duyuru öğrencilere iletildi.");
        return "redirect:/mentor";
    }

    // Mentörün AI Öğrenci Raporunu Oluşturması
    @PostMapping("/mentor/ai-report")
    public String generateAiReport(@RequestParam Long studentId, HttpSession session, RedirectAttributes redirectAttributes) {
        if (!"MENTOR".equals(session.getAttribute("loggedInUserRole"))) return "redirect:/";
        Long mentorId = (Long) session.getAttribute("loggedInUserId");

        User student = userRepository.findById(studentId).orElse(null);

        // GÜVENLİK DÜZELTMESİ (IDOR): studentId'nin gerçekten bu mentöre atanmış
        // olup olmadığı kontrol ediliyor. Aksi halde bir mentör, studentId'yi
        // değiştirerek başka mentörlerin öğrencilerinin AI raporunu görebilirdi.
        if (student == null || !mentorId.equals(student.getAssignedMentorId())) {
            redirectAttributes.addFlashAttribute("error", "Bu öğrenci için rapor oluşturma yetkiniz yok.");
            return "redirect:/mentor";
        }

        List<Answer> last5Answers = answerRepository.findTop5ByStudentIdOrderByCreatedAtDesc(studentId);
        List<String> answerTexts = last5Answers.stream().map(Answer::getAnswerText).collect(Collectors.toList());

        String report = aiService.analyzeStudentPerformance(student.getFullName(), answerTexts);

        // Sonucu SweetAlert ile göstermek için FlashAttribute'a ekliyoruz
        redirectAttributes.addFlashAttribute("aiReportMessage", report);
        return "redirect:/mentor";
    }

    // Mentör Toplu Toplantı Planlama
    @PostMapping("/mentor/schedule-meeting")
    public String scheduleMeeting(@RequestParam String meetingDate, @RequestParam String meetingLink, HttpSession session, RedirectAttributes redirectAttributes) {
        if (!"MENTOR".equals(session.getAttribute("loggedInUserRole"))) return "redirect:/";

        String mentorName = (String) session.getAttribute("loggedInUsername");
        Notification n = new Notification();
        n.setMessage("📅 TOPLANTI (" + mentorName + "): Yeni bir mentör toplantısı planlandı! Tarih: " + meetingDate + " Link: " + meetingLink);
        n.setCreatedAt(LocalDateTime.now());
        notificationRepository.save(n);

        redirectAttributes.addFlashAttribute("successMessage", "Toplantı planlandı ve tüm öğrencilere duyuruldu.");
        return "redirect:/mentor";
    }

    // --- ÖĞRENCİ PANELİ VE CEVAP GÖNDERME ---
    @GetMapping("/student")
    public String studentPanel(HttpSession session, Model model) {
        if (!"STUDENT".equals(session.getAttribute("loggedInUserRole"))) return "redirect:/";

        Long studentId = (Long) session.getAttribute("loggedInUserId");
        User student = userRepository.findById(studentId).orElse(null);

        User mentor = null;
        if (student != null && student.getAssignedMentorId() != null) {
            mentor = userRepository.findById(student.getAssignedMentorId()).orElse(null);
        }

        // DÜZELTME: (List) cast riski kaldırıldı, güvenli şekilde listeye topluyoruz.
        List<Question> allQuestions = new ArrayList<>();
        questionRepository.findAll().forEach(allQuestions::add);

        List<Question> tasks = allQuestions.stream().filter(Question::isTask).collect(Collectors.toList());
        List<Question> questions = allQuestions.stream().filter(q -> !q.isTask()).collect(Collectors.toList());

        List<Answer> answers = answerRepository.findByStudentId(studentId);
        int totalScore = answers.stream().filter(a -> a.getMentorScore() != null).mapToInt(Answer::getMentorScore).sum();

        Map<Long, Answer> answerMap = new HashMap<>();
        for (Answer a : answers) { answerMap.put(a.getQuestionId(), a); }

        // Cuma Soru Kilidi Kontrolü
        boolean isLocked = false;
        LocalDateTime now = LocalDateTime.now();
        // Cuma akşam 18:00'den Pazar gecesine kadar kilitli
        if ((now.getDayOfWeek() == DayOfWeek.FRIDAY && now.getHour() >= 18) ||
                now.getDayOfWeek() == DayOfWeek.SATURDAY ||
                now.getDayOfWeek() == DayOfWeek.SUNDAY) {
            isLocked = true;
        }

        model.addAttribute("isLocked", isLocked);
        model.addAttribute("student", student);
        model.addAttribute("mentor", mentor);
        model.addAttribute("tasks", tasks);
        model.addAttribute("questions", questions);
        model.addAttribute("answerMap", answerMap);
        model.addAttribute("totalScore", totalScore);
        model.addAttribute("notifications", notificationRepository.findAll());

        List<Message> myMessages = messageRepository.findBySenderIdOrReceiverIdOrderBySentAtAsc(studentId, studentId);
        model.addAttribute("chatMessages", myMessages);

        return "student";
    }

    // ÖĞRENCİ CEVAPLAMA MERKEZİ
    @PostMapping("/submit-answer")
    public String submitAnswer(
            @RequestParam Long questionId,
            @RequestParam(required = false) String answerText,
            @RequestParam(required = false) List<String> selectedOptions,
            HttpSession session, RedirectAttributes redirectAttributes) {

        if (!"STUDENT".equals(session.getAttribute("loggedInUserRole"))) return "redirect:/";

        // Güvenlik - Kilitli saatlerde POST isteği gelirse reddet
        LocalDateTime now = LocalDateTime.now();
        if ((now.getDayOfWeek() == DayOfWeek.FRIDAY && now.getHour() >= 18) ||
                now.getDayOfWeek() == DayOfWeek.SATURDAY ||
                now.getDayOfWeek() == DayOfWeek.SUNDAY) {
            redirectAttributes.addFlashAttribute("error", "Sistem şu an kilitli. Hafta sonu cevap gönderimi yapılamaz.");
            return "redirect:/student";
        }

        Long studentId = (Long) session.getAttribute("loggedInUserId");
        User student = userRepository.findById(studentId).orElse(null);
        Question question = questionRepository.findById(questionId).orElse(null);

        if (question == null) return "redirect:/student";

        Answer a = new Answer();
        a.setQuestionId(questionId);
        a.setStudentId(studentId);
        a.setAnswerText(answerText != null ? answerText : "");
        a.setCreatedAt(LocalDateTime.now());

        // ÇOKTAN SEÇMELİ SİSTEMİ VE OTOMATİK PUANLAMA
        if ("COKTAN_SECMELI".equals(question.getType())) {
            int totalScore = 0;

            if (selectedOptions != null && !selectedOptions.isEmpty()) {
                a.setSelectedOptions(String.join(",", selectedOptions));

                if (selectedOptions.contains("A") && question.getOptionAPoint() != null) totalScore += question.getOptionAPoint();
                if (selectedOptions.contains("B") && question.getOptionBPoint() != null) totalScore += question.getOptionBPoint();
                if (selectedOptions.contains("C") && question.getOptionCPoint() != null) totalScore += question.getOptionCPoint();
                if (selectedOptions.contains("D") && question.getOptionDPoint() != null) totalScore += question.getOptionDPoint();
            } else {
                a.setSelectedOptions("");
            }

            a.setMentorScore(totalScore);

        } else {
            a.setMentorScore(null);
        }

        a.setAiNote(aiService.analyzeText(answerText != null ? answerText : ""));
        answerRepository.save(a);

        if (student != null) {
            Notification n = new Notification();
            n.setMessage("🤖 SİSTEM BİLDİRİMİ: " + student.getFullName() + " isimli öğrenci yeni bir soru/vazife yanıtladı.");
            n.setCreatedAt(LocalDateTime.now());
            notificationRepository.save(n);
        }

        redirectAttributes.addFlashAttribute("successMessage", "Cevabın başarıyla sisteme kaydedildi!");
        return "redirect:/student";
    }
}
