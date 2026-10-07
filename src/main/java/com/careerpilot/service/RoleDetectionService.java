package com.careerpilot.service;

import org.springframework.stereotype.Service;

import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

@Service
public class RoleDetectionService {

    public static class RoleProfile {
        private final String roleName;
        private final List<String> primaryKeywords;
        private final List<String> secondaryKeywords;
        private final String defaultSummary;

        public RoleProfile(String roleName, List<String> primaryKeywords, List<String> secondaryKeywords, String defaultSummary) {
            this.roleName = roleName;
            this.primaryKeywords = primaryKeywords;
            this.secondaryKeywords = secondaryKeywords;
            this.defaultSummary = defaultSummary;
        }

        public String getRoleName() { return roleName; }
        public List<String> getPrimaryKeywords() { return primaryKeywords; }
        public List<String> getSecondaryKeywords() { return secondaryKeywords; }
        public String getDefaultSummary() { return defaultSummary; }
    }

    public static class RoleDetectionResult {
        private String bestRole;
        private int confidenceScore;
        private String experienceLevel;
        private List<String> foundSkills = new ArrayList<>();
        private List<String> missingKeywords = new ArrayList<>();
        private List<String> alternateRoles = new ArrayList<>();

        public String getBestRole() { return bestRole; }
        public void setBestRole(String bestRole) { this.bestRole = bestRole; }
        public int getConfidenceScore() { return confidenceScore; }
        public void setConfidenceScore(int confidenceScore) { this.confidenceScore = confidenceScore; }
        public String getExperienceLevel() { return experienceLevel; }
        public void setExperienceLevel(String experienceLevel) { this.experienceLevel = experienceLevel; }
        public List<String> getFoundSkills() { return foundSkills; }
        public void setFoundSkills(List<String> foundSkills) { this.foundSkills = foundSkills; }
        public List<String> getMissingKeywords() { return missingKeywords; }
        public void setMissingKeywords(List<String> missingKeywords) { this.missingKeywords = missingKeywords; }
        public List<String> getAlternateRoles() { return alternateRoles; }
        public void setAlternateRoles(List<String> alternateRoles) { this.alternateRoles = alternateRoles; }
    }

    private final List<RoleProfile> roleProfiles = new ArrayList<>();

    public RoleDetectionService() {
        initRoles();
    }

    private void initRoles() {
        roleProfiles.add(new RoleProfile(
                "Full Stack Java Developer",
                Arrays.asList("java", "spring boot", "spring", "hibernate", "jpa", "microservices", "rest api", "mysql", "postgresql"),
                Arrays.asList("react", "angular", "javascript", "docker", "maven", "git", "jwt", "junit", "redis", "kafka"),
                "High proficiency in backend Java architectures, RESTful API design, relational databases, and enterprise full-stack development."
        ));

        roleProfiles.add(new RoleProfile(
                "Frontend Web Developer",
                Arrays.asList("react", "javascript", "typescript", "html5", "css3", "next.js", "tailwind", "redux"),
                Arrays.asList("vue", "sass", "bootstrap", "figma", "webpack", "vite", "responsive design", "graphql", "ui/ux", "jest"),
                "Specialized in modern client-side rendering, responsive web applications, state management, and modern component design systems."
        ));

        roleProfiles.add(new RoleProfile(
                "Python & Backend Engineer",
                Arrays.asList("python", "django", "fastapi", "flask", "postgresql", "rest api", "celery"),
                Arrays.asList("redis", "docker", "git", "linux", "mongodb", "sqlite", "jwt", "pytest", "rabbitmq"),
                "Focused on scalable server-side systems, asynchronous processing, data modeling, and clean Pythonic architecture."
        ));

        roleProfiles.add(new RoleProfile(
                "Data Scientist & Machine Learning Engineer",
                Arrays.asList("python", "machine learning", "deep learning", "pandas", "numpy", "scikit-learn", "tensorflow", "pytorch"),
                Arrays.asList("nlp", "computer vision", "sql", "data visualization", "matplotlib", "seaborn", "keras", "statistics", "feature engineering"),
                "Experienced in statistical analysis, predictive modeling, deep neural networks, and end-to-end ML model development."
        ));

        roleProfiles.add(new RoleProfile(
                "Cloud & DevOps Engineer",
                Arrays.asList("aws", "docker", "kubernetes", "ci/cd", "terraform", "linux", "jenkins"),
                Arrays.asList("azure", "gcp", "ansible", "prometheus", "grafana", "git", "bash", "nginx", "helm", "devops"),
                "Skilled in container orchestration, infrastructure as code, continuous integration/delivery, and cloud architecture."
        ));

        roleProfiles.add(new RoleProfile(
                "Mobile Application Developer",
                Arrays.asList("flutter", "react native", "android", "kotlin", "swift", "ios", "mobile"),
                Arrays.asList("dart", "java", "firebase", "sqlite", "rest api", "redux", "state management", "xcode", "play store"),
                "Expertise in cross-platform or native mobile client development, app lifecycle, mobile UI, and offline-first data sync."
        ));

        roleProfiles.add(new RoleProfile(
                "Cybersecurity Analyst",
                Arrays.asList("cybersecurity", "penetration testing", "network security", "siem", "cryptography", "firewall", "vulnerability"),
                Arrays.asList("owasp", "wireshark", "kali linux", "incident response", "soc", "compliance", "iso 27001", "ethical hacking"),
                "Specializes in threat hunting, security operations, vulnerability management, defensive security, and incident response."
        ));

        roleProfiles.add(new RoleProfile(
                "QA & Test Automation Engineer",
                Arrays.asList("selenium", "test automation", "qa", "junit", "testng", "cucumber", "cypress"),
                Arrays.asList("postman", "rest assured", "api testing", "jira", "manual testing", "agile", "regression testing", "sql"),
                "Dedicated to test coverage, automated test frameworks, regression suites, API validation, and quality engineering."
        ));

        roleProfiles.add(new RoleProfile(
                "Data Engineer & Big Data",
                Arrays.asList("spark", "kafka", "hadoop", "sql", "etl", "data warehouse", "airflow"),
                Arrays.asList("snowflake", "bigquery", "hive", "python", "scala", "databricks", "nosql", "data pipeline"),
                "Architecture of large-scale distributed pipelines, streaming data processing, batch workloads, and modern data warehouses."
        ));

        roleProfiles.add(new RoleProfile(
                "AI & Prompt / LLM Engineer",
                Arrays.asList("generative ai", "llm", "langchain", "prompt engineering", "rag", "vector database", "openai"),
                Arrays.asList("huggingface", "embeddings", "pinecone", "chromadb", "python", "fine-tuning", "transformers"),
                "Building next-gen applications with Large Language Models, Retrieval-Augmented Generation, and vector embeddings."
        ));
    }

    public RoleDetectionResult detectRole(String resumeText) {
        RoleDetectionResult result = new RoleDetectionResult();
        if (resumeText == null || resumeText.isBlank()) {
            result.setBestRole("Full Stack Java Developer");
            result.setConfidenceScore(60);
            result.setExperienceLevel("Entry-Level / Fresher");
            return result;
        }

        String lowerText = resumeText.toLowerCase();

        Map<RoleProfile, Integer> scoreMap = new HashMap<>();
        Map<RoleProfile, List<String>> foundMap = new HashMap<>();

        for (RoleProfile role : roleProfiles) {
            int score = 0;
            List<String> matched = new ArrayList<>();

            for (String kw : role.getPrimaryKeywords()) {
                if (containsKeyword(lowerText, kw)) {
                    score += 15;
                    matched.add(capitalize(kw));
                }
            }

            for (String kw : role.getSecondaryKeywords()) {
                if (containsKeyword(lowerText, kw)) {
                    score += 7;
                    matched.add(capitalize(kw));
                }
            }

            scoreMap.put(role, score);
            foundMap.put(role, matched);
        }

        // Sort by score
        List<Map.Entry<RoleProfile, Integer>> sorted = new ArrayList<>(scoreMap.entrySet());
        sorted.sort((a, b) -> b.getValue().compareTo(a.getValue()));

        RoleProfile winner = sorted.get(0).getKey();
        int topScore = sorted.get(0).getValue();

        // Calculate confidence (capped at 96% and minimum 65%)
        int confidence = Math.min(96, Math.max(65, 50 + (topScore / 2)));

        result.setBestRole(winner.getRoleName());
        result.setConfidenceScore(confidence);

        List<String> winnerSkills = foundMap.get(winner);
        result.setFoundSkills(winnerSkills);

        // Find missing keywords for winner
        List<String> missing = new ArrayList<>();
        for (String kw : winner.getPrimaryKeywords()) {
            if (!containsKeyword(lowerText, kw)) {
                missing.add(capitalize(kw));
            }
        }
        for (String kw : winner.getSecondaryKeywords()) {
            if (!containsKeyword(lowerText, kw) && missing.size() < 6) {
                missing.add(capitalize(kw));
            }
        }
        result.setMissingKeywords(missing);

        // Alternate roles
        List<String> alternates = new ArrayList<>();
        if (sorted.size() > 1 && sorted.get(1).getValue() > 15) {
            alternates.add(sorted.get(1).getKey().getRoleName());
        }
        if (sorted.size() > 2 && sorted.get(2).getValue() > 15) {
            alternates.add(sorted.get(2).getKey().getRoleName());
        }
        result.setAlternateRoles(alternates);

        // Detect experience level
        result.setExperienceLevel(detectExperienceLevel(lowerText));

        return result;
    }

    private boolean containsKeyword(String text, String keyword) {
        String regex = "\\b" + Pattern.quote(keyword.toLowerCase()) + "\\b";
        return Pattern.compile(regex).matcher(text).find();
    }

    private String detectExperienceLevel(String text) {
        int years = 0;
        Matcher m = Pattern.compile("(\\d+)\\+?\\s*(?:years?|yrs?)\\s*(?:of)?\\s*experience").matcher(text);
        if (m.find()) {
            try {
                years = Integer.parseInt(m.group(1));
            } catch (Exception ignored) {}
        }

        if (years >= 5 || text.contains("senior") || text.contains("lead engineer") || text.contains("tech lead") || text.contains("principal")) {
            return "Senior Level (4+ years)";
        } else if (years >= 2 || text.contains("intermediate") || text.contains("software engineer ii")) {
            return "Mid-Level Professional (2-4 years)";
        } else if (text.contains("junior") || text.contains("intern") || text.contains("b.tech") || text.contains("bachelor") || text.contains("fresh graduate") || text.contains("student")) {
            return "Entry-Level / Fresher (0-1 years)";
        } else {
            return "Aspiring Professional / Early Career";
        }
    }

    private String capitalize(String str) {
        if (str == null || str.isEmpty()) return str;
        String[] words = str.split(" ");
        StringBuilder sb = new StringBuilder();
        for (String w : words) {
            if (w.equalsIgnoreCase("api") || w.equalsIgnoreCase("sql") || w.equalsIgnoreCase("qa") 
                    || w.equalsIgnoreCase("aws") || w.equalsIgnoreCase("ui/ux") || w.equalsIgnoreCase("jwt")
                    || w.equalsIgnoreCase("ci/cd") || w.equalsIgnoreCase("rag") || w.equalsIgnoreCase("llm")) {
                sb.append(w.toUpperCase()).append(" ");
            } else if (!w.isEmpty()) {
                sb.append(Character.toUpperCase(w.charAt(0))).append(w.substring(1)).append(" ");
            }
        }
        return sb.toString().trim();
    }
}
