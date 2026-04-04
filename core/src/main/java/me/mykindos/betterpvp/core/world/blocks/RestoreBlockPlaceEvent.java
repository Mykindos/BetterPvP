package me.mykindos.betterpvp.core.world.blocks;

import lombok.Getter;
import me.mykindos.betterpvp.core.framework.events.CustomCancellableEvent;
import org.bukkit.Material;
import org.bukkit.block.Block;
import org.bukkit.block.data.BlockData;
import org.bukkit.entity.LivingEntity;
import org.bukkit.event.HandlerList;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

@Getter
public class RestoreBlockPlaceEvent extends CustomCancellableEvent {

    private static final HandlerList handlers = new HandlerList();

    @Nullable
    private final LivingEntity summoner;
    private final Block block;
    private final BlockData blockData;
    private final Material newMaterial;
    private final long expiry;
    private final boolean force;
    @Nullable
    private final String label;

    public RestoreBlockPlaceEvent(@Nullable LivingEntity summoner, @NotNull Block block, @NotNull BlockData blockData,
                                  @NotNull Material newMaterial, long expiry, boolean force, @Nullable String label) {
        this.summoner = summoner;
        this.block = block;
        this.blockData = blockData;
        this.newMaterial = newMaterial;
        this.expiry = expiry;
        this.force = force;
        this.label = label;
    }

    @Override
    public @NotNull HandlerList getHandlers() {
        return handlers;
    }

    public static HandlerList getHandlerList() {
        return handlers;
    }
}
