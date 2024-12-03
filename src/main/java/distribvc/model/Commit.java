package distribvc.model;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

public class Commit {
    private final String id;
    private final String message;
    private final LocalDateTime timestamp;
    private final String parentCommitID;
    private final List<FileChange> changes;
    private final String author;

    public Commit (String message, String parentCommitID, List<FileChange>changes, String author) {
        this.id = UUID.randomUUID().toString();
        this.message = message;
        this.timestamp = LocalDateTime.now();
        this.parentCommitID = parentCommitID;
        this.changes = changes;
        this.author = author;
    }

    // Getters
    public String getId() {
        return id;
    }

    public String getMessage() {
        return message;
    }

    public LocalDateTime getTimestamp() {
        return timestamp;
    }

    public String getParentCommitID() {
        return parentCommitID;
    }

    public List<FileChange> getChanges() {
        return changes;
    }

    public String getAuthor() {
        return author;
    }
}
