package com.mjuarez.pandora;

import java.util.List;

public interface Index {
    List<String> search(String query);
    void add(String key, String value);
    void onShutdown();
}
