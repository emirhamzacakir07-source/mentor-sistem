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

    @GetMapping("/")
    public String loginPage() { return "index"; }

    @PostMapping("/login")
    public String login(@RequestParam String username, @RequestParam String password, HttpSession session, RedirectAttributes redirectAttributes) {
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
    public String registerUser(@ModelAttribute User user, RedirectAttributes redirectAttributes) {
        if (userRepository.findByUsername(user.getUsername()) != null) {
            redirectAttributes.addFlashAttribute("error", "Bu kullanıcı adı zaten alınmış!");
            return "redirect:/register";
        }
        user.setRole("STUDENT");
        user.setApproved(true);
        user.setLastLoginDate(LocalDateTime.now());
        user.setPassword(passwordEncoder.encode(user.getPassword()));
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
        user.setApproved(false);
        user.setLastLoginDate(LocalDateTime.now());
        user.setPassword(passwordEncoder.encode(user.getPassword()));
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

        // --- YENİ EKLENEN: Liderlik Tablosu (Sıralama) Mantığı ---
        Map<User, Integer> studentScores = new HashMap<>();
        for(User u : allUsers) {
            if("STUDENT".equals(u.getRole())) { studentScores.put(u, 0); }
        }
        for(Answer a : allAnswers) {
            // SIFIRLANMIŞ (ESKİ AY) PUANLARI LİDERLİK TABLOSUNA EKLEME
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
        // Puanlara göre büyükten küçüğe sırala
        leaderboard.sort((m1, m2) -> ((Integer) m2.get("score")).compareTo((Integer) m1.get("score")));
        model.addAttribute("leaderboard", leaderboard);
        // --------------------------------------------------------

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
            map.put("isMonthlyReset", ans.isMonthlyReset()); // YENİ: Arayüzde eski olduğunu belirtmek için
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

    // YENİ EKLENEN METOT: Puanları Sıfırlama (Ayı Kapatma) İşlemi
    @PostMapping("/admin/reset-monthly-scores")
    public String resetMonthlyScores(HttpSession session, RedirectAttributes redirectAttributes) {
        if (!"ADMIN".equals(session.getAttribute("loggedInUserRole"))) return "redirect:/";

        List<Answer> allAnswers = new ArrayList<>();
        answerRepository.findAll().forEach(allAnswers::add);

        // Tüm mevcut cevapları "Eski Ay" olarak işaretle (Geçmiş veriler silinmez, sadece puanlamadan düşer)
        for(Answer a : allAnswers) {
            a.setMonthlyReset(true);
        }
        answerRepository.saveAll(allAnswers);

        redirectAttributes.addFlashAttribute("successMessage", "🏆 Yeni aya başarıyla geçildi! Tüm öğrencilerin aktif liderlik puanları 0'landı. (Geçmiş cevaplar, vazifeler ve mentör notları arşivde korundu.)");
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
    public String submitFeedback(@RequestParam Long answerId, @RequestParam Integer mentorScore, @RequestParam String mentorFeedback, HttpSession session, RedirectAttributes redirectAttributes) {
        if (!"MENTOR".equals(session.getAttribute("loggedInUserRole"))) return "redirect:/";
        Long mentorId = (Long) session.getAttribute("loggedInUserId");

        Answer ans = answerRepository.findById(answerId).orElse(null);
        if (ans == null) { redirectAttributes.addFlashAttribute("error", "Cevap bulunamadı."); return "redirect:/mentor"; }

        User student = userRepository.findById(ans.getStudentId()).orElse(null);
        if (student == null || !mentorId.equals(student.getAssignedMentorId())) {
            redirectAttributes.addFlashAttribute("error", "Bu cevabı değerlendirme yetkiniz yok.");
            return "redirect:/mentor";
        }

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
        redirectAttributes.addFlashAttribute("successMessage", "Toplantı planlandı ve tüm öğrencilere duyuruldu.");
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
        LocalDateTime twentyFourHoursAgo = LocalDateTime.now().minusHours(24);

        List<Question> availableQuestions = allQuestions.stream()
                .filter(q -> q.getCreatedAt() == null || q.getCreatedAt().isAfter(twentyFourHoursAgo))
                .filter(q -> !q.isEighthGradeOnly() || isEighthGrade)
                .collect(Collectors.toList());

        List<Question> tasks = availableQuestions.stream().filter(Question::isTask).collect(Collectors.toList());
        List<Question> questions = availableQuestions.stream().filter(q -> !q.isTask()).collect(Collectors.toList());

        List<Answer> answers = answerRepository.findByStudentId(studentId);
        Map<Long, Answer> answerMap = new HashMap<>();
        for (Answer a : answers) { answerMap.put(a.getQuestionId(), a); }

        boolean isLocked = false;
        LocalDateTime now = LocalDateTime.now();
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
        model.addAttribute("notifications", notificationRepository.findAll());

        List<Message> myMessages = messageRepository.findBySenderIdOrReceiverIdOrderBySentAtAsc(studentId, studentId);
        model.addAttribute("chatMessages", myMessages);

        return "student";
    }

    @PostMapping("/submit-answer")
    public String submitAnswer(
            @RequestParam Long questionId,
            @RequestParam(required = false) String answerText,
            @RequestParam(required = false) List<String> selectedOptions,
            @RequestParam(required = false, defaultValue = "false") Boolean isCompleted,
            HttpSession session, RedirectAttributes redirectAttributes) {

        if (!"STUDENT".equals(session.getAttribute("loggedInUserRole"))) return "redirect:/";

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

        if (question.getCreatedAt() != null && question.getCreatedAt().isBefore(now.minusHours(24))) {
            redirectAttributes.addFlashAttribute("error", "Süreniz doldu. Bu soru/vazife yüklendikten sonra 24 saat geçmiştir.");
            return "redirect:/student";
        }

        Answer a = new Answer();
        a.setQuestionId(questionId);
        a.setStudentId(studentId);
        a.setAnswerText(answerText != null ? answerText : "");
        a.setCreatedAt(LocalDateTime.now());
        a.setCompleted(isCompleted);

        if ("COKTAN_SECMELI".equals(question.getType())) {
            int totalScore = 0;
            if (selectedOptions != null && !selectedOptions.isEmpty()) {
                a.setSelectedOptions(String.join(",", selectedOptions));
                if (selectedOptions.contains("A") && question.getOptionAPoint() != null) totalScore += question.getOptionAPoint();
                if (selectedOptions.contains("B") && question.getOptionBPoint() != null) totalScore += question.getOptionBPoint();
                if (selectedOptions.contains("C") && question.getOptionCPoint() != null) totalScore += question.getOptionCPoint();
                if (selectedOptions.contains("D") && question.getOptionDPoint() != null) totalScore += question.getOptionDPoint();
            } else { a.setSelectedOptions(""); }
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