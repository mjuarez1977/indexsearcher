package com.mjuarez.pandora;

import com.mjuarez.pandora.utils.Utils;

import java.io.*;
import java.util.*;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

import static com.mjuarez.pandora.utils.Utils.log;

public class InMemorySearcher implements Searcher {
    private static String filePath;
    private final static char delimiter = '\t';
    private final static String idDelimiter = "-";

    private HashMap<String, String> index = new LinkedHashMap<String, String>(100_000);

    public InMemorySearcher(String filePath) {
        this.filePath = filePath;
        initializeIndex();
    }

    public List<String> search(String query) {
        if (index.containsKey(query)) {
            return Arrays.asList(index.get(query).split(idDelimiter));
        }
        return Collections.emptyList();
    }

    @Override
    public void onShutdown() {
        log("Shutting down InMemorySearcher...");
        index.clear();
    }

    public static void main(String[] args) {
        if (args == null || args.length != 1) {
            System.err.println("Invalid input arguments: " + (args == null ? args : Arrays.asList(args)));
            System.exit(1);
        }

        final InMemorySearcher searcher = new InMemorySearcher(args[0]);
        final ExecutorService taskExecutor = Executors.newSingleThreadExecutor();
        taskExecutor.execute(new SearchConsole(searcher));

        setupShutdownHooks(searcher, taskExecutor);
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
                if (index.containsKey(key)) {
                    value = index.get(key) + idDelimiter + fields.get(1);
                }
                index.put(key, value);
            }
        } catch (IOException e) {
            System.out.println("Exception: " + e.getLocalizedMessage());
        }
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
}
