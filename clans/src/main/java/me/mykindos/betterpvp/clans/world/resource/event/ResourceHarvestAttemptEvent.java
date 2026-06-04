package me.mykindos.betterpvp.clans.world.resource.event;

import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.Setter;
import me.mykindos.betterpvp.clans.world.resource.ResourceNodeProp;
import me.mykindos.betterpvp.core.framework.events.CustomEvent;
import net.kyori.adventure.text.Component;
import org.bukkit.entity.Player;
import org.bukkit.event.Cancellable;
import org.jetbrains.annotations.Nullable;

/**
 * Fired before a resource node is harvested, as the profession-level gate. The decoupled
 * {@code @PluginAdapter("Progression")} bridge listens and {@link #setCancelled cancels} (with a
 * {@link #setDenialMessage denial message}) when the player is below {@link #getRequiredLevel}. If Progression is
 * absent nothing cancels and harvesting is ungated (fail-open).
 */
@Getter
@EqualsAndHashCode(callSuper = true)
public class ResourceHarvestAttemptEvent extends CustomEvent implements Cancellable {

    private final Player player;
    private final ResourceNodeProp node;
    private final @Nullable String profession;
    private final int requiredLevel;

    @Setter
    private boolean cancelled;
    @Setter
    private @Nullable Component denialMessage;

    public ResourceHarvestAttemptEvent(Player player, ResourceNodeProp node, @Nullable String profession, int requiredLevel) {
        this.player = player;
        this.node = node;
        this.profession = profession;
        this.requiredLevel = requiredLevel;
    }
}
