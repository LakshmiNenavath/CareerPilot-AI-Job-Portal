package com.careerpilot.service;

import com.careerpilot.model.SystemSetting;
import com.careerpilot.repository.SystemSettingRepository;
import jakarta.annotation.PostConstruct;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Service
public class SettingsService {

    public static final String KEY_GEMINI_API_KEY = "GEMINI_API_KEY";
    public static final String KEY_OPENAI_API_KEY = "OPENAI_API_KEY";
    public static final String KEY_AI_PROVIDER = "AI_PROVIDER"; // BUILTIN, GEMINI, OPENAI
    public static final String KEY_INTERVIEW_TIME_MINUTES = "INTERVIEW_TIME_MINUTES"; // default 7 (5-10 min)
    public static final String KEY_QUESTION_COUNT = "QUESTION_COUNT"; // default 5

    private final SystemSettingRepository settingRepo;

    public SettingsService(SystemSettingRepository settingRepo) {
        this.settingRepo = settingRepo;
    }

    @PostConstruct
    public void initDefaults() {
        initSetting(KEY_AI_PROVIDER, "BUILTIN", "AI", "Active AI Engine: BUILTIN (Smart Offline NLP Engine), GEMINI, or OPENAI");
        initSetting(KEY_GEMINI_API_KEY, "", "AI", "Google Gemini API Key for dynamic LLM generation");
        initSetting(KEY_OPENAI_API_KEY, "", "AI", "OpenAI API Key for GPT-4o / GPT-3.5 generation");
        initSetting(KEY_INTERVIEW_TIME_MINUTES, "8", "INTERVIEW", "Default mock interview time limit in minutes (5 to 10 minutes)");
        initSetting(KEY_QUESTION_COUNT, "5", "INTERVIEW", "Number of questions to ask during the mock interview");
    }

    private void initSetting(String key, String defaultVal, String group, String description) {
        if (settingRepo.findBySettingKey(key).isEmpty()) {
            settingRepo.save(new SystemSetting(key, defaultVal, group, description));
        }
    }

    public String getSetting(String key, String fallback) {
        return settingRepo.findBySettingKey(key)
                .map(SystemSetting::getSettingValue)
                .filter(v -> !v.isBlank())
                .orElse(fallback);
    }

    public List<SystemSetting> getAllSettings() {
        return settingRepo.findAll();
    }

    public Map<String, String> getSettingsMap() {
        Map<String, String> map = new HashMap<>();
        for (SystemSetting s : settingRepo.findAll()) {
            map.put(s.getSettingKey(), s.getSettingValue());
        }
        return map;
    }

    @Transactional
    public void saveSetting(String key, String value) {
        SystemSetting setting = settingRepo.findBySettingKey(key)
                .orElseGet(() -> new SystemSetting(key, value, "GENERAL", "Custom setting"));
        setting.setSettingValue(value != null ? value.trim() : "");
        settingRepo.save(setting);
    }
}
