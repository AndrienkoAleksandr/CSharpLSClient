package org.eclipse.che.utils;

import org.apache.log4j.Logger;
import org.eclipse.che.App;
import org.eclipse.che.ls.LanguageServerConnector;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.URL;
import java.nio.file.*;
import java.util.Collections;
import java.util.Comparator;
import java.util.stream.Stream;

public class ProjectGenerator {

    private final static Logger logger = Logger.getLogger(LanguageServerConnector.class);

    public static Path generateTestProject(String testPrjName) throws Exception {
        ClassLoader classLoader = ProjectGenerator.class.getClassLoader();
        InputStream inputStream = classLoader.getResourceAsStream(testPrjName);
        if (inputStream == null) {
            throw new IllegalStateException("Failed to load resource file by name " + testPrjName);
        }
        String appPath = App.class.getProtectionDomain().getCodeSource().getLocation().toURI().getPath();

        Path rootPath = Paths.get(appPath).getParent();
        Path testPrjPath = rootPath.resolve(testPrjName);

        deletePreviousPrj(testPrjPath);

        logger.info("Trying to find sources for test project ............................");
        Path sampleSrcPath = detectSampleSourcePath(testPrjName, rootPath);
        if (!Files.exists(sampleSrcPath)) {
            throw new FileNotFoundException("Sources for test project was not found!");
        }
        logger.info("Copy sources for sample project by path: " + testPrjPath);
        copyFilesRecursively(sampleSrcPath, testPrjPath);

        logger.info("Test project successfully created!!!");
        return testPrjPath;
    }

    private static void deletePreviousPrj(Path testProjectPath) throws IOException {
        logger.info("Deletion previous test project by path " + testProjectPath.toAbsolutePath() + "............................");
        if (Files.exists(testProjectPath)) {
            Files.walk(testProjectPath, FileVisitOption.FOLLOW_LINKS)
                    .sorted(Comparator.reverseOrder())
                    .map(Path::toFile)
                    .forEach(File::delete);
        }
    }

    private static Path detectSampleSourcePath(String testPrjName, Path appPath) throws URISyntaxException, IOException {
        URI resource;
        Path from;
        URL projectUrl = App.class.getClassLoader().getResource(testPrjName);

        if (projectUrl == null || projectUrl.toExternalForm().startsWith("jar")) {
            resource = App.class.getResource("").toURI();

            // create fs for jar
            FileSystem fileSystem = FileSystems.newFileSystem(resource, Collections.<String, String>emptyMap());
            from = fileSystem.getPath(testPrjName).toAbsolutePath();
        } else {
            from = Paths.get(projectUrl.toURI());
        }
        return from;
    }

    private static void copyFilesRecursively(Path from, Path target) throws IOException {
        try (final Stream<Path> sources = Files.walk(from)) {
            sources.forEach(src -> {
                // Get relative path to resolve (without zfs information in case if we copy from jar)
                Path srcToResolve = Paths.get(from.relativize(src).toString());
                final Path dest = target.resolve(srcToResolve);
                try {
                    if (Files.isDirectory(src)) {
                        if (Files.notExists(dest)) {
                            logger.info("Creating directory" +  dest);
                            Files.createDirectories(dest);
                        }
                    } else {
                        logger.info("Extracting file from:" + src + " to" + dest);
                        Files.copy(src, dest, StandardCopyOption.REPLACE_EXISTING);
                    }
                } catch (IOException e) {
                    throw new RuntimeException("Failed to copy file.", e);
                }
            });
        }
    }

    public static void main(String[] args) throws IOException {
        Path path1 = Paths.get("/home/antey/projects/CSharpLSClient/target/classes/aspnet2.0");
        Path path2 = Paths.get("/home/antey/projects/CSharpLSClient/target/classes/aspnet2.0/Program.cs");
        System.out.println(path1.relativize(path2));
        path1 = Paths.get("/aspnet2.0");
        path2 = Paths.get("/aspnet2.0/Program.cs");
        System.out.println(path1.relativize(path2));

//        deletePreviousPrj(Paths.get("/home/antey/projects/CSharpLSClient/target/aspnet2.0"));
//        copyFilesRecursivly(Paths.get("/home/antey/projects/CSharpLSClient/target/classes/aspnet2.0"),
//                            Paths.get("/home/antey/projects/CSharpLSClient/target/aspnet2.0"));

        deletePreviousPrj(Paths.get("/home/antey/projects/CSharpLSClient/target/aspnet2.0"));
        FileSystem fileSystem = FileSystems.newFileSystem(Paths.get("/home/antey/projects/CSharpLSClient/target/CSharpClient-1.0-SNAPSHOT.jar"), App.class.getClassLoader());
        Path from = fileSystem.getPath("aspnet2.0").toAbsolutePath();
        System.out.println(from);

        copyFilesRecursively(from,
                            Paths.get("/home/antey/projects/CSharpLSClient/target/aspnet2.0"));

    }
}
