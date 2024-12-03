package distribvc.model;

import com.google.gson.annotations.Expose;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.HashMap;
import java.util.Map;

public class Repository {
    @Expose
    private static final String REPO_DIR = ".distribvc";
    @Expose
    private static final String COMMITS_DIR = "commits";
    @Expose
    private static  final String STAGING_DIR = "staging";
    @Expose
    private static final String BRANCHES_DIR = "branches";
    @Expose
    private static final String CURRENT_BRANCH_FILE = "HEAD";
    @Expose
    private static final String IGNORE_FILE = ".distribvcignore";

    @Expose
    private final Path rootPath;
    @Expose
    private final Path repoPath;
    @Expose
    private final IgnoreFile ignoreFile;

    @Expose
    private Map<String, Branch> branches;
    @Expose
    private Branch currentBranch;

    public Repository (Path rootPath) throws IOException {
        this.rootPath = rootPath;
        this.repoPath = rootPath.resolve(REPO_DIR);
        this.branches = new HashMap<>();
        this.ignoreFile = new IgnoreFile();

        //Initialize repository structure
        initializeRepositoryStructure();

    }

    private void initializeRepositoryStructure() throws IOException {

        //create repository directories
        Files.createDirectories(repoPath);
        Files.createDirectories(repoPath.resolve(COMMITS_DIR));
        Files.createDirectories(repoPath.resolve(STAGING_DIR));
        Files.createDirectories(repoPath.resolve(BRANCHES_DIR));


        //Create initial branch
        Branch masterBranch = new Branch("Master");
        branches.put("master", masterBranch);
        currentBranch = masterBranch;

        //write current branch to head file
        Files.writeString(repoPath.resolve(CURRENT_BRANCH_FILE), "master");

        //create ignore file if not exists
        Path ignoreFilePath = rootPath.resolve(IGNORE_FILE);
        if (!Files.exists(ignoreFilePath)) {
            Files.createFile(ignoreFilePath);

            //default ignore patterns
            Files.writeString(ignoreFilePath,
                    "# Ignore specific files or directories\n" +
                    ".distribvc/\n" +
                    "*.log\n" +
                    ".DS_Store\n");
        }
        //Load ignore patterns
        ignoreFile.loadFromFile(ignoreFilePath);

    }

    //Getters and additional methods
    public Path getRootPath() {
        return rootPath;
    }

    public Path getRepoPath() {
        return repoPath;
    }

    public Branch getCurrentBranch() {
        return currentBranch;
    }

    public  IgnoreFile getIgnoreFile() {
        return ignoreFile;
    }

    public void createBranch (String branchName) {
        if (!branches.containsKey(branchName)) {
            Branch newBranch = new Branch (branchName);
            branches.put(branchName, newBranch);
        }

        else {
            throw new IllegalArgumentException("Branch already exists: " + branchName);
        }
    }

    public void  switchBranch (String branchName) {
        if (branches.containsKey(branchName)) {
            currentBranch = branches.get(branchName);
            try {
                Files.writeString(repoPath.resolve(CURRENT_BRANCH_FILE), branchName);
            }

            catch (IOException e) {
                throw new RuntimeException("Could noy update curreny branch", e);
            }
        } else {
            throw new IllegalArgumentException("Branch does not exist: " + branchName);
        }
    }



























}
