package com.mjuarez.pandora;

import java.io.File;

/**
 * Segment holding the file reference for the disk index.
 */
public class Segment {
    private final File file;

    public Segment(File file) {
        this.file = file;
    }

    public File getFile() {
        return file;
    }

    @Override
    public String toString() {
        return "Segment{" +
                "file=" + file +
                '}';
    }
}
