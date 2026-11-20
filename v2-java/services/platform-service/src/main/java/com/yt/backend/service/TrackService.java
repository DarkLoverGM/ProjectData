package com.yt.backend.service;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.yt.backend.config.AppPaths;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/** Log truy cap ghi boi POST /api/track, luu o server/data/log.json (dung chung file voi ban Node). */
@Service
public class TrackService {

    private static final Logger log = LoggerFactory.getLogger(TrackService.class);

    private static final int LOG_MAX = 5000;

    private final Path logFile;
    private final ObjectMapper mapper = new ObjectMapper().enable(SerializationFeature.INDENT_OUTPUT);

    public TrackService(AppPaths paths) {
        this.logFile = paths.logFile();
    }

    /** @throws IOException neu file log hong / khong doc duoc */
    public synchronized List<Map<String, Object>> readLogs() throws IOException {
        if (!Files.exists(logFile)) return new ArrayList<>();
        String content = Files.readString(logFile, StandardCharsets.UTF_8);
        if (content.isBlank()) return new ArrayList<>();
        return mapper.readValue(content, new TypeReference<>() {});
    }

    public synchronized void saveLog(Map<String, Object> entry) throws IOException {
        List<Map<String, Object>> logs;
        try {
            logs = readLogs();
        } catch (IOException e) {
            log.error("Error reading log file", e);
            logs = new ArrayList<>();
        }
        logs.add(entry);
        // Chi giu LOG_MAX dong moi nhat, khong thi file (va RAM moi lan doc no) phinh mai
        if (logs.size() > LOG_MAX) logs = new ArrayList<>(logs.subList(logs.size() - LOG_MAX, logs.size()));
        write(logs);
    }

    public synchronized void clear() throws IOException {
        write(List.of());
    }

    private void write(List<Map<String, Object>> logs) throws IOException {
        Files.createDirectories(logFile.getParent());
        Files.writeString(logFile, mapper.writeValueAsString(logs), StandardCharsets.UTF_8);
    }
}
