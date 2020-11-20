package com.agoda.utils;


import java.io.*;
import java.nio.file.*;
import java.nio.file.attribute.BasicFileAttributes;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import static com.agoda.constants.Constants.FILE_PART_SUFFIX;
import static com.agoda.service.ArchiveService.logger;


public class FileUtils {


    public static Path resolvePartFilePath(Path path, String partNumber) {
        String fileName = path.getFileName().toString();
        if (fileName.contains(".")) {
            int dot = fileName.lastIndexOf(".");
            String baseName = fileName.substring(0, dot);
            String extension = fileName.substring(dot);
            return path.getParent().resolve(baseName + FILE_PART_SUFFIX+ partNumber + extension);
        }
        return path.getParent().resolve(fileName + FILE_PART_SUFFIX + partNumber);
    }

    public static Path findBaseNameFromPart(Path path) {
        String fileName = path.getFileName().toString();
        if (fileName.matches(".*part.[0-9]+.*")) {
            String baseName = fileName.substring(0, fileName.indexOf(FILE_PART_SUFFIX));
            String extension = fileName.substring(fileName.lastIndexOf("."));
            return path.getParent().resolve(baseName + extension);
        }
        return path;
    }

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
