package com.agoda.commands;

import com.agoda.constants.CompressionType;
import com.agoda.service.ArchiveService;
import io.micronaut.configuration.picocli.PicocliRunner;
import picocli.CommandLine.Command;
import picocli.CommandLine.Parameters;

import java.io.IOException;
import java.nio.file.Path;

@Command(name = "compress", description = "Compresses files given in the " +
        "source directory to the destination directory",
        mixinStandardHelpOptions = true)
public class CompressCommand implements Runnable {

    @Parameters(index = "0", description = "Source folder to look for files to compress")
    Path source;
    @Parameters(index = "1", description = "Destination folder to output the compressed files")
    Path destination;
    @Parameters(index = "2", description = "Max file size of the compressed file")
    long maxFileSize;
    @Parameters(index = "3", description = "Compression mode",
            defaultValue = CompressionType.ZIP)
    String mode;

    public static void main(String[] args) throws Exception {
        PicocliRunner.run(CompressCommand.class, args);
    }

    public void run() {

        ArchiveService archiveService = new ArchiveService();
        archiveService.setArchiveStrategy(mode);
        try {
            archiveService.compress(source, destination, maxFileSize);
        } catch (IOException e) {
            e.printStackTrace();
        }

    }
}
