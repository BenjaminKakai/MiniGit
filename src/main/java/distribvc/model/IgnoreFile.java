package distribvc.model;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

public class IgnoreFile {
    private final Set<String> ignoredPatterns;

    public IgnoreFile() {
        this.ignoredPatterns = new HashSet<>();
    }

    public void addIgnoredPattern (String pattern) {
        ignoredPatterns.add(pattern);

    }

    public void loadFromFile(Path ignoreFilePath) throws IOException {
        if (Files.exists(ignoreFilePath)) {
            List<String> patterns = Files.readAllLines(ignoreFilePath);
            ignoredPatterns.addAll(
                    patterns.stream()
                            .filter(line -> !line.trim().isEmpty() && !line.startsWith("#"))
                            .collect(Collectors.toSet())
            );
        }
    }

    public boolean shouldIgnore(Path filePath) {
        return ignoredPatterns.stream()
                .anyMatch(pattern ->
                        filePath.toString().matches(convertGlobalToRegex(pattern)));
    }

    private String convertGlobalToRegex (String glob) {
        //simple glo to regex conversion
        return "^" + glob
                .replace(".", "\\.")
                .replace("*", ".*")
                .replace("?", ".")
                + "$";
    }

    public Set<String> getIgnoredPatterns() {
        return new HashSet<>(ignoredPatterns);
    }
}
