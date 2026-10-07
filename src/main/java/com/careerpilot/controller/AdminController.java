package com.careerpilot.controller;

import com.careerpilot.model.AiPromptConfig;
import com.careerpilot.model.Candidate;
import com.careerpilot.model.InterviewQuestion;
import com.careerpilot.model.InterviewSession;
import com.careerpilot.repository.InterviewQuestionRepository;
import com.careerpilot.service.*;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.servlet.http.HttpSession;
import org.springframework.http.HttpHeaders;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.Optional;

@Controller
@RequestMapping("/admin")
public class AdminController {

    private final AdminAuthService adminAuthService;
    private final CandidateService candidateService;
    private final MockInterviewService mockInterviewService;
    private final PromptService promptService;
    private final SettingsService settingsService;
    private final InterviewQuestionRepository questionRepo;

    public AdminController(AdminAuthService adminAuthService,
                           CandidateService candidateService,
                           MockInterviewService mockInterviewService,
                           PromptService promptService,
                           SettingsService settingsService,
                           InterviewQuestionRepository questionRepo) {
        this.adminAuthService = adminAuthService;
        this.candidateService = candidateService;
        this.mockInterviewService = mockInterviewService;
        this.promptService = promptService;
        this.settingsService = settingsService;
        this.questionRepo = questionRepo;
    }

    @GetMapping("")
    public String adminRoot(HttpSession session) {
        if (session.getAttribute("ADMIN_USER") != null) {
            return "redirect:/admin/dashboard";
        }
        return "redirect:/admin/login";
    }

    @GetMapping("/login")
    public String loginPage(@RequestParam(value = "error", required = false) String error,
                            @RequestParam(value = "logged_out", required = false) String loggedOut,
                            Model model) {
        if (error != null) {
            model.addAttribute("errorMessage", "Invalid administrator credentials. Please try again.");
        }
        if (loggedOut != null) {
            model.addAttribute("successMessage", "You have been logged out securely.");
        }
        return "admin/login";
    }

    @PostMapping("/login")
    public String handleLogin(@RequestParam("username") String username,
                              @RequestParam("password") String password,
                              HttpSession session,
                              RedirectAttributes redirectAttrs) {
        if (adminAuthService.authenticate(username, password)) {
            session.setAttribute("ADMIN_USER", username);
            return "redirect:/admin/dashboard";
        } else {
            redirectAttrs.addAttribute("error", "invalid");
            return "redirect:/admin/login";
        }
    }

    @GetMapping("/logout")
    public String handleLogout(HttpSession session) {
        session.invalidate();
        return "redirect:/admin/login?logged_out=true";
    }

    @GetMapping("/dashboard")
    public String dashboard(Model model, HttpSession session) {
        model.addAttribute("adminUser", session.getAttribute("ADMIN_USER"));
        model.addAttribute("totalCandidates", candidateService.getTotalCandidatesCount());
        model.addAttribute("completedInterviews", candidateService.getCompletedInterviewsCount());
        model.addAttribute("avgAtsScore", candidateService.getAverageAtsScore());
        model.addAttribute("avgInterviewScore", candidateService.getAverageInterviewScore());
        model.addAttribute("recentCandidates", candidateService.getAllCandidates().stream().limit(8).toList());
        return "admin/dashboard";
    }

    @GetMapping("/users")
    public String users(@RequestParam(value = "search", required = false) String search, Model model) {
        List<Candidate> candidates = candidateService.searchCandidates(search);
        model.addAttribute("candidates", candidates);
        model.addAttribute("searchQuery", search != null ? search : "");
        return "admin/users";
    }

    @GetMapping("/users/{id}")
    public String userDetail(@PathVariable Long id, Model model) {
        Optional<Candidate> opt = candidateService.findById(id);
        if (opt.isEmpty()) {
            return "redirect:/admin/users?error=User+not+found";
        }

        Candidate candidate = opt.get();
        model.addAttribute("candidate", candidate);
        model.addAttribute("pros", candidateService.deserializeList(candidate.getProsJson()));
        model.addAttribute("cons", candidateService.deserializeList(candidate.getConsJson()));
        model.addAttribute("improvements", candidateService.deserializeList(candidate.getImprovementSuggestionsJson()));
        model.addAttribute("skills", candidateService.deserializeList(candidate.getSkillsFoundJson()));
        model.addAttribute("missingKeywords", candidateService.deserializeList(candidate.getMissingKeywordsJson()));

        Optional<InterviewSession> optSession = mockInterviewService.getSessionByCandidateId(id);
        if (optSession.isPresent()) {
            InterviewSession session = optSession.get();
            model.addAttribute("session", session);
            model.addAttribute("questions", questionRepo.findByInterviewSessionIdOrderByQuestionIndexAsc(session.getId()));
        }

        return "admin/user-detail";
    }

    @PostMapping("/users/delete/{id}")
    public String deleteUser(@PathVariable Long id, RedirectAttributes redirectAttrs) {
        try {
            candidateService.deleteCandidate(id);
            redirectAttrs.addFlashAttribute("successMessage", "Candidate record deleted successfully.");
        } catch (Exception e) {
            redirectAttrs.addFlashAttribute("errorMessage", "Error deleting candidate: " + e.getMessage());
        }
        return "redirect:/admin/users";
    }

    @GetMapping("/users/export")
    public void exportUsers(HttpServletResponse response) throws IOException {
        String csv = candidateService.exportCandidatesToCsv();
        response.setContentType("text/csv; charset=UTF-8");
        response.setHeader(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"careerpilot_candidates.csv\"");
        response.getOutputStream().write(csv.getBytes(StandardCharsets.UTF_8));
        response.getOutputStream().flush();
    }

    @GetMapping("/prompts")
    public String prompts(Model model) {
        model.addAttribute("prompts", promptService.getAllPrompts());
        return "admin/prompts";
    }

    @PostMapping("/prompts/save")
    public String savePrompt(@RequestParam("promptKey") String promptKey,
                             @RequestParam("title") String title,
                             @RequestParam("description") String description,
                             @RequestParam("templateText") String templateText,
                             RedirectAttributes redirectAttrs) {
        try {
            promptService.updatePrompt(promptKey, title, description, templateText);
            redirectAttrs.addFlashAttribute("successMessage", "AI Prompt '" + title + "' updated successfully! The system will now use your updated instructions.");
        } catch (Exception e) {
            redirectAttrs.addFlashAttribute("errorMessage", "Failed to update prompt: " + e.getMessage());
        }
        return "redirect:/admin/prompts";
    }

    @PostMapping("/prompts/reset")
    public String resetPrompts(RedirectAttributes redirectAttrs) {
        try {
            promptService.resetToDefaults();
            redirectAttrs.addFlashAttribute("successMessage", "All AI Prompts have been reset to factory defaults.");
        } catch (Exception e) {
            redirectAttrs.addFlashAttribute("errorMessage", "Failed to reset prompts: " + e.getMessage());
        }
        return "redirect:/admin/prompts";
    }

    @GetMapping("/settings")
    public String settings(Model model) {
        model.addAttribute("settings", settingsService.getSettingsMap());
        return "admin/settings";
    }

    @PostMapping("/settings/save")
    public String saveSettings(@RequestParam("aiProvider") String aiProvider,
                               @RequestParam(value = "geminiApiKey", required = false) String geminiApiKey,
                               @RequestParam(value = "openaiApiKey", required = false) String openaiApiKey,
                               @RequestParam("interviewMinutes") String interviewMinutes,
                               @RequestParam("questionCount") String questionCount,
                               RedirectAttributes redirectAttrs) {
        try {
            settingsService.saveSetting(SettingsService.KEY_AI_PROVIDER, aiProvider);
            settingsService.saveSetting(SettingsService.KEY_GEMINI_API_KEY, geminiApiKey);
            settingsService.saveSetting(SettingsService.KEY_OPENAI_API_KEY, openaiApiKey);
            settingsService.saveSetting(SettingsService.KEY_INTERVIEW_TIME_MINUTES, interviewMinutes);
            settingsService.saveSetting(SettingsService.KEY_QUESTION_COUNT, questionCount);

            redirectAttrs.addFlashAttribute("successMessage", "System & AI settings saved successfully!");
        } catch (Exception e) {
            redirectAttrs.addFlashAttribute("errorMessage", "Failed to save settings: " + e.getMessage());
        }
        return "redirect:/admin/settings";
    }
}
