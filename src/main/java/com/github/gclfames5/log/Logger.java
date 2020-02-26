package com.github.gclfames5.log;

import java.io.*;

public class Logger {

    private static File file;
    private static FileWriter writer;

    public static void initLog(String path) {
        file = new File(path);
        try {
            writer = new FileWriter(file);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public static boolean isInitialized() {
        return file != null && writer != null;
    }

    public static void log(String s) {
        if (!isInitialized())
            throw new RuntimeException("Attempted to write to uninitialized log!");

        try {
            writer.append(s + "\n");
            writer.flush();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public static void log(String s, boolean sysOut) {
        log(s);

        if (sysOut)
            System.out.println(s);
    }

    public static void log(Exception e) {
        StringWriter sw = new StringWriter();
        PrintWriter pw = new PrintWriter(sw);
        e.printStackTrace(pw);
        e.printStackTrace();

        log(sw.toString());
    }

    public static void finishLog() {
        try {
            writer.close();
        } catch (IOException e) {
            e.printStackTrace();
        } finally {
            writer = null;
        }
    }
}
