package com.configsystem.util;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.*;

/**
 * Jackson utility for walking the config_json tree.
 * Used for extracting leaf nodes and their data types.
 */
@Slf4j
@Component
public class JsonPathUtil {

    private final ObjectMapper objectMapper;

    public JsonPathUtil(ObjectMapper objectMapper) {
        this.objectMapper = objectMapper;
    }

    /**
     * Iteratively traverses the JSON string and collects all leaf nodes.
     * Used by seedAll() to populate json_metadata.
     *
     * @param jsonString  raw JSONB column value from merchants table
     * @return            list of (path, type) leaf pairs
     */
    public List<LeafNode> extractLeafNodes(String jsonString) {
        Objects.requireNonNull(jsonString, "JSON string must not be null");
        JsonNode root;
        try {
            root = objectMapper.readTree(jsonString);
        } catch (Exception e) {
            throw new IllegalArgumentException("Malformed JSON: " + e.getMessage(), e);
        }

        List<LeafNode> result = new ArrayList<>();
        Deque<Frame> stack = new ArrayDeque<>();
        stack.push(new Frame("", root));

        while (!stack.isEmpty()) {
            Frame f = stack.pop();
            JsonNode node = f.node();

            if (node.isObject()) {
                Iterator<Map.Entry<String, JsonNode>> fields = node.fields();
                while (fields.hasNext()) {
                    Map.Entry<String, JsonNode> e = fields.next();
                    String child = f.path().isEmpty() ? e.getKey() : f.path() + "." + e.getKey();
                    stack.push(new Frame(child, e.getValue()));
                }
            } else if (node.isArray()) {
                for (int i = 0; i < node.size(); i++) {
                    stack.push(new Frame(f.path() + "[" + i + "]", node.get(i)));
                }
            } else {
                result.add(new LeafNode(f.path(), detectType(node)));
            }
        }

        log.debug("Extracted {} leaf nodes", result.size());
        return result;
    }

    private String detectType(JsonNode node) {
        if (node.isNull())    return "NULL";
        if (node.isBoolean()) return "BOOLEAN";
        if (node.isNumber())  return "NUMBER";
        return "STRING";
    }

    public  record LeafNode(String path, String type) {}
    private record Frame(String path, JsonNode node) {}
}
