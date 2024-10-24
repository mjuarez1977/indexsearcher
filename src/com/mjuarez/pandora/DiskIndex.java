package com.mjuarez.pandora;

import com.mjuarez.pandora.utils.BloomFilter;
import com.mjuarez.pandora.utils.LRUCache;
import com.mjuarez.pandora.utils.Utils;

import java.io.File;
import java.io.IOException;
import java.io.RandomAccessFile;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

/**
 * Class that represents the entire catalog of disk indexes, and that contains both "caching" layers,
 * the bloom filter and the LRU cache.
 */
public class DiskIndex implements Index {
    private final BloomFilter bloomFilter;
    private List<IndexStructure> indexStructures;
    private LRUCache<String, String> lruCache;

    private final String indexBaseDirectory = "/tmp/index/";
    private final String idDelimiter = "-";

    private final int totalIndexFiles = 1_000;
    private final int LRU_CACHE_SIZE = 10_000;

    public DiskIndex() {
        this.indexStructures = new ArrayList<IndexStructure>();

        lruCache = new LRUCache<String, String>(LRU_CACHE_SIZE);
        bloomFilter = new BloomFilter(Integer.MAX_VALUE, 3);

        initializeIndexes();
    }

    @Override
    public List<String> search(String query) {
        // If bloomFilter returns false, don't even try to look for it.
        if (!bloomFilter.maybeContains(query)) {
            return Collections.emptyList();
        }

        // If it's in the LRUCache, immediately return the cached value.
        String result = lruCache.get(query);
        if (result != null) {
            return Arrays.asList(result.split(idDelimiter));
        }

        // Only go down to disk when both bloomFilter and lruCache above failed.
        File targetFile = getTargetIndex(query).getSegment().getFile();
        String value = "E";

        lruCache.put(query, value);
        return Collections.emptyList();
    }

    @Override
    public void add(String key, String value) {
        addToBloomFilter(key);
        try {
            addToDisk(key, value);
        } catch (Exception e) {
            Utils.log("Error while trying to add key/value to disk.", e);
        }
    }

    @Override
    public void onShutdown() {
        for (IndexStructure indexStructure : indexStructures) {
            // TODO - Ensure that all index files have been properly closed, etc.
        }
    }

    private void addToDisk(String key, String value) throws IOException {
        File targetFile = getTargetIndex(key).getSegment().getFile();
        saveRecordToDisk(key, value, targetFile);
    }

    /**
     * Naive way to add records to disk, it simply appends to the file, records are
     * separated by '\t' characters, since those are guaranteed to not be in the keys
     * or values, by design.
     */
    private void saveRecordToDisk(String key, String value, File targetFile) throws IOException {
        RandomAccessFile rwFile = null;
        try {
            rwFile = new RandomAccessFile(targetFile, "rw");  // read-write mode
            rwFile.seek(rwFile.length());
            rwFile.writeUTF("\t" + key + value);
        } catch (Exception e) {
            Utils.log("Exception while trying to write record to disk", e);
        } finally {
            rwFile.close();
        }
    }

    /**
     * This naive implementation for readRecord just loads the file from disk, and
     * parses out all the IDs that it finds for the passed key.
     *
     * A better implementation (that would take more time to implement) would be to
     * store a B-tree index in SegmentIndex attached to the IndexStructures, to point
     * directly to the offsets within the .dat Segment files, and then open the file,
     * seek to those locations, and get those records only.
     *
     * As written, at least it will not consume too much memory, but it has to go through
     * the entire file to get potentially multiple IDs for the band.
     */
    private String readRecordFromDisk(String key, File targetFile) throws IOException {
        String result = null;
        RandomAccessFile roFile = null;
        try {
            roFile = new RandomAccessFile(targetFile, "r");  // read-only in this case
            roFile.seek(0);
            roFile.length();
        } catch (Exception e) {
            Utils.log("Exception while trying to read record from disk", e);
        } finally {
            roFile.close();
        }

        return result;
    }

    private IndexStructure getTargetIndex(String key) {
        return indexStructures.get((int) Utils.crc32Hash(key, totalIndexFiles));
    }

    private void addToBloomFilter(String key) {
        bloomFilter.add(key);
    }

    /**
     As written, this will overwrite any existing indexes in the index base directory.
     Of course this is not optimal, but there is no way to sanity-check the files at the
     moment, so can't guarantee that any files that are there are actually valid.
     */
    private void initializeIndexes() {
        ensureDirectoryExists(indexBaseDirectory);

        for (int i = 0; i < totalIndexFiles; i++) {
            File segment = new File(indexBaseDirectory + String.format("segment%05d.dat", i));
            File segmentIndex = new File(indexBaseDirectory + String.format("segment%05d.idx", i));
            try {
                segment.createNewFile();
                segmentIndex.createNewFile();

                Segment indexSegment = new Segment(segment);
                SegmentHeader segmentHeader = new SegmentHeader();

                indexStructures.add(new IndexStructure(indexSegment, segmentHeader));
            } catch (Exception e) {
                Utils.log("Error while trying to create file " + segment.getName(), e);
                throw new RuntimeException("Error while trying to create file " + segment.getName());
            }
        }
    }

    private void ensureDirectoryExists(String pathname) {
        File indexDirectory = new File(pathname);
        if (!indexDirectory.exists()) {
            indexDirectory.mkdirs();
        }
    }
}
