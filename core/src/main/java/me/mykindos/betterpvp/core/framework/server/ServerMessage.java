package me.mykindos.betterpvp.core.framework.server;

import lombok.Builder;
import lombok.Data;
import lombok.extern.jackson.Jacksonized;

import java.util.HashMap;

@Jacksonized
@Data
@Builder
public class ServerMessage {

    private String channel;
    private String server;
    private String message;
    private HashMap<String, String> metadata;

    public static class ServerMessageBuilder {

        public ServerMessageBuilder metadata(HashMap<String, String> metadata) {
            this.metadata = metadata;
            return this;
        }

        public ServerMessageBuilder metadata(String key, String value) {
            if (this.metadata == null) {
                this.metadata = new HashMap<>();
            }
            this.metadata.put(key, value);
            return this;
        }


    }
}
