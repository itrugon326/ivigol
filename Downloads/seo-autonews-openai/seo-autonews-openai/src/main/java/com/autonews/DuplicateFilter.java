package com.autonews;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;

import java.io.File;
import java.io.IOException;
import java.util.HashSet;
import java.util.Set;

public class DuplicateFilter {

    private final File file;
    private final Set<String> processed;
    private final ObjectMapper mapper = new ObjectMapper();

    public DuplicateFilter(String filePath) {
        this.file = new File(filePath);
        this.processed = loadFromDisk();
    }

    public boolean isProcessed(String url) {
        return processed.contains(url);
    }

    public void markProcessed(String url) {
        processed.add(url);
        saveToDisk();
    }

    private Set<String> loadFromDisk() {
        if (!file.exists()) return new HashSet<>();
        try {
            return mapper.readValue(file, new TypeReference<HashSet<String>>() {});
        } catch (IOException e) {
            return new HashSet<>();
        }
    }

    private void saveToDisk() {
        try {
            mapper.writeValue(file, processed);
        } catch (IOException e) {
            System.err.println("[DuplicateFilter] Error guardando: " + e.getMessage());
        }
    }
}
