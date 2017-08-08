package org.eclipse.che;

import org.apache.log4j.Logger;
import org.eclipse.che.dotnet.DotNetRestoreCommand;
import org.eclipse.che.ls.LanguageClientImpl;
import org.eclipse.che.ls.LanguageServerConnector;
import org.eclipse.che.utils.ProjectGenerator;
import org.eclipse.lsp4j.Position;
import org.eclipse.lsp4j.jsonrpc.Launcher;
import org.eclipse.lsp4j.services.LanguageServer;

import java.nio.file.Files;
import java.nio.file.Path;

import static java.nio.charset.StandardCharsets.UTF_8;

public class App {

    private final static Logger logger = Logger.getLogger(LanguageServerConnector.class);

    private static final String TEST_FILE = "Program.cs";
    private static final String PROJECT_NAME = "aspnet2.0";

    public static void main(String[] args) throws Exception {
        if (args.length == 0) {
            throw new RuntimeException("You forgot set up node server path like argument command line!");
        }

        String serverExecPath = args[0];

        Path projectPath = ProjectGenerator.generateTestProject(PROJECT_NAME);

        new DotNetRestoreCommand(projectPath.toAbsolutePath().toString()).start();

        logger.info("Try to start Language server process by node server path: " + serverExecPath);

        ProcessBuilder processBuilder = new ProcessBuilder("node", serverExecPath);
        Process lspProcess = processBuilder.start();

        System.out.println("Try to connect to language server ........");

        Launcher<LanguageServer> launcher = Launcher.createLauncher(new LanguageClientImpl(),
                                                                    LanguageServer.class,
                                                                    lspProcess.getInputStream(),
                                                                    lspProcess.getOutputStream());
        launcher.startListening();
        LanguageServer server = launcher.getRemoteProxy();

        LanguageServerConnector languageServerConnector = new LanguageServerConnector();

        try {
            languageServerConnector.initialize(server, projectPath);
        } catch (Exception e) {
            server.shutdown();
            server.exit(); //todo kill process
            return;
        }

        Path filePath = projectPath.resolve(TEST_FILE);
        String content = new String(Files.readAllBytes(filePath), UTF_8);

        languageServerConnector.openFile(server, filePath, content);
        languageServerConnector.hightLight(server, filePath, content);
        languageServerConnector.completion(server, filePath);

        // Todo format doesn't work, lsp4j 0.2 contains bug...
        //languageServerConnector.format(server, filePath, new Position(0, 0), new Position());
    }
}
