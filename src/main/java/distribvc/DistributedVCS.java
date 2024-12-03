package distribvc;

import distribvc.controller.RepositoryController;
import distribvc.view.CommandLineInterface;
import org.springframework.boot.CommandLineRunner;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.stereotype.Component;

import java.nio.file.Path;
import java.nio.file.Paths;


@SpringBootApplication
public class DistributedVCS {
    public static void main(String[] args) {
        SpringApplication.run(DistributedVCS.class, args);
    }

    @Component
    public class DistributedVCSRunner implements CommandLineRunner {
        private final RepositoryController controller;
        private final CommandLineInterface cli;

        public DistributedVCSRunner(RepositoryController controller) {
            this.controller = controller;
            this.cli = new CommandLineInterface(controller);
        }

        @Override
        public void run(String... args) throws Exception {
            if (args.length < 2) {
                System.out.println("Usage: java DistributedVCS <command> <directory>");
                return;
            }

            String command = args[0];
            Path workingDirectory = Paths.get(args[1]).toAbsolutePath().normalize();

            switch (command) {
                case "init" -> cli.initRepository(workingDirectory);
                case "add" -> cli.stageFiles(workingDirectory, args);
                case "commit" -> cli.commit(workingDirectory, args);
                case "status" -> cli.showStatus(workingDirectory);
                case "log" -> cli.showLog(workingDirectory);
                default -> {
                    System.out.println("Unknown command: " + command);
                }
            }
        }
    }
}