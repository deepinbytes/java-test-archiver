package com.agoda.service;

import com.agoda.constants.CompressionType;
import com.agoda.strategy.ArchiveStrategyContext;
import com.agoda.strategy.mode.RarStrategy;
import com.agoda.strategy.mode.ZipStrategy;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.nio.file.NotDirectoryException;
import java.nio.file.Path;

import static com.agoda.utils.Validator.IsValidPath;


public class ArchiveService {
    public static final Logger logger
            = LoggerFactory.getLogger(ArchiveService.class);

    private ArchiveStrategyContext archiveStrategyContext;

    public void compress(Path source, Path destination, long maxFileSize) throws IOException {
        logger.info("Compressing files in directory `{}` to `{}`", source, destination);
        logger.info("Split size set to {}MB", maxFileSize);
        try {
            if (IsValidPath(source) && IsValidPath(destination)) {
                archiveStrategyContext.compress(source, destination, maxFileSize);
            } else {
                throw new NotDirectoryException("Invalid path supplied");
            }
        } catch (NotDirectoryException e) {
            logger.error("Invalid path supplied: `{}` | `{}`", source, destination);
            throw e;
        } catch (IOException e) {
            logger.error("Error compressing:{}", e.getMessage());
            throw e;
        }
        logger.info("Finished compressing files");
    }

    public void decompress(Path source, Path destination) throws IOException {
        logger.info("Decompressing files in directory `{}` to `{}`", source, destination);
        try {
            if (IsValidPath(source) && IsValidPath(destination)) {
                archiveStrategyContext.decompress(source, destination);
            } else {
                throw new NotDirectoryException("Invalid path supplied");
            }
        } catch (NotDirectoryException e) {
            logger.error("Invalid path supplied: `{}` | `{}`", source, destination);
            throw e;
        } catch (IOException e) {
            logger.error("Error compressing:{}", e.getMessage());
            throw e;
        }
        logger.info("Finished decompressing files");
    }


    public void setArchiveStrategy(String mode) throws UnsupportedOperationException {
        logger.info("Setting Archive Strategy:{}", mode);
        if (mode.equalsIgnoreCase(CompressionType.ZIP)) {
            archiveStrategyContext = new ArchiveStrategyContext();
            archiveStrategyContext.setArchiveStrategy(new ZipStrategy());
            return;
        } else if (mode.equalsIgnoreCase(CompressionType.RAR)) {
            archiveStrategyContext = new ArchiveStrategyContext();
            archiveStrategyContext.setArchiveStrategy(new RarStrategy());
            return;
        }
        logger.info("Unsupported Archive Strategy:`{}`", mode);
        throw new UnsupportedOperationException("Mode not found!");
    }
}
