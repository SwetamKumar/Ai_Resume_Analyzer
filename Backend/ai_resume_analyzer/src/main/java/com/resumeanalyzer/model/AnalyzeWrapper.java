package com.resumeanalyzer.model;

public class AnalyzeWrapper {
    private AnalysisResponse analysis;
    private String extractedText;

    public AnalyzeWrapper(AnalysisResponse analysis, String extractedText) {
        this.analysis = analysis;
        this.extractedText = extractedText;
    }

    public AnalysisResponse getAnalysis() { return analysis; }
    public void setAnalysis(AnalysisResponse analysis) { this.analysis = analysis; }

    public String getExtractedText() { return extractedText; }
    public void setExtractedText(String extractedText) { this.extractedText = extractedText; }
}
