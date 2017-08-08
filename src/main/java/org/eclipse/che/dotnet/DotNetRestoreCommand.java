package org.eclipse.che.dotnet;

import org.apache.log4j.Logger;
import org.eclipse.che.ls.LanguageServerConnector;

import java.util.concurrent.TimeUnit;

/**
 * @author Alexander Andrienko
 */
public class DotNetRestoreCommand {

    private final static Logger logger = Logger.getLogger(LanguageServerConnector.class);

    private final String projectName;

    public DotNetRestoreCommand(String projectName) {
        this.projectName = projectName;
    }

    public void start() throws Exception {
        logger.info("Restore project " + projectName);

        ProcessBuilder processBuilder = new ProcessBuilder();
        processBuilder.command("dotnet", "restore", projectName);
        Process process = processBuilder.start();
        boolean isSuccess = process.waitFor(10, TimeUnit.SECONDS);
        if (!isSuccess) {
            throw new RuntimeException("Failed to restore dotnet project " + projectName);
        }
    }
}
