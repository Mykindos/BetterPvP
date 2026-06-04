package me.mykindos.betterpvp.clans.world.resource;

import dev.brauw.mapper.region.CuboidRegion;
import dev.brauw.mapper.region.Region;
import me.mykindos.betterpvp.core.world.zone.ZoneInteraction;
import org.bukkit.block.Block;
import org.bukkit.entity.Player;
import org.bukkit.event.player.PlayerFishEvent;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

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
     * @return true if {@code block} sits at an unbreakable terminal stage and the player should not be able to mine it
     * at all — {@link ResourceNodeManager} cancels the {@code BlockDamageEvent} so no cracking animation plays. Default
     * false.
     */
    default boolean isUnbreakable(@NotNull ResourceNodeProp node, @NotNull Block block) {
        return false;
    }
}
