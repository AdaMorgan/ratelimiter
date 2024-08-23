package com.github.adamorgan.limiter;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;

import java.io.IOException;

public enum ErrorResponse {
    USER_RATE_LIMIT("You are being rate limited."),
    RESOURCES_RATE_LIMIT("The resource is being rate limited."),
    GLOBAL_RATE_LIMIT("You are being rate limited.", true);

    private final String message;
    private final boolean isGlobal;

    ErrorResponse(String message) {
        this(message, false);
    }

    ErrorResponse(String message, boolean isGlobal) {
        this.message = message;
        this.isGlobal = isGlobal;
    }

    public String getMessage() {
        return message;
    }

    public boolean isGlobal() {
        return isGlobal;
    }

    public String toJson(double retryAfter) {
        ObjectMapper mapper = new ObjectMapper();
        ObjectNode node = mapper.createObjectNode()
                .put("message", this.message)
                .put("retry_after", retryAfter)
                .put("global", this.isGlobal);

        try {
            return mapper.writerWithDefaultPrettyPrinter().writeValueAsString(node);
        } catch (IOException error) {
            throw new RuntimeException(error);
        }
    }
}
