package com.reposcout.server;

import java.util.List;

public class AnalyzeResponse {
    List<Opportunity> opportunities;
     public AnalyzeResponse(List<Opportunity> opportunities) {
        this.opportunities = opportunities;
    }

    public List<Opportunity> getOpportunities() {
        return opportunities;
    }

    public void setOpportunities(List<Opportunity> opportunities) {
        this.opportunities = opportunities;
    }
}
