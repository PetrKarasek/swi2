package cz.osu.swi22025.desktop;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import cz.osu.swi22025.model.json.PayloadMessage;

import java.io.IOException;
import java.nio.file.*;
import java.util.ArrayList;
import java.util.List;

public class PendingStore {
    private static final Path FILE = Paths.get(System.getProperty("user.home"), ".swi2_pending.json");
    private final ObjectMapper mapper = new ObjectMapper();

    public List<PayloadMessage> load() {
        if (!Files.exists(FILE)) return new ArrayList<>();
        try {
            String json = Files.readString(FILE);
            if (json == null || json.isBlank()) return new ArrayList<>();
            return mapper.readValue(json, new TypeReference<List<PayloadMessage>>() {});
        } catch (Exception e) {
            return new ArrayList<>();
        }
    }

    public void save(List<PayloadMessage> msgs) {
        try {
            Files.writeString(FILE, mapper.writeValueAsString(msgs),
                    StandardOpenOption.CREATE, StandardOpenOption.TRUNCATE_EXISTING);
        } catch (IOException ignored) {}
    }

    public void clear() {
        try { Files.deleteIfExists(FILE); } catch (IOException ignored) {}
    }
}
