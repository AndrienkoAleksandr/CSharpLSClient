
package org.eclipse.che.provider;

import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.nio.charset.StandardCharsets;

import static org.eclipse.lsp4j.jsonrpc.json.MessageConstants.CONTENT_LENGTH_HEADER;
import static org.eclipse.lsp4j.jsonrpc.json.MessageConstants.CRLF;

/**
 * Code was got from lsp4j lib
 */
public class LSPDataProvider {

    private final OutputStream outputStream;
    private final InputStream  errStream;

    public LSPDataProvider(InputStream inputStream, OutputStream outputStream, InputStream errStream) {
        this.outputStream = outputStream;
        this.errStream = errStream;

        Runnable runnable = () -> {
            try {
                int c;
                while ((c = inputStream.read()) != -1) {
                    char elem = (char)c;
                    System.out.print(elem);
                }
            } catch (Exception e) {
                System.out.println("ops");
            }

        };
        Thread thread = new Thread(runnable);
        thread.start();
    }

    public void consume(String content) {
        try {
            byte[] contentBytes = content.getBytes(StandardCharsets.UTF_8.name());
            int contentLength = contentBytes.length;

            String header = getHeader(contentLength);
            byte[] headerBytes = header.getBytes(StandardCharsets.US_ASCII);

            outputStream.write(headerBytes);
            outputStream.write(contentBytes);
            outputStream.flush();
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    protected String getHeader(int contentLength) {
        StringBuilder headerBuilder = new StringBuilder();
        appendHeader(headerBuilder, CONTENT_LENGTH_HEADER, contentLength).append(CRLF).append(CRLF);
        return headerBuilder.toString();
    }

    protected StringBuilder appendHeader(StringBuilder builder, String name, Object value) {
        return builder.append(name).append(": ").append(value);
    }

    public void printErrors() {
        try {
            Thread.sleep(100);
        } catch (InterruptedException e) {
            e.printStackTrace();
        }

        System.out.println("\n\n*****************************Error stream**************************************************************\n");

        if (errStream != null) {
            char[] buffer = new char[8192];
            try {
                InputStreamReader reader = new InputStreamReader(errStream);
                reader.read(buffer);
            } catch (Exception e) {
                System.err.println("ops");
            }
            System.out.println(new String(buffer));
        }
    }
}
