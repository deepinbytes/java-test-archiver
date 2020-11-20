package com.agoda.strategy;

import java.io.IOException;
import java.nio.file.Path;

public class ArchiveStrategyContext {
    ArchiveStrategy archiveStrategy;

    public void setArchiveStrategy(ArchiveStrategy archiveStrategy) {
        this.archiveStrategy = archiveStrategy;
    }

    public void compress(Path source, Path destination, long maxFileSize) throws IOException {
        archiveStrategy.compress(source, destination, maxFileSize);
    }

    public void decompress(Path source, Path destination) throws IOException {
        archiveStrategy.decompress(source, destination);
    }
}
