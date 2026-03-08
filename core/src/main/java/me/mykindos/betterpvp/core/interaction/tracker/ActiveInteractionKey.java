package me.mykindos.betterpvp.core.interaction.tracker;

import me.mykindos.betterpvp.core.interaction.chain.InteractionChainNode;

import java.util.UUID;

/**
 * Key for tracking active interactions.
 * Unique per actor and chain node (uses node reference identity).
 */
public record ActiveInteractionKey(UUID actorId, InteractionChainNode node) {}
