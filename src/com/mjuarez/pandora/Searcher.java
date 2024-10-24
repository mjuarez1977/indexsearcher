package com.mjuarez.pandora;

import java.util.List;

public interface Searcher {
    List<String> search(String query);
    void onShutdown();
}
