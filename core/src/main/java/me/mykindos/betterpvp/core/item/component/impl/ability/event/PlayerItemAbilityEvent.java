package me.mykindos.betterpvp.core.item.component.impl.ability.event;

import lombok.Getter;
import me.mykindos.betterpvp.core.framework.events.CustomCancellableEvent;
import me.mykindos.betterpvp.core.framework.events.CustomEvent;
import me.mykindos.betterpvp.core.item.component.impl.ability.ItemAbility;
import org.bukkit.entity.Player;

/**
 * Called after an item ability has been executed.
 * <br><br>
 * If you wish to cancel the ability, use {@link PlayerPreItemAbilityEvent} instead.
 *
 * @see PlayerPreItemAbilityEvent
 */
@Getter
public class PlayerItemAbilityEvent extends CustomEvent {

    private final Player player;
    private final ItemAbility ability;

    public PlayerItemAbilityEvent(Player player, ItemAbility ability) {
        this.player = player;
        this.ability = ability;
    }
}
