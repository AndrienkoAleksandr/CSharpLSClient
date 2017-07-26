package org.eclipse.che;

import org.eclipse.lsp4j.ClientCapabilities;
import org.eclipse.lsp4j.CodeActionCapabilities;
import org.eclipse.lsp4j.CodeLensCapabilities;
import org.eclipse.lsp4j.CompletionCapabilities;
import org.eclipse.lsp4j.CompletionItem;
import org.eclipse.lsp4j.CompletionItemCapabilities;
import org.eclipse.lsp4j.CompletionList;
import org.eclipse.lsp4j.DefinitionCapabilities;
import org.eclipse.lsp4j.DidChangeConfigurationCapabilities;
import org.eclipse.lsp4j.DidChangeWatchedFilesCapabilities;
import org.eclipse.lsp4j.DidOpenTextDocumentParams;
import org.eclipse.lsp4j.DocumentHighlight;
import org.eclipse.lsp4j.DocumentHighlightCapabilities;
import org.eclipse.lsp4j.DocumentLinkCapabilities;
import org.eclipse.lsp4j.DocumentSymbolCapabilities;
import org.eclipse.lsp4j.ExecuteCommandCapabilities;
import org.eclipse.lsp4j.FormattingCapabilities;
import org.eclipse.lsp4j.HoverCapabilities;
import org.eclipse.lsp4j.InitializeParams;
import org.eclipse.lsp4j.InitializeResult;
import org.eclipse.lsp4j.MessageActionItem;
import org.eclipse.lsp4j.MessageParams;
import org.eclipse.lsp4j.OnTypeFormattingCapabilities;
import org.eclipse.lsp4j.Position;
import org.eclipse.lsp4j.PublishDiagnosticsParams;
import org.eclipse.lsp4j.RangeFormattingCapabilities;
import org.eclipse.lsp4j.ReferencesCapabilities;
import org.eclipse.lsp4j.RenameCapabilities;
import org.eclipse.lsp4j.ShowMessageRequestParams;
import org.eclipse.lsp4j.SignatureHelpCapabilities;
import org.eclipse.lsp4j.SymbolCapabilities;
import org.eclipse.lsp4j.SynchronizationCapabilities;
import org.eclipse.lsp4j.TextDocumentClientCapabilities;
import org.eclipse.lsp4j.TextDocumentIdentifier;
import org.eclipse.lsp4j.TextDocumentItem;
import org.eclipse.lsp4j.TextDocumentPositionParams;
import org.eclipse.lsp4j.WorkspaceClientCapabilities;
import org.eclipse.lsp4j.WorkspaceEditCapabilities;
import org.eclipse.lsp4j.jsonrpc.Launcher;
import org.eclipse.lsp4j.jsonrpc.messages.Either;
import org.eclipse.lsp4j.services.LanguageClient;
import org.eclipse.lsp4j.services.LanguageServer;

import java.lang.management.ManagementFactory;
import java.net.URI;
import java.nio.file.*;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutionException;

import static java.nio.charset.StandardCharsets.UTF_8;

public class LanguageServerClient {

    private static final String LANGUAGE_ID = "csharp";
    private static final String TEST_FILE = "Program.cs";
    private static final String PROJECT_NAME = "aspnet2.0";

    private static final String CLIENT_NAME = "EclipseChe";

    private static LanguageClient languageClient = new LanguageClient() {

        @Override
        public void telemetryEvent(Object object) {
            System.out.println(object);
        }

        @Override
        public CompletableFuture<MessageActionItem> showMessageRequest(ShowMessageRequestParams requestParams) {
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
        String serverExec;
        if (args.length > 0) {
            serverExec = args[0];
        } else {
            throw new RuntimeException("You forgot set up node server path like argument command line!!!!!");
        }

        Path projectPath = TestProjectGenerator.generateTestProject(PROJECT_NAME);

        System.out.println("Try to start Language server process by node server path: " + serverExec);

        ProcessBuilder processBuilder = new ProcessBuilder("node", serverExec);

        Process lspProcess = processBuilder.start();

        System.out.println("Try to connect to language server ........");

        Launcher<LanguageServer> launcher = Launcher.createLauncher(languageClient,
                                                                    LanguageServer.class,
                                                                    lspProcess.getInputStream(),
                                                                    lspProcess.getOutputStream());
        launcher.startListening();
        LanguageServer server = launcher.getRemoteProxy();

        System.out.println("Begin initialization language server!!!");

        try {
            initialize(server, projectPath);

        Path filePath = projectPath.resolve(TEST_FILE);
        String content = new String(Files.readAllBytes(filePath), UTF_8);
        System.out.println("***********************************************************");
        System.out.println("Try to open file by path : " + filePath.toUri().toString());
        System.out.println("***********************************************************");

        openFile(server, filePath, content);

        hightLight(server, filePath, content);

        completion(server, filePath);

        // format doesn't work lsp4j bug
        // format(server);

        } catch (Exception e) {
            server.shutdown();
            server.exit(); //todo kill process
        }
    }

    private static InitializeParams prepareInitializeParams(String projectPath) throws Exception {
        InitializeParams initializeParams = new InitializeParams();
        initializeParams.setProcessId(getProcessId());
        initializeParams.setRootPath(projectPath);
        initializeParams.setRootUri(new URI(projectPath).toString());

        ClientCapabilities clientCapabilities = new ClientCapabilities();
        WorkspaceClientCapabilities workspace = new WorkspaceClientCapabilities();
        workspace.setApplyEdit(false); //Change when support added
        workspace.setDidChangeConfiguration(new DidChangeConfigurationCapabilities());
        workspace.setDidChangeWatchedFiles(new DidChangeWatchedFilesCapabilities());
        workspace.setExecuteCommand(new ExecuteCommandCapabilities());
        workspace.setSymbol(new SymbolCapabilities());
        workspace.setWorkspaceEdit(new WorkspaceEditCapabilities());
        clientCapabilities.setWorkspace(workspace);

        TextDocumentClientCapabilities textDocument = new TextDocumentClientCapabilities();
        textDocument.setCodeAction(new CodeActionCapabilities());
        textDocument.setCodeLens(new CodeLensCapabilities());
        textDocument.setCompletion(new CompletionCapabilities(new CompletionItemCapabilities()));
        textDocument.setDefinition(new DefinitionCapabilities());
        textDocument.setDocumentHighlight(new DocumentHighlightCapabilities());
        textDocument.setDocumentLink(new DocumentLinkCapabilities());
        textDocument.setDocumentSymbol(new DocumentSymbolCapabilities());
        textDocument.setFormatting(new FormattingCapabilities());
        textDocument.setHover(new HoverCapabilities());
        textDocument.setOnTypeFormatting(new OnTypeFormattingCapabilities());
        textDocument.setRangeFormatting(new RangeFormattingCapabilities());
        textDocument.setReferences(new ReferencesCapabilities());
        textDocument.setRename(new RenameCapabilities());
        textDocument.setSignatureHelp(new SignatureHelpCapabilities());
        textDocument.setSynchronization(new SynchronizationCapabilities(true, false, true));
        clientCapabilities.setTextDocument(textDocument);

        initializeParams.setCapabilities(clientCapabilities);
        initializeParams.setClientName(CLIENT_NAME);

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

    private static void openFile(LanguageServer server, Path filePath, String content) throws Exception {
        DidOpenTextDocumentParams didOpenTextDocumentParams2 = new DidOpenTextDocumentParams();
        didOpenTextDocumentParams2.setTextDocument(new TextDocumentItem());
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
        textDocumentPositionParams.setPosition(new Position(0, 6));
        textDocumentPositionParams.setUri(filePath.toUri().toString());

        System.out.println("*******************Try to get Completion*********************************************");
        Either<List<CompletionItem>, CompletionList> list = server.getTextDocumentService()
                .completion(textDocumentPositionParams)
                .get();

        System.out.println("_______________________________________________________" + list.getRight().getItems().size());
        list.getRight().getItems().forEach(elem -> System.out.println("******************************" + elem.getLabel()));
        System.out.println("_________________________________________________Completion_________________________________________________");

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
            System.out.println("*******************Try to get HightLight*********************************************");
            List<? extends DocumentHighlight> result = server.getTextDocumentService().documentHighlight(positionParams).get();
            System.out.println(result);
            System.out.println("___________________________________________________HightLight______________________________________________");
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
