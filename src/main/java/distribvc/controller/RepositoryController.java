package distribvc.controller;

import java.io.IOException;
import java.nio.file.*;
import java.nio.file.attribute.BasicFileAttributes;
import java.util.*;
import java.util.stream.Collectors;

import com.google.gson.Gson;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import distribvc.model.Repository;
import distribvc.model.Branch;
import distribvc.model.Commit;
import distribvc.model.IgnoreFile;
import distribvc.model.FileChange;
import org.springframework.stereotype.Component;
import org.springframework.beans.factory.annotation.Autowired;

@Component
public class RepositoryController {
    private static final Logger logger = LoggerFactory.getLogger(RepositoryController.class);
    private static final String REPO_DIR = ".distribvc";
    private static final String COMMITS_DIR = "commits";
    private static final String STAGING_DIR = "staging";

    private final Gson gson;

    @Autowired
    public RepositoryController(Gson gson) {
        this.gson = gson;
    }

    /**
     * Initialize a new repository in the given directory
     * @param rootPath Path where the repository should be created
     * @return Repository the initialized repository
     */
    public Repository initRepository(Path rootPath) {
        try {
            // Check if repository already exists
            Path repoPath = rootPath.resolve(REPO_DIR);
            if (Files.exists(repoPath)) {
                throw new IllegalStateException("Repository already exists in this directory");
            }

            // Create and return the new repository
            return new Repository(rootPath);
        } catch (IOException e) {
            logger.error("Failed to initialize repository", e);
            throw new RuntimeException("Could not initialize repository", e);
        }
    }

    /**
     * Load an existing repository from the given root path
     * @param rootPath Path where the repository is located
     * @return Repository the loaded repository
     */
    public Repository loadRepository(Path rootPath) {
        try {
            // Check if repository exists
            Path repoPath = rootPath.resolve(REPO_DIR);
            if (!Files.exists(repoPath)) {
                throw new IllegalStateException("No repository exists in this directory");
            }

            // Create and return the repository
            return new Repository(rootPath);
        } catch (IOException e) {
            logger.error("Failed to load repository", e);
            throw new RuntimeException("Could not load repository", e);
        }
    }

    /**
     * Stage files for commit
     * @param repository The repository where files are being staged
     * @param filePaths Paths of files to be staged
     */
    public void stageFiles(Repository repository, Path... filePaths) {
        Path stagingPath = repository.getRepoPath().resolve(STAGING_DIR);
        IgnoreFile ignoreFile = repository.getIgnoreFile();

        for (Path filePath : filePaths) {
            try {
                // Skip ignored files
                if (ignoreFile.shouldIgnore(filePath)) {
                    logger.info("Skipping ignored file: {}", filePath);
                    continue;
                }

                // Validate file exists and is readable
                if (!Files.exists(filePath) || !Files.isReadable(filePath)) {
                    logger.warn("File does not exist or is not readable: {}", filePath);
                    continue;
                }

                // Compute relative path
                Path relativePath = repository.getRootPath().relativize(filePath);
                Path stagedFilePath = stagingPath.resolve(relativePath);

                // Ensure parent directories exist
                Files.createDirectories(stagedFilePath.getParent());

                // Copy file to staging area
                Files.copy(filePath, stagedFilePath, StandardCopyOption.REPLACE_EXISTING);
                logger.info("Staged file: {}", relativePath);
            } catch (IOException e) {
                logger.error("Error staging file: " + filePath, e);
                throw new RuntimeException("Could not stage file", e);
            }
        }
    }

    /**
     * Commit staged changes
     * @param repository Repository where commit is happening
     * @param message Commit message
     * @param author Author of the commit
     * @return Commit The created commit
     */
    public Commit commit(Repository repository, String message, String author) {
        Path stagingPath = repository.getRepoPath().resolve(STAGING_DIR);
        Path commitsPath = repository.getRepoPath().resolve(COMMITS_DIR);
        Branch currentBranch = repository.getCurrentBranch();

        try {
            // Collect staged files and their changes
            List<FileChange> changes = collectStagedChanges(repository, stagingPath);

            if (changes.isEmpty()) {
                logger.warn("No changes to commit");
                return null;
            }

            // Determine parent commit ID
            String parentCommitId = currentBranch.getHeadCommitID();

            // Create new commit
            Commit newCommit = new Commit(message, parentCommitId, changes, author);

            // Save commit metadata
            Path commitPath = commitsPath.resolve(newCommit.getId() + ".json");
            Files.writeString(commitPath, gson.toJson(newCommit));

            // Add commit to branch
            currentBranch.addCommit(newCommit);

            // Clear staging area
            clearStagingArea(stagingPath);

            logger.info("Committed changes: {}", newCommit.getId());
            return newCommit;
        } catch (IOException e) {
            logger.error("Commit failed", e);
            throw new RuntimeException("Could not commit changes", e);
        }
    }

    /**
     * Collect changes of staged files
     * @param repository The repository
     * @param stagingPath Path to staging directory
     * @return List of file changes
     */
    private List<FileChange> collectStagedChanges(Repository repository, Path stagingPath) throws IOException {
        List<FileChange> changes = new ArrayList<>();

        Files.walkFileTree(stagingPath, new SimpleFileVisitor<Path>() {
            @Override
            public FileVisitResult visitFile(Path file, BasicFileAttributes attrs) throws IOException {
                // Compute relative path
                Path relativePath = stagingPath.relativize(file);
                Path originalPath = repository.getRootPath().resolve(relativePath);

                // Determine change type
                FileChange.ChangeType changeType;
                if (!Files.exists(originalPath)) {
                    changeType = FileChange.ChangeType.ADDED;
                } else {
                    changeType = Files.mismatch(originalPath, file) >= 0
                            ? FileChange.ChangeType.MODIFIED
                            : FileChange.ChangeType.DELETED;
                }

                // Read file content
                String content = changeType != FileChange.ChangeType.DELETED
                        ? Files.readString(file)
                        : null;

                changes.add(new FileChange(relativePath, changeType, content));
                return FileVisitResult.CONTINUE;
            }
        });

        return changes;
    }

    /**
     * Clear the staging area after commit
     * @param stagingPath Path to staging directory
     */
    private void clearStagingArea(Path stagingPath) throws IOException {
        Files.walkFileTree(stagingPath, new SimpleFileVisitor<Path>() {
            @Override
            public FileVisitResult visitFile(Path file, BasicFileAttributes attrs) throws IOException {
                Files.delete(file);
                return FileVisitResult.CONTINUE;
            }

            @Override
            public FileVisitResult postVisitDirectory(Path dir, IOException exc) throws IOException {
                if (!dir.equals(stagingPath)) {
                    Files.delete(dir);
                }
                return FileVisitResult.CONTINUE;
            }
        });
    }

    /**
     * Get commit log for a branch
     * @param repository The repository
     * @return List of commits in chronological order
     */

    public List<Commit> getCommitLog(Repository repository) {
        Path commitsPath = repository.getRepoPath().resolve(COMMITS_DIR);
        Branch currentBranch = repository.getCurrentBranch();

        try {
            List<Commit> commits = currentBranch.getCommitHistory().stream()
                    .map(commitId -> {
                        try {
                            Path commitFile = commitsPath.resolve(commitId + ".json");
                            String commitJson = Files.readString(commitFile);
                            return gson.fromJson(commitJson, Commit.class);
                        } catch (IOException e) {
                            logger.error("Could not read commit: " + commitId, e);
                            return null;
                        }
                    })
                    .filter(Objects::nonNull)
                    .collect(Collectors.toList());

            // Add a log to help diagnose empty commit history
            if (commits.isEmpty()) {
                logger.warn("No commits found in branch: {}", currentBranch.getName());
            }

            return commits;
        } catch (Exception e) {
            logger.error("Error retrieving commit log", e);
            throw new RuntimeException("Could not retrieve commit log", e);
        }
    }


    /**
     * Get repository status
     * @param repository The repository
     * @return RepositoryStatus containing staged and unstaged changes
     */

    public RepositoryStatus getRepositoryStatus(Repository repository) {
        Path stagingPath = repository.getRepoPath().resolve(STAGING_DIR);
        IgnoreFile ignoreFile = repository.getIgnoreFile();

        try {
            RepositoryStatus status = new RepositoryStatus();

            Files.walkFileTree(repository.getRootPath(), new SimpleFileVisitor<Path>() {
                @Override
                public FileVisitResult visitFile(Path file, BasicFileAttributes attrs) throws IOException {
                    // Skip files inside .distribvc directory
                    if (file.startsWith(repository.getRepoPath())) {
                        return FileVisitResult.CONTINUE;
                    }

                    // Skip ignored files
                    if (ignoreFile.shouldIgnore(file)) {
                        return FileVisitResult.CONTINUE;
                    }

                    Path relativePath = repository.getRootPath().relativize(file);
                    Path stagedFile = stagingPath.resolve(relativePath);

                    if (!Files.exists(stagedFile)) {
                        status.addUnstagedFile(relativePath, RepositoryStatus.FileStatus.UNTRACKED);
                    } else if (!Files.isSameFile(file, stagedFile)) {
                        status.addUnstagedFile(relativePath, RepositoryStatus.FileStatus.MODIFIED);
                    }

                    return FileVisitResult.CONTINUE;
                }
            });

            // Walk through staging directory to get staged changes
            Files.walkFileTree(stagingPath, new SimpleFileVisitor<Path>() {
                @Override
                public FileVisitResult visitFile(Path file, BasicFileAttributes attrs) throws IOException {
                    Path relativePath = stagingPath.relativize(file);
                    status.addStagedFile(relativePath, RepositoryStatus.FileStatus.STAGED);
                    return FileVisitResult.CONTINUE;
                }
            });

            return status;
        } catch (IOException e) {
            logger.error("Could not get repository status", e);
            throw new RuntimeException("Could not retrieve repository status", e);
        }
    }


    /**
     * Repository status representation
     */
    public static class RepositoryStatus {
        public enum FileStatus {
            UNTRACKED, MODIFIED, STAGED
        }

        private Map<Path, FileStatus> unstagedFiles = new HashMap<>();
        private Map<Path, FileStatus> stagedFiles = new HashMap<>();

        public void addUnstagedFile(Path path, FileStatus status) {
            unstagedFiles.put(path, status);
        }

        public void addStagedFile(Path path, FileStatus status) {
            stagedFiles.put(path, status);
        }

        public Map<Path, FileStatus> getUnstagedFiles() {
            return new HashMap<>(unstagedFiles);
        }

        public Map<Path, FileStatus> getStagedFiles() {
            return new HashMap<>(stagedFiles);
        }
    }
}