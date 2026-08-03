package com.resumeanalyzer.controller;

import com.resumeanalyzer.model.*;
import com.resumeanalyzer.service.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.*;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.nio.charset.StandardCharsets;

@RestController
@RequestMapping("/api/resume")
public class ResumeController {

    @Autowired private GeminiService geminiService;
    @Autowired private PdfExtractorService pdfExtractorService;
    @Autowired private ResumePdfService resumePdfService;

    // ── Health ────────────────────────────────────────────────────────────────
    @GetMapping("/health")
    public ResponseEntity<String> health() {
        return ResponseEntity.ok("AI Resume Analyzer is running!");
    }

    // ── Analyze via PDF/TXT upload ─────────────────────────────────────────────
    // Returns BOTH the analysis result AND the extracted text
    // so frontend can use extracted text for the Fix step
    @PostMapping("/analyze")
    public ResponseEntity<?> analyzeResume(
            @RequestParam("resume") MultipartFile resumeFile,
            @RequestParam("jobDescription") String jobDescription) {
        try {
            String resumeText = extractText(resumeFile);
            if (resumeText == null || resumeText.isBlank())
                return ResponseEntity.badRequest().body("Could not extract text from the uploaded file.");
            if (jobDescription == null || jobDescription.isBlank())
                return ResponseEntity.badRequest().body("Job description cannot be empty.");

            AnalysisResponse analysis = geminiService.analyzeResume(resumeText, jobDescription);

            // Wrap: send extracted text back so frontend can use it for fixing
            return ResponseEntity.ok(new AnalyzeWrapper(analysis, resumeText));

        } catch (Exception e) {
            return ResponseEntity.internalServerError().body("Analysis failed: " + e.getMessage());
        }
    }

    // ── Analyze via plain text ─────────────────────────────────────────────────
    @PostMapping("/analyze-text")
    public ResponseEntity<?> analyzeText(
            @RequestParam("resumeText") String resumeText,
            @RequestParam("jobDescription") String jobDescription) {
        try {
            AnalysisResponse analysis = geminiService.analyzeResume(resumeText, jobDescription);
            // For text mode, extractedText = what user typed
            return ResponseEntity.ok(new AnalyzeWrapper(analysis, resumeText));
        } catch (Exception e) {
            return ResponseEntity.internalServerError().body("Analysis failed: " + e.getMessage());
        }
    }

    // ── Fix resume via AI ──────────────────────────────────────────────────────
    @PostMapping("/fix")
    public ResponseEntity<?> fixResume(@RequestBody FixRequest fixRequest) {
        try {
            if (fixRequest.getResumeText() == null || fixRequest.getResumeText().isBlank())
                return ResponseEntity.badRequest().body("Resume text is required.");
            if (fixRequest.getJobDescription() == null || fixRequest.getJobDescription().isBlank())
                return ResponseEntity.badRequest().body("Job description is required.");

            FixResponse fixed = geminiService.fixResume(
                fixRequest.getResumeText(),
                fixRequest.getJobDescription(),
                fixRequest.getMissingSkills(),
                fixRequest.getImprovements()
            );
            return ResponseEntity.ok(fixed);
        } catch (Exception e) {
            return ResponseEntity.internalServerError().body("Fix failed: " + e.getMessage());
        }
    }

    // ── Download fixed resume as PDF ───────────────────────────────────────────
    // Accepts JSON body (not FormData) to avoid encoding issues with large text
    @PostMapping("/download-pdf")
    public ResponseEntity<?> downloadPdf(@RequestBody DownloadRequest downloadRequest) {
        try {
            String resumeText    = downloadRequest.getResumeText();
            String candidateName = downloadRequest.getCandidateName();

            if (resumeText == null || resumeText.isBlank())
                return ResponseEntity.badRequest().body("Resume text is required.");

            byte[] pdfBytes = resumePdfService.generateResumePdf(resumeText, candidateName);

            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_PDF);
            headers.setContentDisposition(
                ContentDisposition.attachment().filename("optimized-resume.pdf").build()
            );
            headers.setContentLength(pdfBytes.length);

            return new ResponseEntity<>(pdfBytes, headers, HttpStatus.OK);
        } catch (Exception e) {
            return ResponseEntity.internalServerError().body("PDF generation failed: " + e.getMessage());
        }
    }

    // ── Helper ────────────────────────────────────────────────────────────────
    private String extractText(MultipartFile file) throws Exception {
        String contentType = file.getContentType();
        if (contentType != null && contentType.equals("application/pdf")) {
            return pdfExtractorService.extractText(file);
        }
        // FIX: Explicitly use UTF-8 instead of the JVM default charset.
        // On Windows (where this project was developed), the default is
        // windows-1252, which garbles special characters in .txt resumes
        // and can produce invalid JSON downstream.
        return new String(file.getBytes(), StandardCharsets.UTF_8);
    }
}
