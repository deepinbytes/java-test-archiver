package com.agoda.strategy.mode;

import com.agoda.strategy.ArchiveStrategy;

import java.io.IOException;
import java.nio.file.Path;

/**
 * Provides archiving operations for rar format
 */
public class RarStrategy implements ArchiveStrategy {
    @Override
    public void compress(Path source, Path destination, long maxFileSize) throws IOException {
        throw new UnsupportedOperationException("Compression type not supported yet.");
    }

    @Override
    public void decompress(Path source, Path destination) throws IOException {
        throw new UnsupportedOperationException("Decompression type not supported yet.");
    }
}
