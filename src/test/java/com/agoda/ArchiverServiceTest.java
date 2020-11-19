package com.agoda;

import com.agoda.constants.CompressionType;
import com.agoda.constants.Constants;
import com.agoda.service.ArchiveService;
import io.micronaut.configuration.picocli.PicocliRunner;
import io.micronaut.context.ApplicationContext;
import io.micronaut.context.env.Environment;
import org.junit.jupiter.api.Test;

import java.io.*;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.Random;

import static org.junit.jupiter.api.Assertions.assertTrue;

public class ArchiverServiceTest {
    // TODO Cases
    // Archive by split
    // Archive by no split
    // Archive by mixed large|lesser|0 byte
    // Archive output directory has same file names (File names should have prefix (CopyOf)
    // Archive and unarchive compare crc
    // Archive and unarchive check subdirs
    // Decompression failure
    // Compression failure
    public static String TEMP_DIRECTORY_SRC = "archiver-test-src-";
    public static String TEMP_DIRECTORY_DST = "archiver-test-dst-";
    public static String TEMP_DIRECTORY_DECOMPRESSED = "archiver-test-decomp-";
    public static String DUMMY_FILE = "dummy.dat";
    @Test
    public void testCompressAndDecompress() throws Exception {

        Path tempSrcDir = Files.createTempDirectory(TEMP_DIRECTORY_SRC);
        Path tempCompressedDir = Files.createTempDirectory(TEMP_DIRECTORY_DST);
        Path tempDecompressedDir = Files.createTempDirectory(TEMP_DIRECTORY_DECOMPRESSED);
        createDummyFile(tempSrcDir, DUMMY_FILE, 100000);

        try (ApplicationContext ctx = ApplicationContext.run(Environment.CLI, Environment.TEST)) {
            ArchiveService archiveService = new ArchiveService();
            archiveService.setArchiveStrategy(CompressionType.ZIP);
            archiveService.compress(tempSrcDir, tempCompressedDir, 5);
            List<String> paths = getFilePaths(tempCompressedDir);
            assertTrue(paths.get(0).endsWith(Constants.ZIP_EXTENSION));
            archiveService.decompress(tempCompressedDir, tempDecompressedDir);
            List<String> decompressedPaths = getFilePaths(tempDecompressedDir);
            for (String path : decompressedPaths) {
                assertTrue(new File(path).exists());
            }

        }
    }

    private static List<String> getFilePaths(Path directory) throws IOException {
        List<String> pathList = new ArrayList<>();
        Files.walk(directory).forEach(f -> {
            try {
                if (Files.isRegularFile(f) && !Files.isHidden(f.toAbsolutePath())) {
                    pathList.add(f.toAbsolutePath().toString());
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
