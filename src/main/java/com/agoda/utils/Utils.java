package com.agoda.utils;

public class Utils {
    public static long getMaxMemory() {
        return Runtime.getRuntime().maxMemory() / (1024L * 1024L);
    }
}
