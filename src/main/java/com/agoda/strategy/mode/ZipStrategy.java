package com.agoda.strategy.mode;

import com.agoda.strategy.ArchiveStrategy;

import java.io.IOException;
import java.nio.file.Path;

public class ZipStrategy implements ArchiveStrategy {
    @Override
    public void compress(Path source, Path destination, long maxFileSize) throws IOException {

    }

    @Override
    public void decompress(Path source, Path destination) throws IOException {

    }
}
