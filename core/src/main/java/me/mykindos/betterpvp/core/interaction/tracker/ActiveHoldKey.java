package me.mykindos.betterpvp.core.interaction.tracker;

import me.mykindos.betterpvp.core.interaction.chain.InteractionChainNode;

import java.util.UUID;

/**
 * Key for tracking active HOLD_RIGHT_CLICK executions.
 * Unique per actor and chain node (uses node reference identity).
 */
public record ActiveHoldKey(UUID actorId, InteractionChainNode node) {}
