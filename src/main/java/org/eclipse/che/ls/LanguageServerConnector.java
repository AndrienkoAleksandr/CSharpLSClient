package org.eclipse.che.ls;

import org.apache.log4j.Logger;
import org.eclipse.lsp4j.*;
import org.eclipse.lsp4j.jsonrpc.Launcher;
import org.eclipse.lsp4j.jsonrpc.messages.Either;
import org.eclipse.lsp4j.services.LanguageServer;

import java.io.IOException;
import java.lang.management.ManagementFactory;
import java.net.URI;
import java.nio.file.*;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

import static java.util.Collections.emptyList;
import static java.util.Collections.emptyMap;
import static java.util.Collections.singletonList;

public class LanguageServerConnector {

    private final static Logger logger = Logger.getLogger(LanguageServerConnector.class);

    private static final String CLIENT_NAME = "EclipseCheTest";
    private static final String LANGUAGE_ID = "csharp";
    private static final int    DEFAULT_TIME_OUT = 15;

    private LanguageServer server;

    public LanguageServerConnector(String serverExecPath, DiagnosticsMessagesCollector collector) throws IOException {
        logger.info("Try to start Language server process by node server path: " + serverExecPath);

        ProcessBuilder processBuilder = new ProcessBuilder("node", serverExecPath);
        Process lspProcess = processBuilder.start();

        logger.info("Try to connect to language server ........");

        Launcher<LanguageServer> launcher = Launcher.createLauncher(new LanguageClientImpl(collector),
                LanguageServer.class,
                lspProcess.getInputStream(),
                lspProcess.getOutputStream());
        launcher.startListening();

        this.server = launcher.getRemoteProxy();
    }

    private InitializeParams prepareInitializeParams(String projectPath) throws Exception {
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

    private int getProcessId() {
        String name = ManagementFactory.getRuntimeMXBean().getName();
        int prefixEnd = name.indexOf('@');
        if (prefixEnd != -1) {
            String prefix = name.substring(0, prefixEnd);
            try {
                return Integer.parseInt(prefix);
            } catch (NumberFormatException ignored) {
            }
        }

        logger.error("Failed to recognize the pid of the process");
        return -1;
    }

    public InitializeResult initialize(Path projectPath) throws Exception {
        InitializeParams initializeParams = prepareInitializeParams(projectPath.toAbsolutePath().toString());
        CompletableFuture<InitializeResult> completableFuture = server.initialize(initializeParams);
        try {
            return completableFuture.get();
        } catch (InterruptedException | ExecutionException e) {
            server.shutdown();
            server.exit();

            throw new Exception("Error fetching server capabilities " + LANGUAGE_ID + ". " + e.getMessage(), e);
        }

    }

    public void openFile(Path filePath, String content) throws Exception {
        DidOpenTextDocumentParams didOpenTextDocumentParams2 = new DidOpenTextDocumentParams();
        didOpenTextDocumentParams2.setTextDocument(new TextDocumentItem());
        didOpenTextDocumentParams2.getTextDocument().setUri(filePath.toUri().toString());
        didOpenTextDocumentParams2.getTextDocument().setLanguageId(LANGUAGE_ID);
        didOpenTextDocumentParams2.getTextDocument().setVersion(1);

        didOpenTextDocumentParams2.getTextDocument().setText(content);

        server.getTextDocumentService().didOpen(didOpenTextDocumentParams2);
    }

    public List<CompletionItem> completion(Path filePath, Position position) {
        TextDocumentPositionParams textDocumentPositionParams = new TextDocumentPositionParams();
        TextDocumentIdentifier textDocument = new TextDocumentIdentifier();
        textDocument.setUri(filePath.toUri().toString());
        textDocumentPositionParams.setTextDocument(textDocument);
        textDocumentPositionParams.setPosition(position);
        textDocumentPositionParams.setUri(filePath.toUri().toString());
        try {
            Either<List<CompletionItem>, CompletionList> result = server.getTextDocumentService()
                                                                        .completion(textDocumentPositionParams)
                                                                        .get();
            if (result.getLeft() != null) {
                return result.getLeft();
            } else {
                return result.getRight().getItems();
            }

        } catch (InterruptedException | ExecutionException e) {
            logger.error("Failed to get list completions", e);
        }

        return emptyList();
    }

    public List<? extends DocumentHighlight> hightLight(Path filePath, String content) {
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
            return server.getTextDocumentService()
                         .documentHighlight(positionParams)
                         .get(DEFAULT_TIME_OUT, TimeUnit.SECONDS);
        } catch (InterruptedException | ExecutionException | TimeoutException e) {
            logger.error("Failed to get hightLight tasks", e);
        }
        return emptyList();
    }

    public Hover hover(Path filePath, Position position) {
        TextDocumentPositionParams textDocumentPositionParams = new TextDocumentPositionParams();
        textDocumentPositionParams.setUri(filePath.toUri().toString());
        textDocumentPositionParams.setPosition(position);
        TextDocumentIdentifier textDocumentIdentifier = new TextDocumentIdentifier();
        textDocumentIdentifier.setUri(filePath.toUri().toString());
        textDocumentPositionParams.setTextDocument(textDocumentIdentifier);

        try {
            return server.getTextDocumentService().hover(textDocumentPositionParams).get(DEFAULT_TIME_OUT, TimeUnit.SECONDS);
        } catch (InterruptedException | ExecutionException | TimeoutException e) {
            logger.error("Failed to get hover", e);
        }
        return null;
    }

    public void didChange(Path filePath, String changeContent, Range range) {
        DidChangeTextDocumentParams textDocumentParams = new DidChangeTextDocumentParams();
        VersionedTextDocumentIdentifier versionedTextDocumentIdentifier = new VersionedTextDocumentIdentifier();
        versionedTextDocumentIdentifier.setVersion(3);
        versionedTextDocumentIdentifier.setUri(filePath.toUri().toString());
        textDocumentParams.setTextDocument(versionedTextDocumentIdentifier);
        textDocumentParams.setUri(filePath.toUri().toString());

        TextDocumentContentChangeEvent contentChangeEvent = new TextDocumentContentChangeEvent();
        contentChangeEvent.setText(changeContent);
        contentChangeEvent.setRange(range);
        textDocumentParams.setContentChanges(singletonList(contentChangeEvent));

        server.getTextDocumentService().didChange(textDocumentParams);
    }

    public void saveDocument(Path filePath) {
        TextDocumentIdentifier textDocumentIdentifier = new TextDocumentIdentifier();
        textDocumentIdentifier.setUri(filePath.toUri().toString());

        DidSaveTextDocumentParams didSaveTextDocumentParams = new DidSaveTextDocumentParams();
        didSaveTextDocumentParams.setTextDocument(textDocumentIdentifier);

        server.getTextDocumentService().didSave(didSaveTextDocumentParams);
    }

    /** Lsp4j contains bug and formatter doesn't work **/
    public List<? extends TextEdit> format(String fileName, Position startPosition, Position endPosition) {
        DocumentRangeFormattingParams documentRangeFormattingParams = new DocumentRangeFormattingParams();
        documentRangeFormattingParams.setTextDocument(new TextDocumentIdentifier());
        documentRangeFormattingParams.getTextDocument().setUri(fileName);
        documentRangeFormattingParams.setRange(new Range());
        documentRangeFormattingParams.getRange().setStart(startPosition);
        documentRangeFormattingParams.getRange().setEnd(endPosition);

        FormattingOptions formattingOptions = new FormattingOptions();
        formattingOptions.setTabSize(4);
        formattingOptions.setInsertSpaces(true);
        documentRangeFormattingParams.setOptions(new FormattingOptions(4,true, emptyMap()));

        try {
            return server.getTextDocumentService()
                         .rangeFormatting(documentRangeFormattingParams)
                         .get(DEFAULT_TIME_OUT, TimeUnit.SECONDS);
        } catch (InterruptedException | ExecutionException | TimeoutException e) {
            logger.error(String.format("Failed to format changes for file: ", fileName), e);
        }

        return emptyList();
    }
}
