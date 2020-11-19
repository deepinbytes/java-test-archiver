package com.agoda;

import com.agoda.constants.CompressionType;
import com.agoda.constants.Constants;
import com.agoda.service.ArchiveService;
import io.micronaut.configuration.picocli.PicocliRunner;
import io.micronaut.context.ApplicationContext;
import io.micronaut.context.env.Environment;
import org.junit.jupiter.api.Test;

import java.io.*;
import java.nio.file.*;
import java.nio.file.attribute.BasicFileAttributes;
import java.util.ArrayList;
import java.util.List;
import java.util.Random;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

public class ArchiverServiceTest {
    // TODO Cases
    // Archive by mixed large|lesser|0 byte
    // Archive output directory has same file names (File names should have prefix (CopyOf)
    // Decompression failure
    // Compression failure
    public static String TEMP_DIRECTORY_SRC = "archiver-test-src-";
    public static String TEMP_DIRECTORY_DST = "archiver-test-dst-";
    public static String TEMP_DIRECTORY_DECOMPRESSED = "archiver-test-decomp-";
    public static String DUMMY_FILE = "dummy.dat";
    @Test
    public void testCompressAndDecompressCheckIdentical() throws Exception {

        Path tempSrcDir = Files.createTempDirectory(TEMP_DIRECTORY_SRC);
        Path tempCompressedDir = Files.createTempDirectory(TEMP_DIRECTORY_DST);
        Path tempDecompressedDir = Files.createTempDirectory(TEMP_DIRECTORY_DECOMPRESSED);
        createDummyFile(tempSrcDir, DUMMY_FILE, 100000);

        try (ApplicationContext ctx = ApplicationContext.run(Environment.CLI, Environment.TEST)) {
            ArchiveService archiveService = new ArchiveService();
            archiveService.setArchiveStrategy(CompressionType.ZIP);
            archiveService.compress(tempSrcDir, tempCompressedDir, 100);
            List<Path> paths = getFilePaths(tempCompressedDir);
            assertTrue(String.valueOf(paths.get(0)).endsWith(Constants.ZIP_EXTENSION));

            archiveService.decompress(tempCompressedDir, tempDecompressedDir);
            List<Path> decompressedPaths = getFilePaths(tempDecompressedDir);
            for (Path path : decompressedPaths) {
                assertTrue(new File(String.valueOf(path)).exists());
                Path sourceFile = tempSrcDir.resolve(path.getFileName().toString());
                assertTrue(fileHasSameContent(sourceFile, path));
            }
        }
        finally {
            deleteFolder(tempSrcDir);
            deleteFolder(tempCompressedDir);
            deleteFolder(tempDecompressedDir);
        }
    }

    @Test
    public void testCompressAndDecompressFileSizeExceeds() throws Exception {

        Path tempSrcDir = Files.createTempDirectory(TEMP_DIRECTORY_SRC);
        Path tempCompressedDir = Files.createTempDirectory(TEMP_DIRECTORY_DST);
        Path tempDecompressedDir = Files.createTempDirectory(TEMP_DIRECTORY_DECOMPRESSED);
        createDummyFile(tempSrcDir, DUMMY_FILE, 100000);

        try (ApplicationContext ctx = ApplicationContext.run(Environment.CLI, Environment.TEST)) {
            ArchiveService archiveService = new ArchiveService();
            archiveService.setArchiveStrategy(CompressionType.ZIP);
            archiveService.compress(tempSrcDir, tempCompressedDir, 5);
            List<Path> paths = getFilePaths(tempCompressedDir);
            for (Path path : paths) {
                assertTrue(String.valueOf(path).endsWith(Constants.ZIP_EXTENSION));
                assertTrue( Files.size(path) < (5 * 1024L * 1024L));
            }
            archiveService.decompress(tempCompressedDir, tempDecompressedDir);
            List<Path> decompressedPaths = getFilePaths(tempDecompressedDir);
            for (Path path : decompressedPaths) {
                assertTrue(new File(String.valueOf(path)).exists());
            }

        }
        finally {
            deleteFolder(tempSrcDir);
            deleteFolder(tempCompressedDir);
            deleteFolder(tempDecompressedDir);
        }

    }

    @Test
    public void testUnsupportedMode() throws Exception {

        Path tempSrcDir = Files.createTempDirectory(TEMP_DIRECTORY_SRC);
        Path tempCompressedDir = Files.createTempDirectory(TEMP_DIRECTORY_DST);
        Path tempDecompressedDir = Files.createTempDirectory(TEMP_DIRECTORY_DECOMPRESSED);
        createDummyFile(tempSrcDir, DUMMY_FILE, 100000);

        try (ApplicationContext ctx = ApplicationContext.run(Environment.CLI, Environment.TEST)) {
            ArchiveService archiveService = new ArchiveService();
            archiveService.setArchiveStrategy("test");
            archiveService.compress(tempSrcDir, tempCompressedDir, 5);
        }

        catch (UnsupportedOperationException expected) {
            assertEquals("Mode not found!", expected.getMessage());
        }
        finally {
            deleteFolder(tempSrcDir);
            deleteFolder(tempCompressedDir);
            deleteFolder(tempDecompressedDir);
        }
    }

    @Test
    public void testNotImplementedMode() throws Exception {

        Path tempSrcDir = Files.createTempDirectory(TEMP_DIRECTORY_SRC);
        Path tempCompressedDir = Files.createTempDirectory(TEMP_DIRECTORY_DST);
        Path tempDecompressedDir = Files.createTempDirectory(TEMP_DIRECTORY_DECOMPRESSED);
        createDummyFile(tempSrcDir, DUMMY_FILE, 100000);

        try (ApplicationContext ctx = ApplicationContext.run(Environment.CLI, Environment.TEST)) {
            ArchiveService archiveService = new ArchiveService();
            archiveService.setArchiveStrategy(CompressionType.RAR);
            archiveService.compress(tempSrcDir, tempCompressedDir, 5);
        }

        catch (UnsupportedOperationException expected) {
            assertEquals("Compression type not supported yet.", expected.getMessage());
        }
        finally {
            deleteFolder(tempSrcDir);
            deleteFolder(tempCompressedDir);
            deleteFolder(tempDecompressedDir);
        }

    }

    @Test
    public void testInvalidDirectoryCompress() throws Exception {
        try (ApplicationContext ctx = ApplicationContext.run(Environment.CLI, Environment.TEST)) {
            ArchiveService archiveService = new ArchiveService();
            archiveService.setArchiveStrategy(CompressionType.RAR);
            Path path = Path.of("t3st");
            archiveService.compress(path,path, 5);
        }

        catch (NotDirectoryException expected) {
            assertTrue(expected.getMessage().contains("Invalid path supplied"));
        }
    }

    @Test
    public void testInvalidDirectoryDecompress() throws Exception {
        try (ApplicationContext ctx = ApplicationContext.run(Environment.CLI, Environment.TEST)) {
            ArchiveService archiveService = new ArchiveService();
            archiveService.setArchiveStrategy(CompressionType.RAR);
            Path path = Path.of("t3st");
            archiveService.decompress(path,path);
        }

        catch (NotDirectoryException expected) {
            assertTrue(expected.getMessage().contains("Invalid path supplied"));
        }
    }

    private static void deleteFolder(Path dir) throws IOException {
        Files.walkFileTree(dir, new SimpleFileVisitor<>() {
            @Override
            public FileVisitResult visitFile(Path file, BasicFileAttributes attrs) throws IOException {
                Files.delete(file);
                return FileVisitResult.CONTINUE;
            }

            @Override
            public FileVisitResult postVisitDirectory(Path dir, IOException exc) throws IOException {
                Files.delete(dir);
                return FileVisitResult.CONTINUE;
            }
        });
    }

    private static boolean fileHasSameContent(Path file1, Path file2) throws IOException {
        return Files.mismatch(file1, file2) == -1;
    }

    private static List<Path> getFilePaths(Path directory) throws IOException {
        List<Path> pathList = new ArrayList<>();
        Files.walk(directory).forEach(f -> {
            try {
                if (Files.isRegularFile(f) && !Files.isHidden(f.toAbsolutePath())) {
                    pathList.add(f.toAbsolutePath());
                }
            } catch (IOException ignored) {
            }
        });
        return pathList;
    }

    private File createDummyFile(Path directory, String filename, long size) throws IOException{
        File file = new File(String.valueOf(directory), filename);

        FileOutputStream fileOutputStream = new FileOutputStream(file);

        for(int i = 0; i < size; i++) {
            byte[] buffer = new byte[1024];
            Random random = new Random(System.currentTimeMillis());
            random.nextBytes(buffer);
            fileOutputStream.write(buffer);
        }

        fileOutputStream.close();
        return file;
    }
}
