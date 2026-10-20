package com.reposcout.server;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

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
    private static final Pattern GITHUB_REPO = Pattern.compile("^https://github\\.com/([A-Za-z0-9_.-]+)/([A-Za-z0-9_.-]+?)(?:\\.git)?/?$");
    private final AnalyzerService analyzerService;
    private final RateLimiter rateLimiter;
        public AnalyzeController(AnalyzerService analyzerService, RateLimiter rateLimiter) {
            this.analyzerService = analyzerService;
            this.rateLimiter = rateLimiter;
        }
    @PostMapping("/analyze")
    public AnalyzeResponse analyze(@RequestBody AnalyzeRequest request, HttpServletRequest httpRequest) throws Exception {
        String repoUrl = normalizeRepoUrl(request.getRepoUrl());   // new: validate before anything else
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
        return new AnalyzeResponse(analyzerService.findOpportunities(repoUrl));
        } 
        private String normalizeRepoUrl(String raw) {
            if (raw == null) {
                throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                        "Expected https://github.com/owner/repo");
                }
            Matcher m = GITHUB_REPO.matcher(raw.trim());
            if (!m.matches()) {
                throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                    "Expected https://github.com/owner/repo");
                }
            String owner = m.group(1);
            String repo = m.group(2);
             if (owner.equals(".") || owner.equals("..") || repo.equals(".") || repo.equals("..")) {
                throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                    "Expected https://github.com/owner/repo");
            }
            return "https://github.com/" + owner + "/" + repo;
         }
    }  
