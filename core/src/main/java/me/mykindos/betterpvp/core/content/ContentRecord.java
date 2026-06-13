package me.mykindos.betterpvp.core.content;

import lombok.Value;
import org.jetbrains.annotations.Nullable;

/**
 * A row of authored content as the game sees it. {@link #publishedJson} is the
 * {@code published} JSONB blob (the live, game-facing definition) — never the
 * editor draft. Null published content is filtered out before reaching here.
 */
@Value
public class ContentRecord {

    String id;
    ContentType type;
    String name;
    @Nullable String publishedJson;
    int version;
}
