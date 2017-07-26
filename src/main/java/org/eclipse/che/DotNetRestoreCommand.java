package org.eclipse.che;

import java.util.concurrent.TimeUnit;

/**
 * @author Alexander Andrienko
 */
public class DotNetRestoreCommand {

    private final String projectName;

    public DotNetRestoreCommand(String projectName) {
        this.projectName = projectName;
    }

    public void start() throws Exception {
        ProcessBuilder processBuilder = new ProcessBuilder();
        processBuilder.command("dotnet", "restore", projectName);
        Process process = processBuilder.start();
        boolean isSuccess = process.waitFor(10, TimeUnit.SECONDS);
        if (!isSuccess) {
            throw new RuntimeException("Failed to restore dotnet project " + projectName);
        }
    }
}
