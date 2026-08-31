package com.example.mentor;

import jakarta.servlet.http.HttpSession;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
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

    // --- GENEL GİRİŞ (Öğrenci ve Admin) ---
    @GetMapping("/")
    public String loginPage() { return "index"; }

    @PostMapping("/login")
    public String login(@RequestParam String username, @RequestParam String password, HttpSession session, Model model) {
        User user = userRepository.findByUsernameAndPassword(username, password);
        if (user != null) {
            if ("MENTOR".equals(user.getRole())) {
                model.addAttribute("error", "Mentör girişleri özel sayfadan yapılmaktadır. Lütfen 'Mentör Girişi' bağlantısını kullanın.");
                return "index";
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
        model.addAttribute("error", "Kullanıcı adı veya şifre hatalı!");
        return "index";
    }

    // --- MENTÖR ÖZEL GİRİŞİ ---
    @GetMapping("/mentor-login")
    public String mentorLoginPage() {
        return "mentor-login";
    }

    @PostMapping("/mentor-login")
    public String processMentorLogin(@RequestParam String username, @RequestParam String password, HttpSession session, Model model) {
        User user = userRepository.findByUsernameAndPassword(username, password);

        if (user != null && "MENTOR".equals(user.getRole())) {
            if (!user.isApproved()) {
                model.addAttribute("error", "Hesabınız henüz onaylanmamış. Lütfen yönetici onayını bekleyiniz.");
                return "mentor-login";
            }
            user.setLastLoginDate(LocalDateTime.now());
            user.setInactiveWarningSent(false);
            userRepository.save(user);

            session.setAttribute("loggedInUserId", user.getId());
            session.setAttribute("loggedInUserRole", user.getRole());
            session.setAttribute("loggedInUsername", user.getUsername());

            return "redirect:/mentor";
        }

        model.addAttribute("error", "Kayıtlı mentör bulunamadı veya şifre hatalı!");
        return "mentor-login";
    }

    // --- KAYIT İŞLEMLERİ ---
    @GetMapping("/register")
    public String registerPage() { return "register"; }

    @PostMapping("/register")
    public String registerUser(@ModelAttribute User user, Model model) {
        if (userRepository.findByUsername(user.getUsername()) != null) {
            model.addAttribute("error", "Bu kullanıcı adı zaten alınmış!");
            return "register";
        }
        user.setRole("STUDENT");
        user.setApproved(true);
        user.setLastLoginDate(LocalDateTime.now());
        userRepository.save(user);
        return "redirect:/?success=true";
    }

    @GetMapping("/mentor-register")
    public String mentorRegisterPage() { return "mentor-register"; }

    @PostMapping("/mentor-register")
    public String registerMentor(@ModelAttribute User user, Model model) {
        if (userRepository.findByUsername(user.getUsername()) != null) {
            model.addAttribute("error", "Bu kullanıcı adı zaten alınmış!");
            return "mentor-register";
        }
        user.setRole("MENTOR");
        user.setApproved(false);
        user.setLastLoginDate(LocalDateTime.now());
        userRepository.save(user);
        model.addAttribute("success", true);
        return "mentor-register";
    }

    @GetMapping("/logout")
    public String logout(HttpSession session) {
        session.invalidate();
        return "redirect:/";
    }

    @GetMapping("/forgot-password")
    public String forgotPasswordPage() { return "forgot-password"; }

    @PostMapping("/forgot-password")
    public String processForgotPassword(@RequestParam String username, Model model) {
        User user = userRepository.findByUsername(username);
        if (user != null) {
            Notification n = new Notification();
            n.setMessage("🔴 ŞİFRE SIFIRLAMA TALEBİ: " + user.getFullName());
            n.setCreatedAt(LocalDateTime.now());
            notificationRepository.save(n);
            model.addAttribute("success", "Talebiniz yöneticiye iletildi.");
        } else {
            model.addAttribute("error", "Kullanıcı bulunamadı!");
        }
        return "forgot-password";
    }

    @PostMapping("/send-message")
    public String sendMessage(@RequestParam Long receiverId, @RequestParam String content, HttpSession session) {
        Long senderId = (Long) session.getAttribute("loggedInUserId");
        String role = (String) session.getAttribute("loggedInUserRole");

        if (senderId == null) return "redirect:/";

        Message msg = new Message();
        msg.setSenderId(senderId);
        msg.setReceiverId(receiverId);
        msg.setContent(content);
        msg.setSentAt(LocalDateTime.now());
        msg.setRead(false);
        messageRepository.save(msg);

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

        List<Answer> allAnswers = (List<Answer>) answerRepository.findAll();
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

    // İŞTE DÜZELTİLEN VE SİSTEMİ ÇÖKERTMEKTEN KURTARAN METOT
    @PostMapping("/add-question")
    public String addQuestion(
            @RequestParam(required = false) Long id,
            @RequestParam String content, @RequestParam String type,
            @RequestParam(required = false, defaultValue = "false") boolean isTask,
            @RequestParam(required = false) String category,
            @RequestParam(required = false) String maxPoints, // Integer yerine String aldık
            @RequestParam(required = false) String optionA_text, @RequestParam(required = false) String optionA_point,
            @RequestParam(required = false) String optionB_text, @RequestParam(required = false) String optionB_point,
            @RequestParam(required = false) String optionC_text, @RequestParam(required = false) String optionC_point,
            @RequestParam(required = false) String optionD_text, @RequestParam(required = false) String optionD_point,
            @RequestParam(required = false, defaultValue = "false") boolean allowMultipleSelections,
            HttpSession session) {

        if (!"ADMIN".equals(session.getAttribute("loggedInUserRole"))) return "redirect:/";

        Question q;
        if (id != null) {
            q = questionRepository.findById(id).orElse(new Question());
        } else {
            q = new Question();
        }

        q.setContent(content); q.setType(type); q.setTask(isTask); q.setCategory(category);
        q.setAllowMultipleSelections(allowMultipleSelections);

        // Boş bırakılırsa çökmeyi engelleyen güvenli çevirici
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
        return "redirect:/admin";
    }

    @PostMapping("/admin/delete-question")
    public String deleteQuestion(@RequestParam Long id, HttpSession session) {
        if (!"ADMIN".equals(session.getAttribute("loggedInUserRole"))) return "redirect:/";

        List<Answer> answers = answerRepository.findByQuestionId(id);
        answerRepository.deleteAll(answers);
        questionRepository.deleteById(id);

        return "redirect:/admin";
    }

    @PostMapping("/admin/assign-mentor")
    public String assignMentor(@RequestParam Long mentor_id, @RequestParam Long student_id, HttpSession session) {
        if (!"ADMIN".equals(session.getAttribute("loggedInUserRole"))) return "redirect:/";
        User student = userRepository.findById(student_id).orElse(null);
        if (student != null && "STUDENT".equals(student.getRole())) {
            student.setAssignedMentorId(mentor_id);
            userRepository.save(student);
        }
        return "redirect:/admin";
    }

    @PostMapping("/admin/approve-mentor")
    public String approveMentor(@RequestParam Long mentor_id, HttpSession session) {
        if (!"ADMIN".equals(session.getAttribute("loggedInUserRole"))) return "redirect:/";
        User mentor = userRepository.findById(mentor_id).orElse(null);
        if (mentor != null && "MENTOR".equals(mentor.getRole())) {
            mentor.setApproved(true);
            userRepository.save(mentor);
        }
        return "redirect:/admin";
    }

    // --- MENTÖR PANELİ ---
    @GetMapping("/mentor")
    public String mentorPanel(HttpSession session, Model model) {
        if (!"MENTOR".equals(session.getAttribute("loggedInUserRole"))) return "redirect:/";
        Long mentorId = (Long) session.getAttribute("loggedInUserId");

        List<User> allUsers = (List<User>) userRepository.findAll();
        List<User> myStudents = allUsers.stream().filter(u -> "STUDENT".equals(u.getRole()) && mentorId.equals(u.getAssignedMentorId())).collect(Collectors.toList());
        model.addAttribute("myStudents", myStudents);

        List<Map<String, Object>> studentAnswers = new ArrayList<>();
        for (User student : myStudents) {
            List<Answer> answers = answerRepository.findByStudentId(student.getId());
            for (Answer ans : answers) {
                Map<String, Object> map = new HashMap<>();
                map.put("id", ans.getId());
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
    public String submitFeedback(@RequestParam Long answerId, @RequestParam Integer mentorScore, @RequestParam String mentorFeedback, HttpSession session) {
        if (!"MENTOR".equals(session.getAttribute("loggedInUserRole"))) return "redirect:/";
        Answer ans = answerRepository.findById(answerId).orElse(null);
        if (ans != null) {
            ans.setMentorScore(mentorScore);
            ans.setMentorFeedback(mentorFeedback);
            answerRepository.save(ans);
        }
        return "redirect:/mentor";
    }

    @PostMapping("/mentor/send-announcement")
    public String sendAnnouncement(@RequestParam String message, HttpSession session) {
        if (!"MENTOR".equals(session.getAttribute("loggedInUserRole"))) return "redirect:/";
        String mentorName = (String) session.getAttribute("loggedInUsername");
        Notification n = new Notification();
        n.setMessage("📢 DUYURU (" + mentorName + "): " + message);
        n.setCreatedAt(LocalDateTime.now());
        notificationRepository.save(n);
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

        List<Question> allQuestions = (List<Question>) questionRepository.findAll();
        List<Question> tasks = allQuestions.stream().filter(Question::isTask).collect(Collectors.toList());
        List<Question> questions = allQuestions.stream().filter(q -> !q.isTask()).collect(Collectors.toList());

        List<Answer> answers = answerRepository.findByStudentId(studentId);
        int totalScore = answers.stream().filter(a -> a.getMentorScore() != null).mapToInt(Answer::getMentorScore).sum();

        Map<Long, Answer> answerMap = new HashMap<>();
        for (Answer a : answers) { answerMap.put(a.getQuestionId(), a); }

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

    @PostMapping("/submit-answer")
    public String submitAnswer(@RequestParam Long questionId, @RequestParam String answerText, HttpSession session) {
        if (!"STUDENT".equals(session.getAttribute("loggedInUserRole"))) return "redirect:/";

        Long studentId = (Long) session.getAttribute("loggedInUserId");
        User student = userRepository.findById(studentId).orElse(null);

        Answer a = new Answer();
        a.setQuestionId(questionId);
        a.setAnswerText(answerText);
        a.setStudentId(studentId);

        a.setAiNote(aiService.analyzeText(answerText));
        answerRepository.save(a);

        if (student != null) {
            Notification n = new Notification();
            n.setMessage("🤖 SİSTEM BİLDİRİMİ: " + student.getFullName() + " isimli öğrenci yeni bir soru/vazife yanıtladı.");
            n.setCreatedAt(LocalDateTime.now());
            notificationRepository.save(n);
        }

        return "redirect:/student";
    }
}