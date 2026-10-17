package com.cinema.booking.common;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;

public final class JsonUtils {
    private static final ObjectMapper OBJECT_MAPPER = new ObjectMapper()
            .findAndRegisterModules()
            .disable(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS)
            .disable(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES);

    private JsonUtils() {
    }

    public static String toJson(Object object) {
        try {
            return OBJECT_MAPPER.writeValueAsString(object);
        } catch (JsonProcessingException e) {
            throw new IllegalStateException(
                    "Cannot convert object to JSON", e);
        }
    }

    public static <T> T fromJson(String json, Class<T> c) {
        try {
            return OBJECT_MAPPER.readValue(json, c);
        } catch (JsonProcessingException e) {
            throw new IllegalStateException(
                    "Cannot convert JSON to " + c.getSimpleName(), e);
        }
    }

    public static JsonNode readTree(String json) {
        try {
            return OBJECT_MAPPER.readTree(json);
        } catch (JsonProcessingException e) {
            throw new IllegalStateException("Cannot parse JSON", e);
        }
    }

    public static <T> T treeToValue(JsonNode node, Class<T> c) {
        try {
            // Biến một node bên trong cây thành đối tượng Java
            return OBJECT_MAPPER.treeToValue(node, c);
        } catch (JsonProcessingException e) {
            throw new IllegalStateException(
                    "Cannot convert JSON tree to " + c.getSimpleName(), e);
        }
    }
}
