package com.reposcout.server;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Stream;

import org.springframework.stereotype.Service;

@Service 
public class Harvester {


    //skip dirs 
    private static final List<String> SKIP_DIRS = List.of(
    "node_modules", "vendor", ".git", "dist", "build", 
    "target", "docs", "__pycache__", ".github"
);
    //only go through source files
    private static final List<String> SCAN_EXTENSIONS = List.of(
    ".py", ".js", ".ts", ".java", ".go", ".rb", ".rs"
    );

    //using patterns for better matching 
    private static final Pattern TAG = Pattern.compile("\\b(TODO|FIXME|XXX)\\b");   // case-sensitive, whole word
    private static final Pattern SKIPPED_TEST = Pattern.compile(
    "@pytest\\.mark\\.(skip|xfail)\\b" +
    "|@unittest\\.skip\\b" +
    "|@Disabled\\b|@Ignore\\b" +
    "|\\b(it|test|describe)\\.skip\\(" +
    "|#\\[ignore");
    private static final int CONTEXT_WINDOW = 2;

    public Harvester() {}
    public List<RawSignal> harvest(File dir) {
        Path start = Paths.get(dir.getAbsolutePath());
        List<RawSignal> signals = new ArrayList<>();
        try (Stream<Path> walk = Files.walk(start)) {
            walk.filter(Files::isRegularFile).filter(path -> shouldScan(path)).forEach( path -> read_path_lines(path, signals, start));
        }
        catch (IOException e) 
         {
            System.out.println("Encountered error");
        }
        return signals;

    }
    public void read_path_lines(Path path, List<RawSignal> signals, Path startPath) {
        try {
            //grab the lines and then relativize them 
            String relativePath = startPath.relativize(path).toString();
            List<String> lines = Files.readAllLines(path);
            for (int i = 0; i < lines.size(); i++) {
                 if (isSignal(lines.get(i))) {
                    List<String> context = new ArrayList<>();
                    int min = i  - CONTEXT_WINDOW;
                    int max = i + CONTEXT_WINDOW;
                    if (min < 0) {
                        min = 0;
                    }
                    if (max >= lines.size()) {
                        max = lines.size() - 1;
                    }
                    for (int y = min; y <= max; y++) {
                        context.add(lines.get(y));
                    }
                    RawSignal signal = new RawSignal(i + 1, relativePath, lines.get(i), context);
                    signals.add(signal);
                }  
            }

        } catch (IOException e) {
            //continue
        }

    }
    private boolean shouldScan(Path path) {
    String name = path.toString().toLowerCase();
    // skip if inside a junk directory
    for (String dir : SKIP_DIRS) {
        if (name.contains("/" + dir + "/")) return false;
    }
    // only scan known source file extensions
    return SCAN_EXTENSIONS.stream().anyMatch(name::endsWith);
    }
    private boolean isSignal(String line) {
        if (SKIPPED_TEST.matcher(line).find()) return true;
        Matcher m = TAG.matcher(line);
        if (!m.find()) return false;
        int c = commentStart(line);
        return c != -1 && c < m.start();   // comment begins before the keyword
    }

    private int commentStart(String line) {
    String t = line.stripLeading();
    if (t.startsWith("*")) return line.length() - t.length();  // inside /* ... */
    int best = -1;
    for (String tok : List.of("#", "//", "/*")) {
        int i = line.indexOf(tok);
        if (i != -1 && (best == -1 || i < best)) best = i;
    }
    return best;
}

}
