package com.mjuarez.pandora;

import com.mjuarez.pandora.utils.Utils;

import java.io.*;
import java.util.Arrays;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

import static com.mjuarez.pandora.utils.Utils.log;

/**
 * Naive disk-based Searcher program
 * Spreads out the data over a "catalog" of data/index files, and also uses
 * a bloom filter with multiple hashes and an LRU cache to avoid having to go to disk as much
 * as possible, as well as taking advantage of repeat searches for very popular bands.
 *
 * The index file themselves are mostly PoC files, with very little structure to them,
 * and no index or direct way to seek a particular band/ID.  Of course, this would have
 * to be changed in a production system.
 */
public class DiskIndexSearcher implements Searcher {
    private final static char delimiter = '\t';
    private final static String idDelimiter = "-";

    private final String filePath;
    private final Index index;


    public DiskIndexSearcher(String filePath) {
        this.filePath = filePath;
        this.index = new DiskIndex();

        initializeIndex();
    }

    public List<String> search(String query) {
        return index.search(query);
    }

    @Override
    public void onShutdown() {
        log("Shutting down DiskIndexSearcher...");
        index.onShutdown();
    }

    private void initializeIndex() {
        try {
            InputStream in = new FileInputStream(new File(filePath));
            InputStreamReader reader = new InputStreamReader(in, "UTF-8");
            BufferedReader bufferedReader = new BufferedReader(reader);

            String inputLine;
            long lineNumber = 1;

            while ((inputLine = bufferedReader.readLine()) != null) {
                lineNumber++;
                List<String> fields = Utils.getTokens(inputLine, delimiter);
                if (fields.size() != 2) {
                    log("Invalid line [" + inputLine + "] in file " + filePath + " at line " + lineNumber + ". Skipping.");
                    continue;
                }

                String key = fields.get(0);
                String value = fields.get(1);

                addToDiskIndex(key, value);
            }
        } catch (Exception e) {
            log("Exception while loading file " + filePath, e);
        }
    }

    private void addToDiskIndex(String key, String value) {
        index.add(key, value);
    }

    private static void setupShutdownHooks(final Searcher searcher, final ExecutorService taskExecutor) {
        Runtime.getRuntime().addShutdownHook(new Thread() {
            @Override
            public void run() {
                try {
                    log("Shutting down executor with background tasks");
                    taskExecutor.awaitTermination(1, TimeUnit.SECONDS);
                } catch (InterruptedException e) {
                    log("Interrupted while awaiting termination", e);
                }
                searcher.onShutdown();
            }
        });
    }

    public static void main(String[] args) {
        if (args == null || args.length != 1) {
            System.err.println("Invalid input arguments: " + (args == null ? args : Arrays.asList(args)));
            System.exit(1);
        }

        final DiskIndexSearcher searcher = new DiskIndexSearcher(args[0]);
        final ExecutorService taskExecutor = Executors.newSingleThreadExecutor();
        taskExecutor.execute(new SearchConsole(searcher));

        setupShutdownHooks(searcher, taskExecutor);
    }
}