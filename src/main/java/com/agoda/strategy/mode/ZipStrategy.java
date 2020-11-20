package com.agoda.strategy.mode;

import com.agoda.strategy.ArchiveStrategy;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.IntStream;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;
import java.util.zip.ZipOutputStream;

import static com.agoda.constants.Constants.*;
import static com.agoda.service.ArchiveService.logger;
import static com.agoda.utils.FileUtils.*;
import static com.agoda.utils.Utils.getMaxMemory;
import static java.nio.file.StandardOpenOption.APPEND;
import static java.nio.file.StandardOpenOption.CREATE;

public class ZipStrategy implements ArchiveStrategy {

    public void compress(Path source, Path destination, long maxFileSize) throws IOException {
        maxFileSize = Math.min(maxFileSize, getMaxMemory()) * 1024L * 1024L;
        Path outputZip = destination.resolve(source.getFileName() + ZIP_EXTENSION);

        Path tempDir = Files.createTempDirectory(TEMP_FOLDER_PREFIX).resolve(source.getFileName().toString());
        List<List<Path>> chunks = getChunks(source, maxFileSize, tempDir);
        if (chunks.size() == 1) {
            writeToZip(chunks.get(0), outputZip, source, tempDir);
        } else {
            IntStream.range(0, chunks.size())
                    .parallel()
                    .forEach(index -> {
                        try {
                            Path zipFile = resolvePartFilePath(outputZip, "" + index);
                            writeToZip(chunks.get(index), zipFile, source, tempDir);
                        } catch (IOException e) {
                            logger.error("Error zipping files:", e);
                        }
                    });
        }
        if (Files.notExists(tempDir))
            return;
        deleteFolder(tempDir);
        Files.delete(tempDir.getParent());
    }

    @Override
    public void decompress(Path source, Path destination) throws IOException {

        List<Path> compressedFiles = Files.list(source).sorted()
                .filter(path -> path.toString().endsWith(ZIP_EXTENSION))
                .collect(Collectors.toList());

        if (compressedFiles.size() <= 0) {
            logger.error("Input directory `{}` is empty", source);
            throw new IllegalArgumentException("Input directory '" + source + " is empty");
        }

        for (Path compressedFile : compressedFiles) {
            ZipInputStream zipInputStream = new ZipInputStream(Files.newInputStream(compressedFile));
            ZipEntry zipEntry = zipInputStream.getNextEntry();
            while (zipEntry != null) {
                Path outputFile = destination.resolve(zipEntry.getName());
                if (zipEntry.isDirectory()) {
                    Files.createDirectories(outputFile);
                } else {
                    byte[] buffer = new byte[1024];
                    outputFile = findBaseNameFromPart(outputFile);
                    OutputStream outputStream = Files.newOutputStream(outputFile, CREATE, APPEND);
                    int len;
                    while ((len = zipInputStream.read(buffer)) > 0) {
                        outputStream.write(buffer, 0, len);
                    }
                    outputStream.close();
                }
                zipEntry = zipInputStream.getNextEntry();
            }
            zipInputStream.closeEntry();
            zipInputStream.close();
        }
    }

    private static void writeToZip(List<Path> contents, Path zipFile, Path source, Path tempDir)
            throws IOException {
        try (ZipOutputStream zipOutputStream = new ZipOutputStream(Files.newOutputStream(zipFile))) {
            for (Path path : contents) {
                Path processedPath;
                if (path.getFileName().toString().contains(FILE_PART_SUFFIX))
                    processedPath = tempDir.relativize(path);
                else {
                    processedPath = source.relativize(path);
                }
                String transformedFile = processedPath.toString();
                if (Files.isRegularFile(path)) {
                    try (InputStream inputStream = Files.newInputStream(path)) {
                        byte[] buffer = new byte[1024];
                        ZipEntry zipEntry = new ZipEntry(transformedFile);
                        zipOutputStream.putNextEntry(zipEntry);
                        int bufferReadLength;
                        while ((bufferReadLength = inputStream.read(buffer)) >= 0) {
                            zipOutputStream.write(buffer, 0, bufferReadLength);
                        }
                        zipOutputStream.closeEntry();
                    }
                    logger.debug("Written file `{}` into archive", transformedFile);
                } else {
                    transformedFile += File.separator;
                    zipOutputStream.putNextEntry(new ZipEntry(transformedFile));
                    zipOutputStream.closeEntry();
                    logger.debug("Written directory `{}` into archive", transformedFile);
                }
            }
        }
    }

    private static List<List<Path>> getChunks(Path directory, long maxFileSize, Path destination) throws IOException {
        FileVisitor visitor = new FileVisitor(directory, destination, maxFileSize);
        Files.walkFileTree(directory, visitor);
        return visitor.getChunks();
    }

}
