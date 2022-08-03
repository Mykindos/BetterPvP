package me.mykindos.betterpvp.core.client.properties;

import lombok.AllArgsConstructor;
import lombok.Getter;

@AllArgsConstructor
@Getter
public enum ClientProperty {

    CHAT_ENABLED("CHAT_ENABLED"),
    LUNAR("LUNAR");

    private final String key;

    @Override
    public String toString(){
        return key;
    }
}
