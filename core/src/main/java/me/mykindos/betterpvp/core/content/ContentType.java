package me.mykindos.betterpvp.core.content;

import lombok.Getter;

import java.util.Optional;

/**
 * The kinds of authored content the admin console produces and the game
 * consumes. The {@link #key} matches the {@code content.type} column value.
 */
@Getter
public enum ContentType {

    LOOT_TABLE("loot_table"),
    SAGA("saga"),
    QUEST("quest"),
    CONVERSATION("conversation"),
    CINEMATIC("cinematic");

    private final String key;

    ContentType(String key) {
        this.key = key;
    }

    public static Optional<ContentType> fromKey(String key) {
        for (ContentType type : values()) {
            if (type.key.equals(key)) {
                return Optional.of(type);
            }
        }
        return Optional.empty();
    }
}
