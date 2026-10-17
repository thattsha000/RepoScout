package com.reposcout.server;

import java.io.File;
import java.nio.file.Files;
import java.util.List;

import org.eclipse.jgit.api.Git;
import org.eclipse.jgit.api.errors.GitAPIException;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

@Service 
public class AnalyzerService {
    private final Harvester harvester;
    private final RankerService rankerService;
    public AnalyzerService(Harvester harvester, RankerService rankerService) {
        this.harvester = harvester;
        this.rankerService = rankerService;
    }
    public List<Opportunity> findOpportunities(String repoUrl) throws Exception {
        File tempDir = Files.createTempDirectory("reposcout-").toFile();
        try {
            try (Git git = Git.cloneRepository()
            .setURI(repoUrl)
            .setDirectory(tempDir)
            .setDepth(1)
            .call())
            {//no work needs to be done here 
            } 
            catch (GitAPIException e) {
             throw new ResponseStatusException(
                    HttpStatus.BAD_REQUEST, "Repo not found or not public: " + repoUrl); // passes up through the controller; Spring sends it as a 400
            }
            //harvest signals from the files 
            List<RawSignal> signals = harvester.harvest(tempDir);
            try {
                return rankerService.rank(signals, repoUrl);
            } 
            catch (Exception e) {
                 e.printStackTrace();
                throw new ResponseStatusException(
                 HttpStatus.BAD_GATEWAY, "Ranking failed for " + repoUrl, e);
                }         
            }  
        finally {
            // delete the temp folder when done
            deleteDirectory(tempDir);
        }
    }


  private void deleteDirectory(File dir) {
    File[] children = dir.listFiles();
    if (children != null) {
        for (File child : children) {
            deleteDirectory(child);
        }
    }
    dir.delete();
}
}
