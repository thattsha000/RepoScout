package com.reposcout.server;

import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;


@RestController 
@RequestMapping("/api")
public class AnalyzeController {


    private final AnalyzerService analyzerService;
        public AnalyzeController(AnalyzerService analyzerService) {
            this.analyzerService = analyzerService;
        }
    @PostMapping("/analyze")
    public AnalyzeResponse analyze(@RequestBody AnalyzeRequest request) throws Exception {
        return new AnalyzeResponse(analyzerService.findOpportunities(request.getRepoUrl()));
    }  
}
