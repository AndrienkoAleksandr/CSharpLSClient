package org.eclipse.che.ls;

import org.apache.log4j.Logger;
import org.eclipse.lsp4j.MessageActionItem;
import org.eclipse.lsp4j.MessageParams;
import org.eclipse.lsp4j.PublishDiagnosticsParams;
import org.eclipse.lsp4j.ShowMessageRequestParams;
import org.eclipse.lsp4j.services.LanguageClient;

import java.util.concurrent.CompletableFuture;

public class LanguageClientImpl implements LanguageClient {

    private final static Logger logger = Logger.getLogger(LanguageClientImpl.class);

    private DiagnosticsMessagesCollector collector;

    public LanguageClientImpl(DiagnosticsMessagesCollector collector) {
        this.collector = collector;
    }

    @Override
    public void telemetryEvent(Object o) {
        logger.info(o);
    }

    @Override
    public void publishDiagnostics(PublishDiagnosticsParams publishDiagnosticsParams) {
        collector.add(publishDiagnosticsParams.getUri(), publishDiagnosticsParams.getDiagnostics());
        logger.info(publishDiagnosticsParams);
    }

    @Override
    public void showMessage(MessageParams messageParams) {
        logger.info(messageParams);
    }

    @Override
    public CompletableFuture<MessageActionItem> showMessageRequest(ShowMessageRequestParams showMessageRequestParams) {
        logger.info("Message request: " + showMessageRequestParams);
        return null;
    }

    @Override
    public void logMessage(MessageParams messageParams) {
        int messageType = messageParams.getType().getValue();
        switch (messageType) {
            // error
            case 1:
                logger.error(messageParams);
                break;
            // warning
            case 2:
                logger.warn(messageParams);
                break;
            // info
            case 3:
                // Fall, fall thought;
            // log
            case 4:
                logger.info(messageParams);
                break;
        }
    }
}
