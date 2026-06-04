package me.mykindos.betterpvp.clans.world.resource.event;

import lombok.EqualsAndHashCode;
import lombok.Getter;
import me.mykindos.betterpvp.clans.world.resource.ResourceNodeProp;
import me.mykindos.betterpvp.core.framework.events.CustomEvent;
import org.bukkit.Material;
import org.bukkit.block.Block;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.Nullable;

/**
 * Fired after a resource node has been successfully harvested. The decoupled
 * {@code @PluginAdapter("Progression")} bridge listens and awards the matching profession XP — the same pattern the
 * old Fields {@code FieldsInteractableUseEvent} used to drive mining XP.
 * <p>
 * {@link #harvestedMaterial} is the block's material <em>before</em> the archetype transformed it (e.g. the ore, not
 * the resulting stone), captured so the mining bridge can look up the correct per-ore XP.
 */
@Getter
@EqualsAndHashCode(callSuper = true)
public class ResourceHarvestEvent extends CustomEvent {

    private final Player player;
    private final ResourceNodeProp node;
    private final @Nullable String profession;
    private final @Nullable Block block;
    private final @Nullable Material harvestedMaterial;

    public ResourceHarvestEvent(Player player, ResourceNodeProp node, @Nullable String profession,
                                @Nullable Block block, @Nullable Material harvestedMaterial) {
        this.player = player;
        this.node = node;
        this.profession = profession;
        this.block = block;
        this.harvestedMaterial = harvestedMaterial;
    }
}
