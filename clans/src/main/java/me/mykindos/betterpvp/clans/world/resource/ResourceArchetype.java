package me.mykindos.betterpvp.clans.world.resource;

import dev.brauw.mapper.region.CuboidRegion;
import dev.brauw.mapper.region.Region;
import me.mykindos.betterpvp.core.framework.blockbreak.rule.BlockBreakRule;
import me.mykindos.betterpvp.core.world.zone.ZoneInteraction;
import org.bukkit.block.Block;
import org.bukkit.entity.Player;
import org.bukkit.event.player.PlayerFishEvent;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.Collection;
import java.util.List;
import java.util.Set;

/**
 * The behaviour strategy for one kind of resource node (tree felling, ore mining, fishing). There is exactly one
 * archetype instance per kind ({@code @Singleton}); the unbounded variety of actual nodes ("Willow Tree Lv67",
 * "Copper Mine Lv25") is pure data carried by {@link ResourceNodeProp}/{@link ResourceNodeDefinition}.
 * <p>
 * Lifecycle: {@link #onActivate} captures world state at load (e.g. snapshot ore blocks), {@link #tick} runs each
 * server tick via the node's scene behaviour (respawn), and {@link #onDeactivate} cleans up on reload. Harvesting is
 * routed by {@link ResourceNodeManager} through {@link #onHarvest} (block break/interact) or {@link #onFish}.
 */
public interface ResourceArchetype {

    /**
     * @return the archetype id, matched case-insensitively against {@code ResourceNodeDefinition#getArchetype()}
     */
    @NotNull String id();

    /**
     * @return the Mapper region type this archetype binds to. Area archetypes (ore, fishing) use {@code CUBOID};
     * trees use {@code PERSPECTIVE} (a point + yaw marking where and how the tree is placed). Default {@code CUBOID}.
     */
    default @NotNull Region.RegionType regionType() {
        return Region.RegionType.CUBOID;
    }

    /**
     * Resolves the cuboid used for the node's gate zone, label search, and label fallback. For cuboid archetypes this
     * is the matched region itself; perspective archetypes (tree) compute it from the placed schematic's footprint.
     *
     * @param definition the node definition (carries archetype config such as schematic names)
     * @param region     the matched Mapper region
     */
    default @NotNull CuboidRegion zoneBounds(@NotNull ResourceNodeDefinition definition, @NotNull Region region) {
        return (CuboidRegion) region;
    }

    /**
     * @return extra zone tags every node of this archetype carries (e.g. ore fields add {@code "fields"} so
     * {@code ClanManager#isFields} keeps working). Default none.
     */
    default @NotNull Set<String> zoneTags() {
        return Set.of();
    }

    /**
     * Captures any world state the node needs (called once when the node is spawned at load).
     */
    default void onActivate(@NotNull ResourceNodeProp node) {
    }

    /**
     * Releases per-node state (called when the node is removed on reload/shutdown).
     */
    default void onDeactivate(@NotNull ResourceNodeProp node) {
    }

    /**
     * Per-tick hook (respawn, ambient effects). Called every server tick via the node's scene behaviour.
     */
    default void tick(@NotNull ResourceNodeProp node) {
    }

    /**
     * A block break/interact inside the node's zone, already gated (the player meets the level and the attempt was not
     * cancelled). Implementations transform the block, schedule respawn, and award loot.
     *
     * @return true if the interaction was consumed as a harvest (suppresses the denial feedback)
     */
    default boolean onHarvest(@NotNull ResourceNodeProp node, @NotNull Player player, @Nullable Block block,
                              @NotNull ZoneInteraction interaction) {
        return false;
    }

    /**
     * A fishing catch whose hook is inside the node's zone, already gated. Implementations award the node's loot and
     * may cancel the vanilla catch.
     *
     * @return true if the catch was consumed as a harvest
     */
    default boolean onFish(@NotNull ResourceNodeProp node, @NotNull Player player, @NotNull PlayerFishEvent event) {
        return false;
    }

    /**
     * The points of {@code node} that are currently regrowing for {@code viewer}, so their remaining time can be shown
     * floating on the block.
     * <p>
     * Asking per viewer is what lets one method serve both kinds of node: an archetype with shared state ignores the
     * argument and answers the same for everyone, while a per-player one answers from that player's own state. The
     * caller never has to know which it is holding.
     * <p>
     * Points whose chain has {@link DegradeChain#showTimer()} turned off are left out here rather than filtered later:
     * whether a countdown is worth showing is a property of the chain, which the archetype can see and a renderer
     * cannot.
     *
     * @param viewer whose regrowth to report, or null to ask an archetype with shared state for everyone's
     * @return the regrowing points, or an empty collection for an archetype that has none to report
     */
    default @NotNull Collection<RespawnPoint> respawningPoints(@NotNull ResourceNodeProp node, @Nullable Player viewer) {
        return List.of();
    }

    /**
     * @return true if {@link #respawningPoints} gives different answers to different players, so each needs their own
     * countdown rather than one everybody shares
     */
    default boolean timersArePerPlayer() {
        return false;
    }

    /**
     * Break rules this archetype wants applied to {@code player} for as long as they stand inside {@code node}, on top
     * of whatever the node's own configuration contributes. Registered on zone entry and withdrawn on exit by
     * {@link NodeBreakRuleService}.
     * <p>
     * The returned rules are held per player, so an archetype whose state is per player can close over the player here
     * and answer from a matcher that only sees the block.
     *
     * @return the rules to register, or an empty list for an archetype that needs none
     */
    default @NotNull List<BlockBreakRule> breakRules(@NotNull ResourceNodeProp node, @NotNull Player player) {
        return List.of();
    }

    /**
     * @return true if {@code block} sits at an unbreakable terminal stage and the player should not be able to mine it
     * at all — {@link ResourceNodeManager} cancels the {@code BlockDamageEvent} so no cracking animation plays. Default
     * false.
     */
    default boolean isUnbreakable(@NotNull ResourceNodeProp node, @NotNull Block block) {
        return false;
    }
}
