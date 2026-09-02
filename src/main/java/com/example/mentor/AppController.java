package com.example.mentor;

import jakarta.servlet.http.HttpServletResponse;
import jakarta.servlet.http.HttpSession;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.io.PrintWriter;
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

    // --- ZAMAN KİLİDİ METODU (Sadece Perşembe 18:00 - Cuma 18:00 arası TRUE döner) ---
    private boolean isSystemOpen(LocalDateTime time) {
        DayOfWeek day = time.getDayOfWeek();
        int hour = time.getHour();
        return (day == DayOfWeek.THURSDAY && hour >= 18) || (day == DayOfWeek.FRIDAY && hour < 18);
    }

    @GetMapping("/")
    public String loginPage() { return "index"; }

    @PostMapping("/login")
    public String login(@RequestParam String username, @RequestParam String password, HttpSession session, RedirectAttributes redirectAttributes) {
        // Önlem 1: Boşlukları temizle
        username = username.trim();
        password = password.trim();

        User user = userRepository.findByUsername(username);
        if (user != null && passwordEncoder.matches(password, user.getPassword())) {
            if ("MENTOR".equals(user.getRole())) {
                redirectAttributes.addFlashAttribute("error", "Mentör girişleri özel sayfadan yapılmaktadır. Lütfen 'Mentör Girişi' bağlantısını kullanın.");
                return "redirect:/";
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

    @GetMapping("/mentor-login")
    public String mentorLoginPage() { return "mentor-login"; }

    @PostMapping("/mentor-login")
    public String processMentorLogin(@RequestParam String username, @RequestParam String password, HttpSession session, RedirectAttributes redirectAttributes) {
        username = username.trim();
        password = password.trim();

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

    @GetMapping("/register")
    public String registerPage() { return "register"; }

    @PostMapping("/register")
    public String registerUser(@ModelAttribute User user, @RequestParam String passwordConfirm, HttpSession session, RedirectAttributes redirectAttributes) {
        user.setUsername(user.getUsername().trim());
        user.setPassword(user.getPassword().trim());
        passwordConfirm = passwordConfirm.trim();

        if (!user.getPassword().equals(passwordConfirm)) {
            redirectAttributes.addFlashAttribute("error", "Girdiğiniz şifreler birbiriyle uyuşmuyor!");
            return "redirect:/register";
        }
        if (userRepository.findByUsername(user.getUsername()) != null) {
            redirectAttributes.addFlashAttribute("error", "Bu kullanıcı adı zaten alınmış!");
            return "redirect:/register";
        }
        user.setRole("STUDENT");
        user.setApproved(true);
        user.setLastLoginDate(LocalDateTime.now());
        user.setPassword(passwordEncoder.encode(user.getPassword()));

        // BUG FIX: Veritabanına anında yazıp, oturumu sıfırlıyoruz.
        userRepository.saveAndFlush(user);
        session.invalidate();

        redirectAttributes.addFlashAttribute("success", "Kayıt başarılı! Lütfen giriş yapınız.");
        return "redirect:/?success=true";
    }

    @GetMapping("/mentor-register")
    public String mentorRegisterPage() { return "mentor-register"; }

    @PostMapping("/mentor-register")
    public String registerMentor(@ModelAttribute User user, @RequestParam String passwordConfirm, HttpSession session, RedirectAttributes redirectAttributes) {
        user.setUsername(user.getUsername().trim());
        user.setPassword(user.getPassword().trim());
        passwordConfirm = passwordConfirm.trim();

        if (!user.getPassword().equals(passwordConfirm)) {
            redirectAttributes.addFlashAttribute("error", "Girdiğiniz şifreler birbiriyle uyuşmuyor!");
            return "redirect:/mentor-register";
        }
        if (userRepository.findByUsername(user.getUsername()) != null) {
            redirectAttributes.addFlashAttribute("error", "Bu kullanıcı adı zaten alınmış!");
            return "redirect:/mentor-register";
        }
        user.setRole("MENTOR");
        user.setApproved(false);
        user.setLastLoginDate(LocalDateTime.now());
        user.setPassword(passwordEncoder.encode(user.getPassword()));

        userRepository.saveAndFlush(user);
        session.invalidate();

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
    public String processForgotPassword(@RequestParam String phone, RedirectAttributes redirectAttributes) {
        List<User> allUsers = new ArrayList<>();
        userRepository.findAll().forEach(allUsers::add);

        User foundUser = allUsers.stream()
                .filter(u -> phone.equals(u.getPersonalPhone()) || phone.equals(u.getParentPhone()))
                .findFirst().orElse(null);

        if (foundUser != null) {
            Notification n = new Notification();
            n.setMessage("🔴 ŞİFRE SIFIRLAMA TALEBİ: " + foundUser.getFullName() + " (Tel: " + phone + ")");
            n.setCreatedAt(LocalDateTime.now());
            notificationRepository.save(n);
            redirectAttributes.addFlashAttribute("success", "Talebiniz yöneticiye iletildi. Yeni şifreniz numaranıza gönderilecektir.");
        } else {
            redirectAttributes.addFlashAttribute("error", "Bu telefon numarasına ait bir kayıt bulunamadı!");
        }
        return "redirect:/forgot-password";
    }

    @PostMapping("/send-message")
    public String sendMessage(@RequestParam Long receiverId, @RequestParam String content, HttpSession session, RedirectAttributes redirectAttributes) {
        Long senderId = (Long) session.getAttribute("loggedInUserId");
        String role = (String) session.getAttribute("loggedInUserRole");
        if (senderId == null) return "redirect:/";

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
        return "MENTOR".equals(role) ? "redirect:/mentor" : "redirect:/student";
    }

    // --- ADMİN PANELİ ---
    @GetMapping("/admin")
    public String adminPanel(HttpSession session, Model model) {
        if (!"ADMIN".equals(session.getAttribute("loggedInUserRole"))) return "redirect:/";

        model.addAttribute("notifications", notificationRepository.findAll());
        model.addAttribute("questions", questionRepository.findAll());

        List<User> allUsers = new ArrayList<>();
        userRepository.findAll().forEach(allUsers::add);
        model.addAttribute("users", allUsers);

        List<Answer> allAnswers = new ArrayList<>();
        answerRepository.findAll().forEach(allAnswers::add);

        Map<User, Integer> studentScores = new HashMap<>();
        for(User u : allUsers) {
            if("STUDENT".equals(u.getRole())) { studentScores.put(u, 0); }
        }
        for(Answer a : allAnswers) {
            if(a.isMonthlyReset() != null && a.isMonthlyReset()) continue;

            if(a.getMentorScore() != null) {
                User student = userRepository.findById(a.getStudentId()).orElse(null);
                if(student != null && studentScores.containsKey(student)) {
                    studentScores.put(student, studentScores.get(student) + a.getMentorScore());
                }
            }
        }
        List<Map<String, Object>> leaderboard = new ArrayList<>();
        for(Map.Entry<User, Integer> entry : studentScores.entrySet()) {
            Map<String, Object> lMap = new HashMap<>();
            lMap.put("studentName", entry.getKey().getFullName());
            lMap.put("score", entry.getValue());
            leaderboard.add(lMap);
        }
        leaderboard.sort((m1, m2) -> ((Integer) m2.get("score")).compareTo((Integer) m1.get("score")));
        model.addAttribute("leaderboard", leaderboard);

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
            map.put("isMonthlyReset", ans.isMonthlyReset());
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

    // YENİ: Excel İndirme Metodu
    @GetMapping("/admin/export-users")
    public void exportUsersToCSV(HttpServletResponse response, HttpSession session) throws Exception {
        if (!"ADMIN".equals(session.getAttribute("loggedInUserRole"))) return;

        response.setContentType("text/csv; charset=UTF-8");
        response.setHeader("Content-Disposition", "attachment; filename=\"dijital_gelisim_kullanicilar.csv\"");

        PrintWriter writer = response.getWriter();
        writer.write('\uFEFF'); // Türkçe Karakter Desteği (BOM)
        writer.println("Ad Soyad,Kullanici Adi,Rol,Telefon,Okul,Sinif veya Bolge,Atanan Mentor ID");

        for (User u : userRepository.findAll()) {
            String phone = u.getPersonalPhone() != null ? u.getPersonalPhone() : (u.getParentPhone() != null ? u.getParentPhone() : "Yok");
            String detail = "MENTOR".equals(u.getRole()) ? (u.getRegion() != null ? u.getRegion() : "") : (u.getGradeClass() != null ? u.getGradeClass() : "");
            String school = u.getSchool() != null ? u.getSchool() : "";
            String mentorId = u.getAssignedMentorId() != null ? u.getAssignedMentorId().toString() : "";

            writer.printf("%s,%s,%s,%s,%s,%s,%s\n",
                    u.getFullName(), u.getUsername(), u.getRole(), phone, school, detail, mentorId);
        }
    }

    // YENİ: Admin Şifre Sıfırlama
    @PostMapping("/admin/reset-password")
    public String adminResetPassword(@RequestParam Long userId, @RequestParam String newPassword, HttpSession session, RedirectAttributes redirectAttributes) {
        if (!"ADMIN".equals(session.getAttribute("loggedInUserRole"))) return "redirect:/";

        User user = userRepository.findById(userId).orElse(null);
        if (user != null) {
            user.setPassword(passwordEncoder.encode(newPassword.trim()));
            userRepository.save(user);
            redirectAttributes.addFlashAttribute("successMessage", user.getUsername() + " adlı kullanıcının şifresi başarıyla yenilendi.");
        } else {
            redirectAttributes.addFlashAttribute("error", "Kullanıcı bulunamadı.");
        }
        return "redirect:/admin";
    }

    // YENİ: Admin AI Karne Çıkarma (Mentör yetkisi Admin'e de verildi)
    @PostMapping("/admin/ai-report")
    public String adminAiReport(@RequestParam Long studentId, HttpSession session, RedirectAttributes redirectAttributes) {
        if (!"ADMIN".equals(session.getAttribute("loggedInUserRole"))) return "redirect:/";

        User student = userRepository.findById(studentId).orElse(null);
        if (student != null && "STUDENT".equals(student.getRole())) {
            List<Answer> last5Answers = answerRepository.findTop5ByStudentIdOrderByCreatedAtDesc(studentId);
            List<String> answerTexts = last5Answers.stream().map(Answer::getAnswerText).collect(Collectors.toList());
            String report = aiService.analyzeStudentPerformance(student.getFullName(), answerTexts);

            // Sonucu adminin ekranında büyük popup ile göstermek için
            redirectAttributes.addFlashAttribute("successMessage", "YAPAY ZEKA ANALİZİ:\n" + report);
        } else {
            redirectAttributes.addFlashAttribute("error", "Öğrenci bulunamadı.");
        }
        return "redirect:/admin";
    }

    @PostMapping("/admin/reset-monthly-scores")
    public String resetMonthlyScores(HttpSession session, RedirectAttributes redirectAttributes) {
        if (!"ADMIN".equals(session.getAttribute("loggedInUserRole"))) return "redirect:/";

        List<Answer> allAnswers = new ArrayList<>();
        answerRepository.findAll().forEach(allAnswers::add);

        for(Answer a : allAnswers) {
            a.setMonthlyReset(true);
        }
        answerRepository.saveAll(allAnswers);

        redirectAttributes.addFlashAttribute("successMessage", "🏆 Yeni aya başarıyla geçildi! Tüm öğrencilerin aktif liderlik puanları 0'landı.");
        return "redirect:/admin";
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
            @RequestParam(required = false, defaultValue = "1") Integer weekNumber,
            @RequestParam(required = false, defaultValue = "false") Boolean isEighthGradeOnly,
            HttpSession session, RedirectAttributes redirectAttributes) {

        if (!"ADMIN".equals(session.getAttribute("loggedInUserRole"))) return "redirect:/";

        Question q = (id != null) ? questionRepository.findById(id).orElse(new Question()) : new Question();
        q.setContent(content); q.setType(type); q.setTask(isTask); q.setCategory(category);
        q.setAllowMultipleSelections(allowMultipleSelections);
        q.setWeekNumber(weekNumber);
        q.setEighthGradeOnly(isEighthGradeOnly);

        if (q.getId() == null || q.getCreatedAt() == null) { q.setCreatedAt(LocalDateTime.now()); }
        try { q.setMaxPoints((maxPoints != null && !maxPoints.trim().isEmpty()) ? Integer.parseInt(maxPoints) : 0); } catch (Exception e) { q.setMaxPoints(0); }

        if ("COKTAN_SECMELI".equals(type)) {
            q.setOptionA(optionA_text); try { q.setOptionAPoint((optionA_point != null && !optionA_point.trim().isEmpty()) ? Integer.parseInt(optionA_point) : 0); } catch (Exception e) { q.setOptionAPoint(0); }
            q.setOptionB(optionB_text); try { q.setOptionBPoint((optionB_point != null && !optionB_point.trim().isEmpty()) ? Integer.parseInt(optionB_point) : 0); } catch (Exception e) { q.setOptionBPoint(0); }
            q.setOptionC(optionC_text); try { q.setOptionCPoint((optionC_point != null && !optionC_point.trim().isEmpty()) ? Integer.parseInt(optionC_point) : 0); } catch (Exception e) { q.setOptionCPoint(0); }
            q.setOptionD(optionD_text); try { q.setOptionDPoint((optionD_point != null && !optionD_point.trim().isEmpty()) ? Integer.parseInt(optionD_point) : 0); } catch (Exception e) { q.setOptionDPoint(0); }
        } else {
            q.setOptionA(null); q.setOptionAPoint(0); q.setOptionB(null); q.setOptionBPoint(0);
            q.setOptionC(null); q.setOptionCPoint(0); q.setOptionD(null); q.setOptionDPoint(0);
        }
        questionRepository.save(q);
        redirectAttributes.addFlashAttribute("successMessage", "Soru/Vazife başarıyla kaydedildi.");
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
    public String assignMentor(
            @RequestParam(name = "mentor_id", required = false) Long mentorIdAltCizgi,
            @RequestParam(name = "mentorId", required = false) Long mentorIdDuzz,
            @RequestParam(name = "student_id", required = false) Long studentIdAltCizgi,
            @RequestParam(name = "studentId", required = false) Long studentIdDuzz,
            HttpSession session, RedirectAttributes redirectAttributes) {
        if (!"ADMIN".equals(session.getAttribute("loggedInUserRole"))) return "redirect:/";

        Long finalMentorId = (mentorIdAltCizgi != null) ? mentorIdAltCizgi : mentorIdDuzz;
        Long finalStudentId = (studentIdAltCizgi != null) ? studentIdAltCizgi : studentIdDuzz;

        if (finalMentorId == null || finalStudentId == null) {
            redirectAttributes.addFlashAttribute("error", "Lütfen atama yapmak için hem bir öğrenci hem de bir mentör seçin.");
            return "redirect:/admin";
        }
        User student = userRepository.findById(finalStudentId).orElse(null);
        if (student != null && "STUDENT".equals(student.getRole())) {
            student.setAssignedMentorId(finalMentorId);
            userRepository.save(student);
            redirectAttributes.addFlashAttribute("successMessage", "Mentör başarıyla atandı!");
        } else {
            redirectAttributes.addFlashAttribute("error", "Öğrenci bulunamadı veya geçersiz.");
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
                map.put("studentId", student.getId());
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
    public String submitFeedback(@RequestParam Long answerId, @RequestParam String mentorFeedback, HttpSession session, RedirectAttributes redirectAttributes) {
        if (!"MENTOR".equals(session.getAttribute("loggedInUserRole"))) return "redirect:/";
        Long mentorId = (Long) session.getAttribute("loggedInUserId");

        Answer ans = answerRepository.findById(answerId).orElse(null);
        if (ans == null) { redirectAttributes.addFlashAttribute("error", "Cevap bulunamadı."); return "redirect:/mentor"; }

        User student = userRepository.findById(ans.getStudentId()).orElse(null);
        if (student == null || !mentorId.equals(student.getAssignedMentorId())) {
            redirectAttributes.addFlashAttribute("error", "Bu cevabı değerlendirme yetkiniz yok.");
            return "redirect:/mentor";
        }

        ans.setMentorFeedback(mentorFeedback);
        answerRepository.save(ans);
        redirectAttributes.addFlashAttribute("successMessage", "Geri bildirim başarıyla öğrenciye iletildi.");
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

    @PostMapping("/mentor/ai-report")
    public String generateAiReport(@RequestParam Long studentId, HttpSession session, RedirectAttributes redirectAttributes) {
        if (!"MENTOR".equals(session.getAttribute("loggedInUserRole"))) return "redirect:/";
        Long mentorId = (Long) session.getAttribute("loggedInUserId");

        User student = userRepository.findById(studentId).orElse(null);
        if (student == null || !mentorId.equals(student.getAssignedMentorId())) {
            redirectAttributes.addFlashAttribute("error", "Bu öğrenci için rapor oluşturma yetkiniz yok.");
            return "redirect:/mentor";
        }

        List<Answer> last5Answers = answerRepository.findTop5ByStudentIdOrderByCreatedAtDesc(studentId);
        List<String> answerTexts = last5Answers.stream().map(Answer::getAnswerText).collect(Collectors.toList());
        String report = aiService.analyzeStudentPerformance(student.getFullName(), answerTexts);
        redirectAttributes.addFlashAttribute("aiReportMessage", report);
        return "redirect:/mentor";
    }

    @PostMapping("/mentor/schedule-meeting")
    public String scheduleMeeting(@RequestParam String meetingDate, @RequestParam String meetingLink, HttpSession session, RedirectAttributes redirectAttributes) {
        if (!"MENTOR".equals(session.getAttribute("loggedInUserRole"))) return "redirect:/";
        String mentorName = (String) session.getAttribute("loggedInUsername");
        Notification n = new Notification();
        n.setMessage("📅 TOPLANTI (" + mentorName + "): Yeni bir mentör toplantısı planlandı! Tarih: " + meetingDate + " Link: " + meetingLink);
        n.setCreatedAt(LocalDateTime.now());
        notificationRepository.save(n);
        redirectAttributes.addFlashAttribute("successMessage", "Toplantı planlandı ve duyuruldu.");
        return "redirect:/mentor";
    }

    // --- ÖĞRENCİ PANELİ ---
    @GetMapping("/student")
    public String studentPanel(HttpSession session, Model model) {
        if (!"STUDENT".equals(session.getAttribute("loggedInUserRole"))) return "redirect:/";
        Long studentId = (Long) session.getAttribute("loggedInUserId");
        User student = userRepository.findById(studentId).orElse(null);

        User mentor = null;
        if (student != null && student.getAssignedMentorId() != null) {
            mentor = userRepository.findById(student.getAssignedMentorId()).orElse(null);
        }

        List<Question> allQuestions = new ArrayList<>();
        questionRepository.findAll().forEach(allQuestions::add);

        boolean isEighthGrade = student != null && student.getGradeClass() != null && student.getGradeClass().contains("8");

        // 24 Saat filtrelemesi iptal edildi. Öğrenci artık açık olan tüm aktif soruları görecek.
        List<Question> availableQuestions = allQuestions.stream()
                .filter(q -> !q.isEighthGradeOnly() || isEighthGrade)
                .collect(Collectors.toList());

        List<Question> tasks = availableQuestions.stream().filter(Question::isTask).collect(Collectors.toList());
        List<Question> questions = availableQuestions.stream().filter(q -> !q.isTask()).collect(Collectors.toList());

        List<Answer> answers = answerRepository.findByStudentId(studentId);
        Map<Long, Answer> answerMap = new HashMap<>();
        for (Answer a : answers) { answerMap.put(a.getQuestionId(), a); }

        // YENİ: Öğrenci için sadece Kendi Mentörünün Duyurularını Filtreleme
        List<Notification> allNotifs = new ArrayList<>();
        notificationRepository.findAll().forEach(allNotifs::add);
        List<Notification> myNotifs = new ArrayList<>();
        if (mentor != null) {
            String mentorName = mentor.getUsername(); // Mentörün gönderdiği isim formatına göre
            for(Notification n : allNotifs) {
                // Sadece içinde mentörün adı/kullanıcı adı geçen duyuruları ekle (İzolasyon)
                if(n.getMessage().contains(mentorName) || n.getMessage().contains(mentor.getFullName())) {
                    myNotifs.add(n);
                }
            }
        }
        model.addAttribute("notifications", myNotifs);

        model.addAttribute("isSystemOpen", isSystemOpen(LocalDateTime.now()));
        model.addAttribute("student", student);
        model.addAttribute("mentor", mentor);
        model.addAttribute("tasks", tasks);
        model.addAttribute("questions", questions);
        model.addAttribute("answerMap", answerMap);

        List<Message> myMessages = messageRepository.findBySenderIdOrReceiverIdOrderBySentAtAsc(studentId, studentId);
        model.addAttribute("chatMessages", myMessages);

        return "student";
    }

    @PostMapping("/submit-answer")
    public String submitAnswer(
            @RequestParam Long questionId,
            @RequestParam(required = false) String answerText,
            @RequestParam(required = false) List<String> selectedOptions,
            @RequestParam(required = false) Integer completedDays,
            HttpSession session, RedirectAttributes redirectAttributes) {

        if (!"STUDENT".equals(session.getAttribute("loggedInUserRole"))) return "redirect:/";

        // GÜVENLİK: Kilit Kontrolü
        if (!isSystemOpen(LocalDateTime.now())) {
            redirectAttributes.addFlashAttribute("error", "Görev ve vazife gönderme süresi kapalıdır. Sadece Perşembe 18:00 - Cuma 18:00 arası işlem yapabilirsiniz.");
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

        // YENİ: Vazife Puanlama ve Gün Kaydı Mantığı
        if (question.isTask()) {
            a.setCompleted(true);
            if (completedDays != null) {
                a.setCompletedDays(completedDays);
                // Günlük Puan * Seçilen Gün = Toplam Kazanılan Puan
                int dailyPoint = (question.getMaxPoints() != null) ? question.getMaxPoints() : 0;
                a.setMentorScore(dailyPoint * completedDays);
            } else {
                a.setMentorScore(0);
            }
        }
        // YENİ: Çoktan Seçmeli (Soru) Puanlama Mantığı
        else if ("COKTAN_SECMELI".equals(question.getType())) {
            a.setCompleted(false);
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
        }
        else {
            a.setCompleted(false);
            a.setMentorScore(null);
        }

        a.setAiNote(aiService.analyzeText(answerText != null ? answerText : ""));
        answerRepository.save(a);

        redirectAttributes.addFlashAttribute("successMessage", "Cevabın başarıyla kaydedildi! Harika gidiyorsun.");
        return "redirect:/student";
    }
}