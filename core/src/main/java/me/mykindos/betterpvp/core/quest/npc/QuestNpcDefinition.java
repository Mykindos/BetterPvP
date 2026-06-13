package me.mykindos.betterpvp.core.quest.npc;

import lombok.Value;
import org.jetbrains.annotations.Nullable;

/**
 * A panel-authored quest-giver NPC. Placement comes from a Mapper PointRegion
 * named with {@link #id}; this carries only the appearance and identity — the NPC
 * starts no content of its own (quests auto-start from their own root objective).
 *
 * <p>{@link #source} is either {@code factory} (spawn via a registered
 * {@link me.mykindos.betterpvp.core.scene.SceneObjectFactory} + {@link #type},
 * like {@code /npc spawn}) or {@code human} (a {@code HumanNPC} with a skin).
 */
@Value
public class QuestNpcDefinition {
    String id;
    String displayName;
    String source;                // factory | human
    @Nullable String factory;
    @Nullable String type;
    @Nullable String skinValue;
    @Nullable String skinSignature;

    public boolean isHuman() {
        return !"factory".equalsIgnoreCase(source);
    }
}
