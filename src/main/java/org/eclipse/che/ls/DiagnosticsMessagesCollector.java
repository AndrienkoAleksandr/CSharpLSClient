package org.eclipse.che.ls;

import org.eclipse.lsp4j.Diagnostic;
import org.eclipse.lsp4j.DiagnosticSeverity;

import java.util.Arrays;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.stream.IntStream;

import static java.util.stream.Collectors.toList;

public class DiagnosticsMessagesCollector {
    private final LinkedHashMap<String, List<Diagnostic>> diagnosticsQueue = new LinkedHashMap<>();
    private final List<Integer> defaultSeverityIndexFilter = IntStream.range(1, 4).boxed().collect(toList());

    public void add(String fileUrl, List<Diagnostic> diagnostics) {
        if (!diagnosticsQueue.containsValue(fileUrl)) {
            diagnosticsQueue.put(fileUrl, diagnostics);
        } else {
            diagnosticsQueue.get(fileUrl).addAll(diagnostics);
        }
    }

    public List<Diagnostic> getDiagnostics(String filePath, DiagnosticSeverity... diagnosticSeverity) {
        List<Integer> severities = getCodes(diagnosticSeverity);
        return diagnosticsQueue.get(filePath).stream()
                                             .filter(diagnostic -> diagnostic.getSeverity() != null && severities.contains(diagnostic.getSeverity().getValue()))
                                             .collect(toList());
    }

    private List<Integer> getCodes(DiagnosticSeverity... diagnosticSeverity) {
        List<Integer> severities = Arrays.stream(diagnosticSeverity)
                                         .map(DiagnosticSeverity::getValue)
                                         .collect(toList());
        if (severities.isEmpty()) {
            severities = defaultSeverityIndexFilter;
        }
        return severities;
    }

    public void clear() {
        diagnosticsQueue.clear();
    }
}
