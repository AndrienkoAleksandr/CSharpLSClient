package org.eclipse.che.ls;

import org.eclipse.che.dotnet.DotNetRestoreCommand;
import org.eclipse.che.utils.ProjectGenerator;
import org.eclipse.lsp4j.*;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.Test;

import java.io.*;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Collections;
import java.util.List;
import java.util.Properties;

import static java.nio.charset.StandardCharsets.UTF_8;
import static org.testng.AssertJUnit.assertEquals;
import static org.testng.AssertJUnit.assertNotNull;
import static org.testng.AssertJUnit.assertTrue;

public class LanguageServerConnectorTest {
    private static final String PROJECT_NAME = "aspnet2.0";
    private static final String TEST_FILE = "Program.cs";

    private LanguageServerConnector lsConnector;
    private Path testPrjPath;
    private String fileContent;
    private Path filePath;

    @BeforeClass
    public void setUp() throws Exception {
        Properties properties = new Properties();
        InputStream in = LanguageServerConnectorTest.class.getClassLoader().getResourceAsStream("test.properties");
        properties.load(in);

        String serverExecPath = properties.getProperty("node.modules.path");

        testPrjPath = ProjectGenerator.generateTestProject(PROJECT_NAME);

        filePath = testPrjPath.resolve(TEST_FILE);
        fileContent = new String(Files.readAllBytes(filePath), UTF_8);

        new DotNetRestoreCommand(testPrjPath.toAbsolutePath().toString()).start();

        lsConnector = new LanguageServerConnector(serverExecPath);
    }

    @Test(priority = 0)
    public void lsShouldbeInitialized() throws Exception {
        InitializeResult initializeResult = lsConnector.initialize(testPrjPath);
        ServerCapabilities capabilities = initializeResult.getCapabilities();
        assertEquals(capabilities.getTextDocumentSync().getLeft(), TextDocumentSyncKind.Incremental);
        assertEquals(capabilities.getTextDocumentSync().getRight(), null);
        assertEquals(capabilities.getHoverProvider(), Boolean.TRUE);
        assertNotNull(capabilities.getCompletionProvider());
        assertEquals(capabilities.getCompletionProvider().getResolveProvider(), null);
        assertEquals(capabilities.getCompletionProvider().getTriggerCharacters(), null);
        assertEquals(capabilities.getSignatureHelpProvider().getTriggerCharacters(), Collections.singletonList("("));
        assertEquals(capabilities.getDefinitionProvider().booleanValue(), true);
        assertEquals(capabilities.getReferencesProvider().booleanValue(), true);
        assertEquals(capabilities.getDocumentHighlightProvider().booleanValue(), true);
        assertEquals(capabilities.getWorkspaceSymbolProvider().booleanValue(), true);
        assertEquals(capabilities.getCodeLensProvider().isResolveProvider(), true);
        assertEquals(capabilities.getDocumentFormattingProvider().booleanValue(), true);
        assertEquals(capabilities.getDocumentRangeFormattingProvider().booleanValue(), true);
        assertEquals(capabilities.getRenameProvider().booleanValue(), true);
    }

    @Test(priority = 1)
    public void checkOpenTestFile() throws Exception {
        lsConnector.openFile(filePath, fileContent);
    }

    @Test(priority = 2)
    public void codeHighLightShouldBeClearBecauseFileHasNiceState() {
        List<? extends DocumentHighlight> highLights = lsConnector.hightLight(filePath, fileContent);
        assertTrue(highLights.isEmpty());
    }

    @Test(priority = 3)
    public void checkListCompletion() {
        List<CompletionItem> competions = lsConnector.completion(filePath, new Position(0, 6));
        assertEquals(competions.size(), 4);
        assertEquals(competions.get(0).getLabel(), "aspnet2");
        assertEquals(competions.get(1).getLabel(), "Microsoft");
        assertEquals(competions.get(2).getLabel(), "static");
        assertEquals(competions.get(3).getLabel(), "System");
    }

}
