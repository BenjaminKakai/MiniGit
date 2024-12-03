package distribvc.controller;

import distribvc.model.Repository;
import distribvc.model.Commit;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.List;

@RestController
@RequestMapping("/api/repository")
public class RepositoryRestController {
    @Autowired
    private RepositoryController repositoryController;


    @PostMapping("/init")
    public ResponseEntity<?> initRepository(@RequestParam String directory) {
        try {
            Path rootPath = Paths.get(directory);
            Repository repository = repositoryController.initRepository(rootPath);
            return ResponseEntity.ok(repository);
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(e.getMessage());
        }
    }

    @PostMapping("/stage")
    public ResponseEntity<?> stageFiles(@RequestParam String repoPath, @RequestBody List<String> filePaths) {
        try {
            Repository repository = repositoryController.loadRepository(Paths.get(repoPath));
            Path[] paths = filePaths.stream()
                    .map(filePath -> repository.getRootPath().resolve(filePath)) // Use getRootPath() and resolve
                    .toArray(Path[]::new);

            repositoryController.stageFiles(repository, paths);
            return ResponseEntity.ok().build();
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(e.getMessage());
        }
    }

    @PostMapping("/commit")
    public ResponseEntity<?> commit(@RequestParam String repoPath,
                                    @RequestParam String message,
                                    @RequestParam String author) {
        try {
            Repository repository = repositoryController.loadRepository(Paths.get(repoPath));
            Commit commit = repositoryController.commit(repository, message, author);
            return ResponseEntity.ok(commit);
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(e.getMessage());
        }
    }

    @GetMapping("/status")
    public ResponseEntity<?> getStatus(@RequestParam String repoPath) {
        try {
            Repository repository = repositoryController.loadRepository(Paths.get(repoPath));
            RepositoryController.RepositoryStatus status = repositoryController.getRepositoryStatus(repository);
            return ResponseEntity.ok(status);
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(e.getMessage());
        }
    }

    @GetMapping("/log")
    public ResponseEntity<?> getCommitLog(@RequestParam String repoPath) {
        try {
            Repository repository = repositoryController.loadRepository(Paths.get(repoPath));
            List<Commit> commitLog = repositoryController.getCommitLog(repository);
            return ResponseEntity.ok(commitLog);
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(e.getMessage());
        }
    }
}