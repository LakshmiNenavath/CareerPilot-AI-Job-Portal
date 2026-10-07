package com.careerpilot.controller;

import com.careerpilot.model.Candidate;
import com.careerpilot.model.InterviewQuestion;
import com.careerpilot.model.InterviewSession;
import com.careerpilot.repository.InterviewQuestionRepository;
import com.careerpilot.service.CandidateService;
import com.careerpilot.service.MockInterviewService;
import com.careerpilot.service.SettingsService;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;

import java.util.List;
import java.util.Map;
import java.util.Optional;

@Controller
public class HomeController {

    private final CandidateService candidateService;
    private final MockInterviewService mockInterviewService;
    private final InterviewQuestionRepository questionRepo;
    private final SettingsService settingsService;

    public HomeController(CandidateService candidateService,
                          MockInterviewService mockInterviewService,
                          InterviewQuestionRepository questionRepo,
                          SettingsService settingsService) {
        this.candidateService = candidateService;
        this.mockInterviewService = mockInterviewService;
        this.questionRepo = questionRepo;
        this.settingsService = settingsService;
    }

    @GetMapping("/")
    public String index(Model model) {
        model.addAttribute("totalCandidates", candidateService.getTotalCandidatesCount());
        model.addAttribute("completedInterviews", candidateService.getCompletedInterviewsCount());
        model.addAttribute("avgAtsScore", candidateService.getAverageAtsScore());
        return "index";
    }

    @GetMapping("/upload")
    public String upload() {
        return "upload";
    }

    @GetMapping("/analysis/{candidateId}")
    public String analysis(@PathVariable Long candidateId, Model model) {
        Optional<Candidate> opt = candidateService.findById(candidateId);
        if (opt.isEmpty()) {
            return "redirect:/upload?error=Candidate+not+found";
        }

        Candidate candidate = opt.get();
        model.addAttribute("candidate", candidate);
        model.addAttribute("pros", candidateService.deserializeList(candidate.getProsJson()));
        model.addAttribute("cons", candidateService.deserializeList(candidate.getConsJson()));
        model.addAttribute("improvements", candidateService.deserializeList(candidate.getImprovementSuggestionsJson()));
        model.addAttribute("skills", candidateService.deserializeList(candidate.getSkillsFoundJson()));
        model.addAttribute("missingKeywords", candidateService.deserializeList(candidate.getMissingKeywordsJson()));
        model.addAttribute("subScores", candidateService.deserializeMap(candidate.getBreakdownScoresJson()));

        return "analysis";
    }

    @GetMapping("/interview/{candidateId}")
    public String interview(@PathVariable Long candidateId, Model model) {
        Optional<Candidate> opt = candidateService.findById(candidateId);
        if (opt.isEmpty()) {
            return "redirect:/upload?error=Candidate+not+found";
        }

        Candidate candidate = opt.get();
        model.addAttribute("candidate", candidate);

        String interviewMinutes = settingsService.getSetting(SettingsService.KEY_INTERVIEW_TIME_MINUTES, "8");
        model.addAttribute("interviewMinutes", interviewMinutes);

        return "interview";
    }

    @GetMapping("/interview-report/{candidateId}")
    public String report(@PathVariable Long candidateId, Model model) {
        Optional<Candidate> optCandidate = candidateService.findById(candidateId);
        if (optCandidate.isEmpty()) {
            return "redirect:/upload?error=Candidate+not+found";
        }

        Candidate candidate = optCandidate.get();
        model.addAttribute("candidate", candidate);

        Optional<InterviewSession> optSession = mockInterviewService.getSessionByCandidateId(candidateId);
        if (optSession.isPresent()) {
            InterviewSession session = optSession.get();
            model.addAttribute("session", session);
            model.addAttribute("strengths", candidateService.deserializeList(session.getStrengthsJson()));
            model.addAttribute("improvementAreas", candidateService.deserializeList(session.getImprovementAreasJson()));

            List<InterviewQuestion> questions = questionRepo.findByInterviewSessionIdOrderByQuestionIndexAsc(session.getId());
            model.addAttribute("questions", questions);
        }

        return "report";
    }
}
