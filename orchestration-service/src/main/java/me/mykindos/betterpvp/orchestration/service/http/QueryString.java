package me.mykindos.betterpvp.orchestration.service.http;

import java.net.URLDecoder;
import java.nio.charset.StandardCharsets;
import java.util.LinkedHashMap;
import java.util.Map;

final class QueryString {

    private QueryString() {
    }

    static Map<String, String> parse(String rawQuery) {
        final Map<String, String> values = new LinkedHashMap<>();
        if (rawQuery == null || rawQuery.isBlank()) {
            return values;
        }

        final String[] pairs = rawQuery.split("&");
        for (String pair : pairs) {
            final int separator = pair.indexOf('=');
            if (separator < 0) {
                values.put(decode(pair), "");
                continue;
            }

            values.put(decode(pair.substring(0, separator)), decode(pair.substring(separator + 1)));
        }

        return values;
    }

    private static String decode(String value) {
        return URLDecoder.decode(value, StandardCharsets.UTF_8);
    }
}
