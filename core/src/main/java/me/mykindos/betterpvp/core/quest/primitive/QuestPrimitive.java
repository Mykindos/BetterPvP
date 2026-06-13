package me.mykindos.betterpvp.core.quest.primitive;

import lombok.Value;

import java.util.Map;

/**
 * A self-describing quest primitive (trigger / condition / action / requirement
 * / reward). The game code is the source of truth: these descriptors are
 * exported to the {@code quest_primitives} table so the admin console renders
 * matching inspector forms. {@link #paramSchema} maps a parameter name to a
 * descriptor map (e.g. {@code {"type":"item_ref","required":true}}).
 */
@Value
public class QuestPrimitive {

    String id;
    String category; // trigger | condition | action | requirement | reward
    String label;
    Map<String, Object> paramSchema;
}
