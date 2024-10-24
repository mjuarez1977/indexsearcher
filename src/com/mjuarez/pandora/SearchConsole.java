package com.mjuarez.pandora;

import java.util.List;
import java.util.Scanner;

public class SearchConsole implements Runnable {
    private Searcher searcher;

    public SearchConsole(Searcher searcher) {
        this.searcher = searcher;
    }

    @Override
    public void run() {
        prompt();

        Scanner scanner = new Scanner(System.in);

        while (scanner.hasNext()) {
            String queryString = scanner.nextLine();
            List<String> results = searcher.search(queryString);
            if (results.isEmpty()) {
                System.out.println("No results found!");
            } else {
                for (String result : results) {
                    System.out.println(result);
                }
            }
            prompt();
        }
    }

    private static void prompt() {
        System.out.print("search> ");
    }
}
