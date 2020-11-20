package com.agoda.strategy;

import java.io.IOException;
import java.nio.file.Path;

public interface ArchiveStrategy {

    void compress(Path source, Path destination, long maxFileSize) throws IOException;

    void decompress(Path source, Path destination) throws IOException;
}
