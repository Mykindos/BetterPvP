package me.mykindos.betterpvp.clans.world.camp.resource;

import lombok.EqualsAndHashCode;
import lombok.Getter;
import me.mykindos.betterpvp.core.framework.events.CustomEvent;
import me.mykindos.betterpvp.core.world.site.SiteKey;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;

import java.util.Collections;
import java.util.EnumMap;
import java.util.Map;

/** A member put resources into a camp's balance at a resource chest. */
@Getter
@EqualsAndHashCode(callSuper = true)
public class ResourcesDepositedEvent extends CustomEvent {

    private final SiteKey site;
    private final Player player;
    private final Map<ResourceKind, Integer> amounts;

    public ResourcesDepositedEvent(@NotNull SiteKey site, @NotNull Player player,
                                   @NotNull Map<ResourceKind, Integer> amounts) {
        this.site = site;
        this.player = player;
        this.amounts = Collections.unmodifiableMap(new EnumMap<>(amounts));
    }
}
