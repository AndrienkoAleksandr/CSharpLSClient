package org.eclipse.che;

import org.eclipse.che.dotnet.DotNetRestoreCommand;
import org.eclipse.che.ls.DiagnosticsMessagesCollector;
import org.eclipse.che.ls.LanguageServerConnector;
import org.eclipse.che.utils.ProjectGenerator;
import org.eclipse.lsp4j.Position;

import java.nio.file.Files;
import java.nio.file.Path;

import static java.nio.charset.StandardCharsets.UTF_8;

public class App {

    private static final String TEST_FILE = "Program.cs";
    private static final String PROJECT_NAME = "aspnet2.0";

    public static void main(String[] args) throws Exception {
        if (args.length == 0) {
            throw new IllegalArgumentException("You forgot set up node server path like argument command line!");
        }

        String serverExecPath = args[0];
        Path prjPath = ProjectGenerator.generateTestProject(PROJECT_NAME);

        new DotNetRestoreCommand(prjPath.toAbsolutePath().toString()).start();

        DiagnosticsMessagesCollector messagesCollector = new DiagnosticsMessagesCollector();
        LanguageServerConnector languageServerConnector = new LanguageServerConnector(serverExecPath, messagesCollector);
        languageServerConnector.initialize(prjPath);

        Path filePath = prjPath.resolve(TEST_FILE);
        String content = new String(Files.readAllBytes(filePath), UTF_8);

        languageServerConnector.openFile(filePath, content);
        languageServerConnector.hightLight(filePath, content);
        languageServerConnector.completion(filePath, new Position(0, 6));

        // Todo format doesn't work, lsp4j 0.2 contains bug...
        //languageServerConnector.format(filePath, new Position(0, 0), new Position());
    }
}
