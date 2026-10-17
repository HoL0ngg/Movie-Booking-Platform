package com.cinema.booking.common;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;

public final class JsonUtils {
    private static final ObjectMapper OBJECT_MAPPER = new ObjectMapper();

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
}
