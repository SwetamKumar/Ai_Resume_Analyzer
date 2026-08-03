package com.resumeanalyzer.service;

import org.apache.pdfbox.pdmodel.*;
import org.apache.pdfbox.pdmodel.common.PDRectangle;
import org.apache.pdfbox.pdmodel.font.*;
import org.apache.pdfbox.pdmodel.graphics.color.PDColor;
import org.apache.pdfbox.pdmodel.graphics.color.PDDeviceRGB;
import org.springframework.stereotype.Service;

import java.io.*;
import java.util.*;

@Service
public class ResumePdfService {

    private static final float MARGIN       = 50f;
    private static final float FONT_NORMAL  = 10f;
    private static final float FONT_HEADING = 12f;
    private static final float FONT_NAME    = 20f;
    private static final float FONT_SUBTITLE = 11f;
    private static final float LINE_HEIGHT  = 16f;
    private static final float PAGE_WIDTH   = PDRectangle.A4.getWidth();
    private static final float PAGE_HEIGHT  = PDRectangle.A4.getHeight();
    private static final float USABLE_WIDTH = PAGE_WIDTH - 2 * MARGIN;

    public byte[] generateResumePdf(String resumeText, String candidateName) throws IOException {

        try (PDDocument doc = new PDDocument();
             ByteArrayOutputStream out = new ByteArrayOutputStream()) {

            // Use built-in fonts — guaranteed to work, no external deps
            PDFont boldFont   = new PDType1Font(Standard14Fonts.FontName.HELVETICA_BOLD);
            PDFont normalFont = new PDType1Font(Standard14Fonts.FontName.HELVETICA);
            PDFont monoFont   = new PDType1Font(Standard14Fonts.FontName.COURIER);

            // Clean and split the resume into lines
            String cleanedText = cleanText(resumeText);
            String[] rawLines  = cleanedText.split("\n");

            List<LineEntry> entries = new ArrayList<>();
            for (String line : rawLines) {
                String trimmed = line.trim();
                if (trimmed.isEmpty()) {
                    entries.add(new LineEntry("", LineType.BLANK));
                    continue;
                }
                LineType type = classifyLine(trimmed);
                // Word-wrap long lines
                List<String> wrapped = wrapText(trimmed, type == LineType.HEADING ? boldFont : normalFont,
                                                type == LineType.HEADING ? FONT_HEADING : FONT_NORMAL,
                                                type == LineType.BULLET ? USABLE_WIDTH - 15 : USABLE_WIDTH);
                for (int i = 0; i < wrapped.size(); i++) {
                    entries.add(new LineEntry(wrapped.get(i), i == 0 ? type : LineType.CONTINUATION));
                }
            }

            // ── First page ──────────────────────────────────────────────────────
            PDPage page = new PDPage(PDRectangle.A4);
            doc.addPage(page);
            PDPageContentStream cs = openStream(doc, page);

            // ── Header ──────────────────────────────────────────────────────────
            // Dark navy background
            cs.setNonStrokingColor(new PDColor(new float[]{0.08f, 0.08f, 0.14f}, PDDeviceRGB.INSTANCE));
            cs.addRect(0, PAGE_HEIGHT - 80, PAGE_WIDTH, 80);
            cs.fill();

            // Candidate name
            String name = (candidateName != null && !candidateName.isBlank())
                          ? candidateName
                          : extractNameFromResume(resumeText);
            cs.beginText();
            cs.setFont(boldFont, FONT_NAME);
            cs.setNonStrokingColor(new PDColor(new float[]{1f, 1f, 1f}, PDDeviceRGB.INSTANCE));
            cs.newLineAtOffset(MARGIN, PAGE_HEIGHT - 42);
            cs.showText(safe(name));
            cs.endText();

            // AI Optimized tag
            cs.beginText();
            cs.setFont(normalFont, 8f);
            cs.setNonStrokingColor(new PDColor(new float[]{0.6f, 0.78f, 1f}, PDDeviceRGB.INSTANCE));
            cs.newLineAtOffset(MARGIN, PAGE_HEIGHT - 62);
            cs.showText("AI Optimized by ResumeAI  |  " + java.time.LocalDate.now());
            cs.endText();

            float y = PAGE_HEIGHT - 100;

            // ── Body ────────────────────────────────────────────────────────────
            for (LineEntry entry : entries) {
                // Page break check
                if (y < MARGIN + 20) {
                    addFooter(cs, normalFont, doc.getNumberOfPages());
                    cs.close();
                    page = new PDPage(PDRectangle.A4);
                    doc.addPage(page);
                    cs = openStream(doc, page);
                    y = PAGE_HEIGHT - MARGIN;
                }

                switch (entry.type) {
                    case BLANK -> y -= LINE_HEIGHT * 0.5f;

                    case HEADING -> {
                        y -= 6;
                        // Purple accent line
                        cs.setStrokingColor(new PDColor(new float[]{0.43f, 0.40f, 0.96f}, PDDeviceRGB.INSTANCE));
                        cs.setLineWidth(1f);
                        cs.moveTo(MARGIN, y - 2);
                        cs.lineTo(MARGIN + USABLE_WIDTH, y - 2);
                        cs.stroke();
                        // Heading text
                        cs.beginText();
                        cs.setFont(boldFont, FONT_HEADING);
                        cs.setNonStrokingColor(new PDColor(new float[]{0.12f, 0.12f, 0.45f}, PDDeviceRGB.INSTANCE));
                        cs.newLineAtOffset(MARGIN, y);
                        cs.showText(safe(entry.text.toUpperCase()));
                        cs.endText();
                        y -= FONT_HEADING + 8;
                    }

                    case BULLET -> {
                        cs.beginText();
                        cs.setFont(normalFont, FONT_NORMAL);
                        cs.setNonStrokingColor(new PDColor(new float[]{0f, 0f, 0f}, PDDeviceRGB.INSTANCE));
                        cs.newLineAtOffset(MARGIN + 8, y);
                        // Replace bullet chars with simple hyphen for font safety
                        String bulletText = entry.text.replaceFirst("^[•\\-\\*]\\s*", "- ");
                        cs.showText(safe(bulletText));
                        cs.endText();
                        y -= LINE_HEIGHT;
                    }

                    case SUBHEADING -> {
                        cs.beginText();
                        cs.setFont(boldFont, FONT_NORMAL + 0.5f);
                        cs.setNonStrokingColor(new PDColor(new float[]{0.1f, 0.1f, 0.1f}, PDDeviceRGB.INSTANCE));
                        cs.newLineAtOffset(MARGIN, y);
                        cs.showText(safe(entry.text));
                        cs.endText();
                        y -= LINE_HEIGHT;
                    }

                    default -> { // NORMAL, CONTINUATION
                        cs.beginText();
                        cs.setFont(normalFont, FONT_NORMAL);
                        cs.setNonStrokingColor(new PDColor(new float[]{0.15f, 0.15f, 0.15f}, PDDeviceRGB.INSTANCE));
                        cs.newLineAtOffset(MARGIN, y);
                        cs.showText(safe(entry.text));
                        cs.endText();
                        y -= LINE_HEIGHT;
                    }
                }
            }

            addFooter(cs, normalFont, doc.getNumberOfPages());
            cs.close();
            doc.save(out);
            return out.toByteArray();
        }
    }

    // ── Helpers ───────────────────────────────────────────────────────────────

    private PDPageContentStream openStream(PDDocument doc, PDPage page) throws IOException {
        return new PDPageContentStream(doc, page, PDPageContentStream.AppendMode.OVERWRITE, true);
    }

    private void addFooter(PDPageContentStream cs, PDFont font, int pageNum) throws IOException {
        cs.beginText();
        cs.setFont(font, 7.5f);
        cs.setNonStrokingColor(new PDColor(new float[]{0.6f, 0.6f, 0.6f}, PDDeviceRGB.INSTANCE));
        cs.newLineAtOffset(MARGIN, 20);
        cs.showText("Generated by AI Resume Analyzer  |  Page " + pageNum);
        cs.endText();
    }

    private LineType classifyLine(String line) {
        String upper = line.toUpperCase().trim();
        String[] headings = {
            "SUMMARY", "OBJECTIVE", "PROFESSIONAL SUMMARY",
            "TECHNICAL SKILLS", "SKILLS", "LANGUAGES",
            "EXPERIENCE", "PROFESSIONAL EXPERIENCE", "WORK EXPERIENCE",
            "PROJECTS", "PROJECT",
            "EDUCATION",
            "CERTIFICATIONS", "ACHIEVEMENTS", "CONTACT", "INTERNSHIPS"
        };
        for (String h : headings) {
            if (upper.equals(h) || upper.equals(h + ":")) return LineType.HEADING;
        }
        if (line.startsWith("-") || line.startsWith("•") || line.startsWith("*")) return LineType.BULLET;
        // Lines ending with ) or containing – or | are likely job title lines
        if ((line.endsWith(")") && line.contains("(")) ||
            line.contains(" – ") || line.contains(" | ")) return LineType.SUBHEADING;
        return LineType.NORMAL;
    }

    private List<String> wrapText(String text, PDFont font, float fontSize, float maxWidth) throws IOException {
        List<String> lines = new ArrayList<>();
        String[] words = text.split(" ");
        StringBuilder current = new StringBuilder();
        for (String word : words) {
            String test = current.isEmpty() ? word : current + " " + word;
            try {
                float width = font.getStringWidth(safe(test)) / 1000 * fontSize;
                if (width > maxWidth && !current.isEmpty()) {
                    lines.add(current.toString());
                    current = new StringBuilder(word);
                } else {
                    if (!current.isEmpty()) current.append(" ");
                    current.append(word);
                }
            } catch (Exception e) {
                // Skip problematic characters
                if (!current.isEmpty()) current.append(" ");
                current.append(word.replaceAll("[^\\x20-\\x7E]", ""));
            }
        }
        if (!current.isEmpty()) lines.add(current.toString());
        return lines.isEmpty() ? List.of(text) : lines;
    }

    /** Remove all non-ASCII and PDF-unsafe characters */
    private String cleanText(String text) {
        if (text == null) return "";
        return text
            .replace("\r\n", "\n")
            .replace("\r", "\n")
            .replaceAll("[\\x00-\\x08\\x0B\\x0C\\x0E-\\x1F\\x7F]", "") // control chars
            .replaceAll("[^\u0000-\u007F]", "");                         // non-ASCII
    }

    /** Safe string for PDFBox — strip anything that could cause encoding errors */
    private String safe(String text) {
        if (text == null || text.isEmpty()) return "";
        return text.replaceAll("[^\\x20-\\x7E]", "").trim();
    }

    /** Try to pull first line of resume as candidate name */
    private String extractNameFromResume(String resumeText) {
        if (resumeText == null) return "Candidate";
        String[] lines = resumeText.trim().split("\n");
        for (String line : lines) {
            String t = line.trim();
            if (!t.isEmpty() && t.length() < 50 && !t.contains("@") && !t.contains(":")) {
                return t;
            }
        }
        return "Candidate";
    }

    // ── Inner types ───────────────────────────────────────────────────────────

    private enum LineType { BLANK, HEADING, SUBHEADING, BULLET, NORMAL, CONTINUATION }

    private static class LineEntry {
        String text; LineType type;
        LineEntry(String text, LineType type) { this.text = text; this.type = type; }
    }
}
