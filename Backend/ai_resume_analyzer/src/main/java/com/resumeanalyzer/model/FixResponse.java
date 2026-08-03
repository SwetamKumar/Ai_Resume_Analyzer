package com.resumeanalyzer.model;

public class FixResponse {
    private String fixedResumeText;  // Full rewritten resume text
    private String changesSummary;   // What was changed and why

    public String getFixedResumeText() { return fixedResumeText; }
    public void setFixedResumeText(String fixedResumeText) { this.fixedResumeText = fixedResumeText; }

    public String getChangesSummary() { return changesSummary; }
    public void setChangesSummary(String changesSummary) { this.changesSummary = changesSummary; }
}
