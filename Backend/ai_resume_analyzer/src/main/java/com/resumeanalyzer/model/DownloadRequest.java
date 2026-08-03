package com.resumeanalyzer.model;

public class DownloadRequest {
    private String resumeText;
    private String candidateName;

    public String getResumeText() { return resumeText; }
    public void setResumeText(String resumeText) { this.resumeText = resumeText; }

    public String getCandidateName() { return candidateName; }
    public void setCandidateName(String candidateName) { this.candidateName = candidateName; }
}
