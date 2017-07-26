package org.eclipse.che;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.net.URI;
import java.net.URISyntaxException;
import java.nio.file.*;
import java.util.Collections;
import java.util.Comparator;
import java.util.stream.Stream;

public class TestProjectGenerator {

    public static Path generateTestProject(String testPrjName) throws Exception {
        System.out.println("Search test project template inside jar ............................");

        ClassLoader classLoader = TestProjectGenerator.class.getClassLoader();
        InputStream inputStream = classLoader.getResourceAsStream(testPrjName);
        if (inputStream == null) {
            throw new IllegalStateException("Failed to load resource file by name " + testPrjName);
        }
        String jarPath = LanguageServerClient.class.getProtectionDomain().getCodeSource().getLocation().toURI().getPath();

        Path rootPath = Paths.get(jarPath).getParent();
        Path testProjectPath = rootPath.resolve(testPrjName);

        System.out.println("Deletion previous test project by path " + testProjectPath.toAbsolutePath() + "............................");

        deletePreviousPrj(testProjectPath);

        System.out.println("Generation new one test project by path " + testProjectPath.toAbsolutePath() + "............................");

        copyFromJar(testPrjName, rootPath);

        System.out.println("Test project successfully generated!!!");

        return testProjectPath;
    }

    private static void deletePreviousPrj(Path testProjectPath) throws IOException {
        if (Files.exists(testProjectPath)) {
            Files.walk(testProjectPath, FileVisitOption.FOLLOW_LINKS)
                    .sorted(Comparator.reverseOrder())
                    .map(Path::toFile)
                    .peek(System.out::println)
                    .forEach(File::delete);
        }
    }

    private static void copyFromJar(String source, final Path target) throws URISyntaxException, IOException {
        URI resource = LanguageServerClient.class.getResource("").toURI();
        FileSystem fileSystem = FileSystems.newFileSystem(resource, Collections.<String, String>emptyMap());

        Path from = fileSystem.getPath(source);

        try (final Stream<Path> sources = Files.walk(from)) {
            sources.forEach(src -> {
                final Path dest = Paths.get(target.toAbsolutePath().toString() + src.toAbsolutePath());
                try {
                    if (Files.isDirectory(src)) {
                        if (Files.notExists(dest)) {
                            System.out.println("Creating directory" +  dest);
                            Files.createDirectories(dest);
                        }
                    } else {
                        System.out.println("Extracting file from jar:" + src + " to" + dest);
                        Files.copy(src, dest, StandardCopyOption.REPLACE_EXISTING);
                    }
                } catch (IOException e) {
                    throw new RuntimeException("Failed to unzip file.", e);
                }
            });
        }
    }
}
