package com.agoda.commands;

import com.agoda.constants.CompressionType;
import com.agoda.service.ArchiveService;
import io.micronaut.configuration.picocli.PicocliRunner;
import picocli.CommandLine;
import picocli.CommandLine.Command;

import java.io.IOException;
import java.nio.file.Path;

@Command(name = "decompress", description = "Decompresses files given in the" +
        " source directory to the destination directory",
        mixinStandardHelpOptions = true)
public class DecompressCommand implements Runnable {


    @CommandLine.Parameters(index = "0", description = "Source folder to look for files to decompress")
    Path source;
    @CommandLine.Parameters(index = "1", description = "Destination folder to output the decompressed files")
    Path destination;
    @CommandLine.Parameters(index = "2", description = "Compression mode",
            defaultValue = CompressionType.ZIP)
    String mode;

    public static void main(String[] args) throws Exception {
        PicocliRunner.run(DecompressCommand.class, args);
    }

    public void run() {
        ArchiveService archiveService = new ArchiveService();
        archiveService.setArchiveStrategy(mode);
        try {
            archiveService.decompress(source, destination);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
}
