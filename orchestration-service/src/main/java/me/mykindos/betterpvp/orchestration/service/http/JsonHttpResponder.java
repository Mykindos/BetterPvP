package me.mykindos.betterpvp.orchestration.service.http;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.sun.net.httpserver.HttpExchange;
import me.mykindos.betterpvp.orchestration.json.OrchestrationObjectMapperFactory;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;

final class JsonHttpResponder {

    private final ObjectMapper objectMapper = OrchestrationObjectMapperFactory.create();

    public <T> T readBody(HttpExchange exchange, Class<T> type) throws IOException {
        try (InputStream stream = exchange.getRequestBody()) {
            return objectMapper.readValue(stream, type);
        }
    }

    public void writeJson(HttpExchange exchange, int statusCode, Object body) throws IOException {
        final byte[] response = objectMapper.writeValueAsBytes(body);
        exchange.getResponseHeaders().set("Content-Type", "application/json");
        exchange.sendResponseHeaders(statusCode, response.length);
        try (OutputStream outputStream = exchange.getResponseBody()) {
            outputStream.write(response);
        }
    }

    public void writeEmpty(HttpExchange exchange, int statusCode) throws IOException {
        exchange.sendResponseHeaders(statusCode, -1);
        exchange.close();
    }

    public void writeError(HttpExchange exchange, int statusCode, String message) throws IOException {
        writeJson(exchange, statusCode, new ErrorPayload(message));
    }

    record ErrorPayload(String message) {
    }
}
