package com.reposcout.server;

import java.io.File;
import java.nio.file.Files;
import java.util.List;
import java.util.Map;

import org.eclipse.jgit.api.Git;
import org.eclipse.jgit.api.errors.GitAPIException;
import org.eclipse.jgit.lib.Ref;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

@Service 
public class AnalyzerService {
    private final Harvester harvester;
    private final RankerService rankerService;
    private final ScanCache cache;
    public AnalyzerService(Harvester harvester, RankerService rankerService, ScanCache cache) {
        this.harvester = harvester;
        this.rankerService = rankerService;
        this.cache = cache;
    }
    public List<Opportunity> findOpportunities(String repoUrl) throws Exception {
        // new: check the cache before doing any work
        String sha = latestCommit(repoUrl);
        String key = repoUrl + "@" + sha;

        List<Opportunity> cached = cache.get(key);
        if (cached != null) {
            return cached;
        }
        File tempDir = Files.createTempDirectory("reposcout-").toFile();
        try {
            try (Git git = Git.cloneRepository()
            .setURI(repoUrl)
            .setDirectory(tempDir)
            .setDepth(1)
            .setTimeout(30)
            .call())
            {//no work needs to be done here 
            } 
            catch (GitAPIException e) {
             throw new ResponseStatusException(
                    HttpStatus.BAD_REQUEST, "Repo not found or not public: " + repoUrl); // passes up through the controller; Spring sends it as a 400
            }
            //harvest signals from the files 
            List<RawSignal> signals = harvester.harvest(tempDir);
            List<Opportunity> result;
            try {
                result = rankerService.rank(signals, repoUrl, sha);
            } catch (Exception e) {
                e.printStackTrace();
                throw new ResponseStatusException(
                    HttpStatus.BAD_GATEWAY, "Ranking failed for " + repoUrl, e);
            }
            cache.put(key, result);   //add it to cache on a successful ranking 
            return result;    
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
   private String latestCommit(String repoUrl) {
    try {
        Map<String, Ref> refs = Git.lsRemoteRepository()
                .setRemote(repoUrl)
                .setTimeout(10)
                .callAsMap();
        Ref head = refs.get("HEAD");
        if (head == null) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Repo has no commits: " + repoUrl);
        }
        return head.getObjectId().name();   // the 40-character SHA
    } catch (GitAPIException e) {
        throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Repo not found or not public: " + repoUrl);
    }
    }
}
