package me.mykindos.betterpvp.core.quest.npc;

import lombok.Value;
import org.jetbrains.annotations.Nullable;

/**
 * Binds a spawned scene object to its quest-NPC identity, so {@code trigger.npc_interact}
 * ("Talk to NPC") objectives can match it. The NPC carries no content of its own — quests
 * declare their own entry NPC via their root objective and auto-start when it is recorded.
 */
@Value
public class QuestGiverBinding {
    /** The {@code quest_npcs.id} this scene object represents, or null for a manually-bound NPC. */
    @Nullable String npcId;
}
