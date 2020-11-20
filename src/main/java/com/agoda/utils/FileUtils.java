package com.agoda.utils;


import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.file.FileVisitResult;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.SimpleFileVisitor;
import java.nio.file.attribute.BasicFileAttributes;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import static com.agoda.constants.Constants.FILE_PART_SUFFIX;
import static com.agoda.service.ArchiveService.logger;

/**
 * Provides utilities for file operations
 */
public class FileUtils {


    /**
     * Adds part suffix to the given filename
     * @param path path of the file
     * @param partNumber split part number
     * @return transformed path
     */
    public static Path resolvePartFilePath(Path path, String partNumber) {
        String fileName = path.getFileName().toString();
        if (fileName.contains(".")) {
            int dot = fileName.lastIndexOf(".");
            String baseName = fileName.substring(0, dot);
            String extension = fileName.substring(dot);
            return path.getParent().resolve(baseName + FILE_PART_SUFFIX + partNumber + extension);
        }
        return path.getParent().resolve(fileName + FILE_PART_SUFFIX + partNumber);
    }

    /**
     * Checks if its and splitted file and gives original filename+path
     * @param path filepath
     * @return the transformed path
     */
    public static Path findBaseNameFromPart(Path path) {
        String fileName = path.getFileName().toString();
        if (fileName.matches(".*part.[0-9]+.*")) {
            String baseName = fileName.substring(0, fileName.indexOf(FILE_PART_SUFFIX));
            String extension = "";
            if (fileName.contains(".")) {
                extension = fileName.substring(fileName.lastIndexOf("."));
            }
            return path.getParent().resolve(baseName + extension);
        }
        return path;
    }

    /**
     * Checks given path is valid or not
     * @param path path to check
     * @return true or false
     */
    public static boolean IsValidPath(Path path) {
        return Files.isDirectory(path);
    }

    /**
     * Deletes a given folder
     * @param dir path of the folder
     * @throws IOException if operation fails
     */
    public static void deleteFolder(Path dir) throws IOException {
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

    /**
     * Provides file and folder operations and makes sure each chunk output generated does not exceed given maxFilesize.
     */
    public static class FileVisitor extends SimpleFileVisitor<Path> {

        private final List<List<Path>> chunks = new ArrayList<>();

        private List<Path> currentChunk = new ArrayList<>();

        private long currentChunkSize = 0;
        private final long maxFileSize;

        private final Path destination;
        private final Path source;


        public FileVisitor(Path source, Path destination, long maxFileSize) {
            this.destination = destination;
            this.source = source;
            this.maxFileSize = maxFileSize;
        }

        @Override
        public FileVisitResult visitFile(Path file, BasicFileAttributes attrs) throws IOException {

            super.visitFile(file, attrs);
            long fileSize = Files.size(file);

            if (maxFileSize > 0 && fileSize + currentChunkSize > maxFileSize) {
                if (fileSize > maxFileSize) {
                    List<Path> splitParts = splitFile(file, fileSize, destination);
                    for (Path part : splitParts) {
                        visitFile(part, attrs);
                    }
                    return FileVisitResult.CONTINUE;
                }
                chunks.add(currentChunk);
                currentChunk = new ArrayList<>();
                currentChunkSize = 0;
            } else {
                logger.trace("File `{}` added on chunk `{}`", file, chunks.size());
            }
            addToChunk(file, fileSize);
            return FileVisitResult.CONTINUE;
        }

        private void addToChunk(Path file, long size) {
            currentChunk.add(file);
            currentChunkSize += size;
            logger.trace("File '{}' added on chunk `{}`", file, chunks.size());
        }

        @Override
        public FileVisitResult preVisitDirectory(Path dir, BasicFileAttributes attrs) throws IOException {
            if (dir.equals(source))
                return FileVisitResult.CONTINUE;

            super.preVisitDirectory(dir, attrs);
            addToChunk(dir, 0);
            return FileVisitResult.CONTINUE;
        }

        public List<List<Path>> getChunks() {
            if (currentChunk.size() > 0) {
                chunks.add(currentChunk);
                currentChunk = Collections.emptyList();
            }
            return chunks;
        }


        static void bufferedWrite(InputStream inputStream, OutputStream outputStream, long numBytes) throws IOException {
            byte[] buffer = new byte[(int) numBytes];
            int val = inputStream.read(buffer);
            if (val != -1) {
                outputStream.write(buffer);
            }
        }

        /**
         * Splits a file into multiple chunks such that it does not exceed maxFileSize
         * @param path path of the file
         * @param size maxFileSize
         * @param tempDir destination directory
         * @return list of paths of the chunks
         * @throws IOException if operation fails
         */
        private List<Path> splitFile(Path path, long size, Path tempDir) throws IOException {
            List<Path> fileParts = new ArrayList<>();
            long parts = size / maxFileSize;
            long sizePerPart = maxFileSize;
            long remainingSize = size % maxFileSize;
            int bufferSize = 1024;

            try (InputStream inputStream = Files.newInputStream(path)) {
                for (int i = 0; i < parts; i++) {
                    Path part = tempDir.resolve(source.relativize(
                            resolvePartFilePath(path, "" + i)).toString());
                    Files.createDirectories(part.getParent());
                    OutputStream outputStream = Files.newOutputStream(part);
                    if (sizePerPart > bufferSize) {
                        long readsRequired = sizePerPart / bufferSize;
                        long remainingBufferToRead = sizePerPart % bufferSize;
                        for (int j = 0; j < readsRequired; j++) {
                            bufferedWrite(inputStream, outputStream, bufferSize);
                        }
                        if (remainingBufferToRead > 0) {
                            bufferedWrite(inputStream, outputStream, remainingBufferToRead);
                        }
                    } else {
                        bufferedWrite(inputStream, outputStream, sizePerPart);
                    }
                    fileParts.add(part);
                    outputStream.close();
                }
                if (remainingSize > 0) {
                    Path part = tempDir.resolve(source.relativize(
                            resolvePartFilePath(path, "" + parts)).toString()
                    );
                    try (OutputStream bw = Files.newOutputStream(part)) {
                        bufferedWrite(inputStream, bw, remainingSize);
                        fileParts.add(part);
                    }
                }
            } catch (Exception e) {
                logger.error(e.getMessage());
                throw e;
            }
            return fileParts;
        }

    }
}
