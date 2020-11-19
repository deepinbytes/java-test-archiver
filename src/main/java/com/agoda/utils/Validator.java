package com.agoda.utils;

import java.nio.file.Files;
import java.nio.file.Path;;

public class Validator {
    public static boolean IsValidPath(Path path) {
        return Files.isDirectory(path);
    }
}
