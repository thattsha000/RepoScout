package com.reposcout.server;

import java.io.File;
import java.nio.file.Files;
import java.util.List;

import org.eclipse.jgit.api.Git;
import org.springframework.stereotype.Service;

@Service 
public class AnalyzerService {
    private final Harvester harvester;
    public AnalyzerService(Harvester harvester) {
        this.harvester = harvester;
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
             return List.of();
        }  
         catch (Exception e) {
        throw new IllegalArgumentException("Could not clone repo. Make sure you're passing a valid GitHub URL: " + repoUrl);
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
