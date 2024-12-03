package distribvc.model;

import java.util.ArrayList;
import java.util.List;

public class Branch {
    private final String name;
    private List<String> commitHistory;
    private String headCommitID;

    public Branch (String name) {
        this.name = name;
        this.commitHistory = new ArrayList<>();
        this.headCommitID = null;
    }

    public void addCommit (Commit commit) {
        commitHistory.add(commit.getId());
        headCommitID = commit.getId();
    }

    //Getters
    public String getName() {
        return name;
    }

    public List<String> getCommitHistory() {
        return new ArrayList<>(commitHistory);
    }

    public String getHeadCommitID() {
        return headCommitID;
    }
}
