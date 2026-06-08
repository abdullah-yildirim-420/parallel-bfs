package edu.gazi.ceng479.bfs.viz.store;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import edu.gazi.ceng479.bfs.bench.Records.AggRecord;
import edu.gazi.ceng479.bfs.viz.event.Event;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;

/**
 * Exports benchmark aggregates to JSON for portability and the {@code /api/export}
 * endpoint (design.md §20.1, §18.4). Jackson serializes records out of the box.
 */
public final class JsonExporter {

    private static final ObjectMapper MAPPER = new ObjectMapper()
            .findAndRegisterModules();

    private JsonExporter() {
    }

    public static void writeAgg(Path path, List<AggRecord> rows) throws IOException {
        Files.createDirectories(path.toAbsolutePath().getParent());
        MAPPER.writerWithDefaultPrettyPrinter().writeValue(path.toFile(), rows);
    }

    public static String toJson(Object value) {
        try {
            return MAPPER.writeValueAsString(value);
        } catch (Exception e) {
            throw new RuntimeException("JSON serialization failed", e);
        }
    }

    /**
     * Serialize an {@link Event} to a JSON object that includes a {@code type}
     * discriminator field (design.md §18.4 envelope), since record components alone
     * don't carry the {@code type()} method value.
     */
    public static String eventJson(Event e) {
        ObjectNode node = MAPPER.valueToTree(e);
        node.put("type", e.type());
        return node.toString();
    }
}
