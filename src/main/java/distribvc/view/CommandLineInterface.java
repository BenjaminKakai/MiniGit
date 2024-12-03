package distribvc.view;


import distribvc.controller.RepositoryController;
import distribvc.model.Commit;
import distribvc.model.Repository;

import java.nio.file.Path;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Map;

public class CommandLineInterface {
    private final RepositoryController controller;

    public CommandLineInterface(RepositoryController controller) {
        this.controller = controller;
    }

    /**
     * Initialize a new repository
     * @param workingDirectory Directory where repository will be created
     */
    public void initRepository(Path workingDirectory) {
        try {
            Repository repository = controller.initRepository(workingDirectory);
            System.out.println("Initialized empty DistributedVCS repository in " +
                    repository.getRepoPath());
        } catch (Exception e) {
            System.err.println("Repository initialization failed: " + e.getMessage());
            System.exit(1);
        }
    }

    /**
     * Stage files for commit
     * @param workingDirectory Current working directory
     * @param args Command line arguments (first arg is "add", rest are file paths)
     */
    public void stageFiles(Path workingDirectory, String[] args) {
        try {
            Repository repository = new Repository(workingDirectory);
            Path[] filesToStage = new Path[args.length - 2];
            for (int i = 2; i < args.length; i++) {
                Path filePath = workingDirectory.resolve(args[i]).toAbsolutePath().normalize();
                filesToStage[i - 2] = filePath;
            }
            controller.stageFiles(repository, filesToStage);
            System.out.println("Files staged successfully.");
        } catch (Exception e) {
            System.err.println("Staging failed: " + e.getMessage());
            System.exit(1);
        }
    }

    /**
     * Commit staged changes
     * @param workingDirectory Current working directory
     * @param args Command line arguments
     */
    public void commit(Path workingDirectory, String[] args) {
        try {
            Repository repository = new Repository(workingDirectory);
            if (args.length < 3) {
                System.err.println("Commit requires a message. Usage: commit -m \"Commit message\"");
                System.exit(1);
            }
            String commitMessage = null;
            for (int i = 2; i < args.length; i++) {
                if (args[i].equals("-m") && i + 1 < args.length) {
                    commitMessage = args[i + 1];
                    break;
                }
            }
            if (commitMessage == null) {
                System.err.println("No commit message provided. Usage: commit -m \"Commit message\"");
                System.exit(1);
            }
            String author = System.getProperty("user.name");
            Commit commit = controller.commit(repository, commitMessage, author);
            if (commit != null) {
                System.out.println("Commit created: " + commit.getId());
                System.out.println("Message: " + commit.getMessage());
            } else {
                System.out.println("No changes to commit.");
            }
        } catch (Exception e) {
            System.err.println("Commit failed: " + e.getMessage());
            System.exit(1);
        }
    }

    /**
     * Show repository status
     * @param workingDirectory Current working directory
     */
    public void showStatus(Path workingDirectory) {
        try {
            Repository repository = new Repository(workingDirectory);
            RepositoryController.RepositoryStatus status = controller.getRepositoryStatus(repository);
            System.out.println("Repository Status:");
            System.out.println("Branch: " + repository.getCurrentBranch().getName());
            System.out.println("");
            Map<Path, RepositoryController.RepositoryStatus.FileStatus> stagedFiles = status.getStagedFiles();
            if (!stagedFiles.isEmpty()) {
                System.out.println("Staged Changes:");
                for (Map.Entry<Path, RepositoryController.RepositoryStatus.FileStatus> entry : stagedFiles.entrySet()) {
                    System.out.println("\t" + entry.getKey() + " [" + entry.getValue() + "]");
                }
                System.out.println("");
            }
            Map<Path, RepositoryController.RepositoryStatus.FileStatus> unstagedFiles = status.getUnstagedFiles();
            if (!unstagedFiles.isEmpty()) {
                System.out.println("Unstaged Changes:");
                for (Map.Entry<Path, RepositoryController.RepositoryStatus.FileStatus> entry : unstagedFiles.entrySet()) {
                    System.out.println("\t" + entry.getKey() + " [" + entry.getValue() + "]");
                }
                System.out.println("");
            }
            if (stagedFiles.isEmpty() && unstagedFiles.isEmpty()) {
                System.out.println("Working directory clean. No changes to commit.");
            }
        } catch (Exception e) {
            System.err.println("Could not get repository status: " + e.getMessage());
            System.exit(1);
        }
    }

    /**
     * Show commit log
     * @param workingDirectory Current working directory
     */
    public void showLog(Path workingDirectory) {
        try {
            Repository repository = new Repository(workingDirectory);
            List<Commit> commits = controller.getCommitLog(repository);
            System.out.println("Commit History:");
            System.out.println("===============");
            DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");
            for (Commit commit : commits) {
                System.out.println("Commit: " + commit.getId());
                System.out.println("Author: " + commit.getAuthor());
                System.out.println("Date:   " + commit.getTimestamp().format(formatter));
                System.out.println("Message: " + commit.getMessage());
                System.out.println("Changes:");
                commit.getChanges().forEach(change ->
                        System.out.println("\t" + change.getChangeType() + ": " + change.getFilePath())
                );
                System.out.println("");
            }
            if (commits.isEmpty()) {
                System.out.println("No commits yet.");
            }
        } catch (Exception e) {
            System.err.println("Could not retrieve commit log: " + e.getMessage());
            System.exit(1);
        }
    }
}
