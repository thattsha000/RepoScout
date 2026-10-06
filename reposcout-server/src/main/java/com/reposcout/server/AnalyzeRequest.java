package com.reposcout.server;

public class AnalyzeRequest {
        String repoUrl;
        public AnalyzeRequest () {}
        public String getRepoUrl() {
            return repoUrl;
        }
        public void setRepoUrl(String repoUrl) {
            this.repoUrl = repoUrl;
        }

        public AnalyzeRequest (String repoUrl) {
            this.repoUrl = repoUrl;
        }
    }