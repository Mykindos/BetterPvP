package me.mykindos.betterpvp.core.framework.net;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.Map;

/**
 * A {@link BusMessage} as it travels through Redis.
 * <p>
 * Separate from {@code BusMessage} so the wire format can change without changing what callers write, and so the
 * reply flag stays out of the payload where a caller could set it.
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
class RedisEnvelope {

    private String topic;
    private String origin;
    private String correlationId;
    private boolean reply;
    private Map<String, String> payload;
}
