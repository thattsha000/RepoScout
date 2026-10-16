package com.reposcout.server;

import java.io.File;
import java.nio.file.Files;
import java.util.List;

import org.eclipse.jgit.api.Git;
import org.springframework.stereotype.Service;

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
            Git.cloneRepository()
               .setURI(repoUrl)
               .setDirectory(tempDir)
               .setDepth(1)         // shallow clone — only the latest commit
               .call();
            System.out.println("Cloned to: " + tempDir.getAbsolutePath());

            //harvest signals from the files 
            List<RawSignal> signals = harvester.harvest(tempDir);
            System.out.println("Found " + signals.size() + " signals");
            signals.forEach(s -> System.out.println(s.getFile() + ":" + s.getLine() + " → " + s.getText()));
             return rankerService.rank(signals, repoUrl);
        }  
         catch (Exception e) {
            e.printStackTrace();
            throw new IllegalArgumentException("Could not analyze repo: " + repoUrl, e);
         }
        finally {
            // delete the temp folder when done
            deleteDirectory(tempDir);
        }
    }

    private void deleteDirectory(File dir) {
        if (dir.isDirectory()) {
            for (File child : dir.listFiles()) {
                deleteDirectory(child);
            }
        }
        dir.delete();
        }
}
