package distribvc.model;

import java.nio.file.Path;

public class FileChange {
    public enum ChangeType {
        ADDED, MODIFIED, DELETED
    }

    private final Path filePath;
    private final ChangeType changeType;
    private final String content; //tracking file contents

    public FileChange (Path filepath, ChangeType changeType, String content) {
        this.filePath = filepath;
        this.changeType = changeType;
        this.content = content;
    }

    //Getters
    public Path getFilePath() {
        return filePath;
    }

    public ChangeType getChangeType() {
        return changeType;
    }

    public String getContent() {
        return content;
    }
}
