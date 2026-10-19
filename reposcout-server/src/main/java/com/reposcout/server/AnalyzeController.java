package com.reposcout.server;

import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.server.ResponseStatusException;

import jakarta.servlet.http.HttpServletRequest;


@RestController 
@RequestMapping("/api")
public class AnalyzeController {

    private final AnalyzerService analyzerService;
    private final RateLimiter rateLimiter;
        public AnalyzeController(AnalyzerService analyzerService, RateLimiter rateLimiter) {
            this.analyzerService = analyzerService;
            this.rateLimiter = rateLimiter;
        }
    @PostMapping("/analyze")
    public AnalyzeResponse analyze(@RequestBody AnalyzeRequest request, HttpServletRequest httpRequest) throws Exception {
        String ip = httpRequest.getHeader("X-Forwarded-For");
        if (ip == null || ip.isBlank()) {
            //running locally there could be nothing in this header 
            ip = httpRequest.getRemoteAddr();
        } 
        else {
            ip = ip.split(",")[0].trim();
        }
        if (!rateLimiter.checkIP(ip)) {
            throw new ResponseStatusException(HttpStatus.TOO_MANY_REQUESTS, "Too many requests sent by this user within an hour");
            }
        return new AnalyzeResponse(analyzerService.findOpportunities(request.getRepoUrl()));
        } 
    }  
