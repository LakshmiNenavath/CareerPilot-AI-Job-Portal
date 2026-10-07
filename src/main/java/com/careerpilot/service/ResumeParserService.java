package com.careerpilot.service;

import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.text.PDFTextStripper;
import org.apache.poi.xwpf.usermodel.XWPFDocument;
import org.apache.poi.xwpf.usermodel.XWPFParagraph;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.BufferedReader;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

@Service
public class ResumeParserService {

    private static final Pattern EMAIL_PATTERN = Pattern.compile(
            "[a-zA-Z0-9._%+-]+@[a-zA-Z0-9.-]+\\.[a-zA-Z]{2,6}"
    );

    private static final Pattern PHONE_PATTERN = Pattern.compile(
            "(\\+?\\d{1,3}[-.\\s]?)?(\\(?\\d{3}\\)?[-.\\s]?)?\\d{3}[-.\\s]?\\d{4}"
    );

    public String extractText(MultipartFile file) throws Exception {
        if (file == null || file.isEmpty()) {
            throw new IllegalArgumentException("The uploaded file is empty.");
        }

        String originalFilename = file.getOriginalFilename();
        if (originalFilename == null) {
            originalFilename = "uploaded_file";
        }
        String lowerName = originalFilename.toLowerCase();

        if (lowerName.endsWith(".pdf")) {
            return extractTextFromPdf(file.getInputStream());
        } else if (lowerName.endsWith(".docx")) {
            return extractTextFromDocx(file.getInputStream());
        } else if (lowerName.endsWith(".txt")) {
            return extractTextFromTxt(file.getInputStream());
        } else {
            // Attempt to read as text if unknown extension
            return extractTextFromTxt(file.getInputStream());
        }
    }

    public String extractTextFromPdf(InputStream inputStream) throws Exception {
        try (PDDocument document = PDDocument.load(inputStream)) {
            PDFTextStripper stripper = new PDFTextStripper();
            stripper.setSortByPosition(true);
            String text = stripper.getText(document);
            return cleanExtractedText(text);
        }
    }

    public String extractTextFromDocx(InputStream inputStream) throws Exception {
        try (XWPFDocument document = new XWPFDocument(inputStream)) {
            StringBuilder sb = new StringBuilder();
            for (XWPFParagraph p : document.getParagraphs()) {
                sb.append(p.getText()).append("\n");
            }
            return cleanExtractedText(sb.toString());
        }
    }

    public String extractTextFromTxt(InputStream inputStream) throws Exception {
        try (BufferedReader reader = new BufferedReader(new InputStreamReader(inputStream, StandardCharsets.UTF_8))) {
            String text = reader.lines().collect(Collectors.joining("\n"));
            return cleanExtractedText(text);
        }
    }

    public String cleanExtractedText(String text) {
        if (text == null) return "";
        // Replace non-printable characters and collapse multiple blank lines
        String cleaned = text.replaceAll("[\\r\\t]+", " ");
        cleaned = cleaned.replaceAll("[\\p{Cntrl}&&[^\n\r\t]]", "");
        cleaned = cleaned.replaceAll("[ ]{2,}", " ");
        cleaned = cleaned.replaceAll("\n{3,}", "\n\n");
        return cleaned.trim();
    }

    public String extractEmail(String text) {
        if (text == null) return null;
        Matcher matcher = EMAIL_PATTERN.matcher(text);
        if (matcher.find()) {
            return matcher.group();
        }
        return null;
    }

    public String extractPhone(String text) {
        if (text == null) return null;
        Matcher matcher = PHONE_PATTERN.matcher(text);
        if (matcher.find()) {
            return matcher.group().trim();
        }
        return null;
    }

    public String extractCandidateName(String text, String fallback) {
        if (text == null || text.isBlank()) return fallback;
        // Typically the candidate's name is on the first 1-3 lines before contact details
        String[] lines = text.split("\n");
        for (String line : lines) {
            String trimmed = line.trim();
            if (trimmed.length() > 2 && trimmed.length() < 50
                    && !trimmed.contains("@")
                    && !trimmed.matches(".*\\d{5,}.*")
                    && !trimmed.toLowerCase().contains("resume")
                    && !trimmed.toLowerCase().contains("curriculum")
                    && !trimmed.toLowerCase().contains("profile")
                    && !trimmed.toLowerCase().contains("education")) {
                // Ensure line has mostly alphabetic characters
                if (trimmed.matches("^[a-zA-Z\\s.'-]+$")) {
                    return trimmed;
                }
            }
        }
        return fallback;
    }
}
