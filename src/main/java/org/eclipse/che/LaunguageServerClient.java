package org.eclipse.che;

import org.eclipse.lsp4j.ClientCapabilities;
import org.eclipse.lsp4j.CompletionList;
import org.eclipse.lsp4j.DidOpenTextDocumentParams;
import org.eclipse.lsp4j.DocumentHighlight;
import org.eclipse.lsp4j.InitializeParams;
import org.eclipse.lsp4j.InitializeResult;
import org.eclipse.lsp4j.MessageParams;
import org.eclipse.lsp4j.Position;
import org.eclipse.lsp4j.PublishDiagnosticsParams;
import org.eclipse.lsp4j.ShowMessageRequestParams;
import org.eclipse.lsp4j.TextDocumentIdentifier;
import org.eclipse.lsp4j.TextDocumentItem;
import org.eclipse.lsp4j.TextDocumentPositionParams;
import org.eclipse.lsp4j.jsonrpc.Launcher;
import org.eclipse.lsp4j.services.LanguageClient;
import org.eclipse.lsp4j.services.LanguageServer;
import org.eclipse.xtend.lib.macro.services.SourceTypeLookup;

import java.io.File;
import java.lang.management.ManagementFactory;
import java.net.URISyntaxException;
import java.net.URL;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutionException;
import java.util.stream.Collectors;

import static java.nio.charset.StandardCharsets.UTF_8;

public class LaunguageServerClient {

    private static final String LANGUAGE_ID = "csharp";
    private static final String TEST_FILE = "Program.cs";
    private static final String PROJECT_NAME = "aspnet-web-simple";

    private static LanguageClient languageClient = new LanguageClient() {

        @Override
        public void telemetryEvent(Object object) {
            System.out.println(object);
        }

        @Override
        public CompletableFuture<Void> showMessageRequest(ShowMessageRequestParams requestParams) {
            return CompletableFuture.completedFuture(null);
        }

        @Override
        public void showMessage(MessageParams messageParams) {
            System.out.println(messageParams);
        }

        @Override
        public void publishDiagnostics(PublishDiagnosticsParams diagnostics) {
            System.out.println(diagnostics);
        }

        @Override
        public void logMessage(MessageParams message) {
            System.out.println(message);
        }
    };

    public static void main(String[] args) throws Exception {
        //ProcessBuilder processBuilder = new ProcessBuilder("node", "/home/user/projects/aCute/org.eclipse.acute.omnisharpServer/server/languageserver/server.js");
        ProcessBuilder processBuilder = new ProcessBuilder("node", "/home/user/projects/omnisharp-node-client/languageserver/server.js");

        Process lspProcess = processBuilder.start();

        LanguageServer server;

        Launcher<LanguageServer> launcher = Launcher.createLauncher(languageClient,
                                                                    LanguageServer.class,
                                                                    lspProcess.getInputStream(),
                                                                    lspProcess.getOutputStream());
        launcher.startListening();
        server = launcher.getRemoteProxy();

        Path projectPath = getTestResourcePath(PROJECT_NAME);
        initialize(server, projectPath);

        Path filePath = getTestResourcePath(PROJECT_NAME + File.separator + TEST_FILE);
        String content = new String(Files.readAllBytes(filePath), UTF_8);
        openFiles(server, filePath, content);
        hightLight(server, filePath, content);
        //completion(server, filePath);

//        format(server);

        server.shutdown();
        server.exit();
        //todo kill process
    }

    private static Path getTestResourcePath(String resource) throws URISyntaxException {
        try {
            ClassLoader classloader = Thread.currentThread().getContextClassLoader();
            URL fileUrl = classloader.getResource(resource);
            if (fileUrl == null) {
                throw new IllegalStateException("Failed to load resource file!!!!");
            }
            return Paths.get(fileUrl.toURI());
        } catch (URISyntaxException e) {
            System.err.println("Invalid Url ! ");
            throw e;
        }
    }

    private static InitializeParams prepareInitializeParams(String projectPath) {
        InitializeParams initializeParams = new InitializeParams();
        initializeParams.setProcessId(getProcessId());
        initializeParams.setRootPath(projectPath);
        initializeParams.setCapabilities(new ClientCapabilities());
        initializeParams.setClientName("EclipseCHE");
        return initializeParams;
    }

    private static int getProcessId() {
        String name = ManagementFactory.getRuntimeMXBean().getName();
        int prefixEnd = name.indexOf('@');
        if (prefixEnd != -1) {
            String prefix = name.substring(0, prefixEnd);
            try {
                return Integer.parseInt(prefix);
            } catch (NumberFormatException ignored) {
            }
        }

        System.out.println("Failed to recognize the pid of the process");
        return -1;
    }

    private static void initialize(LanguageServer server, Path projectPath) throws Exception {
        InitializeParams initializeParams = prepareInitializeParams(projectPath.toAbsolutePath().toString());
        CompletableFuture<InitializeResult> completableFuture = server.initialize(initializeParams);
        try {
            InitializeResult initializeResult = completableFuture.get();
            System.out.println(initializeResult);
            System.out.println();
        } catch (InterruptedException | ExecutionException e) {
            server.shutdown();
            server.exit();

            throw new Exception("Error fetching server capabilities " + LANGUAGE_ID + ". " + e.getMessage(), e);
        }

    }

    private static void openFiles(LanguageServer server, Path filePath, String content) throws Exception {
        DidOpenTextDocumentParams didOpenTextDocumentParams2 = new DidOpenTextDocumentParams();
        didOpenTextDocumentParams2.setTextDocument(new TextDocumentItem());
        System.out.println("***********************************************************");
        System.out.println("Try to open file by path : " + filePath.toUri().toString());
        System.out.println("***********************************************************");
        didOpenTextDocumentParams2.getTextDocument().setUri(filePath.toUri().toString());
        didOpenTextDocumentParams2.getTextDocument().setLanguageId(LANGUAGE_ID);
        didOpenTextDocumentParams2.getTextDocument().setVersion(1);

        didOpenTextDocumentParams2.getTextDocument().setText(content);

//        System.out.println(content);

        server.getTextDocumentService().didOpen(didOpenTextDocumentParams2);
    }

    private static void completion(LanguageServer server, Path filePath) throws Exception {
      TextDocumentPositionParams textDocumentPositionParams = new TextDocumentPositionParams();
      TextDocumentIdentifier textDocument = new TextDocumentIdentifier();
      textDocument.setUri(filePath.toUri().toString());
      textDocumentPositionParams.setTextDocument(textDocument);
      textDocumentPositionParams.setPosition(new Position(0, 13));
      textDocumentPositionParams.setUri(filePath.toUri().toString());

      CompletionList list = server.getTextDocumentService()
                                  .completion(textDocumentPositionParams)
                                  .get();

      list.getItems().forEach(elem -> System.out.println(elem.getLabel()));
    //            .getItems()
    //            .forEach(completionItem -> {
    //                System.out.println(completionItem.toString());
    //            }
    //            );
   }

   private static void hightLight(LanguageServer server, Path filePath, String content) throws ExecutionException, InterruptedException {
        TextDocumentPositionParams positionParams = new TextDocumentPositionParams();
        positionParams.setUri(filePath.toUri().toString());
        Position position = new Position();
        position.setLine(0);
        position.setCharacter(content.length() - 1);
        positionParams.setPosition(position);

        TextDocumentIdentifier textDocumentIdentifier = new TextDocumentIdentifier();
        positionParams.setTextDocument(textDocumentIdentifier);

        positionParams.getTextDocument().setUri(filePath.toUri().toString());

        try {
            System.out.println("*******************");
           List<? extends DocumentHighlight> result = server.getTextDocumentService().documentHighlight(positionParams).get();
           System.out.println(result);
           System.out.println("^^^^^^^^^^^^^^^^^^");
        } catch (InterruptedException | ExecutionException e1) {
           System.out.println("Failed to complete hightLight task");
           throw e1;
        }
   }

//    private static void format(LanguageServer server) throws Exception {
//        DocumentRangeFormattingParams documentRangeFormattingParams = new DocumentRangeFormattingParams();
//        documentRangeFormattingParams.setTextDocument(new TextDocumentIdentifier());
//        documentRangeFormattingParams.getTextDocument().setUri("file:///home/antey/projects/cSimple/file.c");
//        documentRangeFormattingParams.setRange(new Range());
//        documentRangeFormattingParams.getRange().setStart(new Position(1, 4));
//        documentRangeFormattingParams.getRange().setEnd(new Position(1, 12));
//
//        FormattingOptions formattingOptions = new FormattingOptions();
//        formattingOptions.setTabSize(4);
//        formattingOptions.setInsertSpaces(true);
//        documentRangeFormattingParams.setOptions(new FormattingOptions(4,true, emptyMap()));
//
//        server.getTextDocumentService()
//              .rangeFormatting(documentRangeFormattingParams)
//              .get()
////              .get(15, TimeUnit.SECONDS)
//              .forEach(text -> System.out.println(" New Text " + text.getNewText()));
//    }
}
