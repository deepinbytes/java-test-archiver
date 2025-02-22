package com.agoda;

import com.agoda.commands.DecompressCommand;
import io.micronaut.configuration.picocli.PicocliRunner;
import io.micronaut.context.ApplicationContext;
import io.micronaut.context.env.Environment;

import java.io.ByteArrayOutputStream;
import java.io.PrintStream;
import java.nio.file.Files;
import java.nio.file.Path;

import org.junit.jupiter.api.Test;

import static com.agoda.utils.FileUtils.deleteFolder;
import static org.junit.jupiter.api.Assertions.assertTrue;

public class DecompressCommandTest {
    public static String TEMP_DIRECTORY_SRC = "archiver-cmd-test-src-";
    public static String TEMP_DIRECTORY_DST = "archiver-cmd-test-dst";
    @Test
    public void testWithCommandLineOption() throws Exception {
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        System.setOut(new PrintStream(baos));
        Path tempSrcDir = Files.createTempDirectory(TEMP_DIRECTORY_SRC);
        Path tempCompressedDir = Files.createTempDirectory(TEMP_DIRECTORY_DST);
        try (ApplicationContext ctx = ApplicationContext.run(Environment.CLI, Environment.TEST)) {
            String[] args = new String[] {String.valueOf(tempSrcDir), String.valueOf(tempCompressedDir)};
            PicocliRunner.run(DecompressCommand.class, ctx, args);
            assertTrue(baos.toString().contains("Decompressing files in directory"));
        }
        finally {
            deleteFolder(tempSrcDir);
            deleteFolder(tempCompressedDir);
        }
    }
}
