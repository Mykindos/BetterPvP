package me.mykindos.betterpvp.core.item.component.impl.ability.event;

import lombok.Getter;
import me.mykindos.betterpvp.core.client.Client;
import me.mykindos.betterpvp.core.framework.events.CustomCancellableEvent;
import me.mykindos.betterpvp.core.item.component.impl.ability.ItemAbility;
import org.bukkit.entity.Player;

/**
 * Called before an item ability is attempted to execute.
 * <br><br>
 * Do NOT use this event to listen if the ability has been executed, as this is called
 * before the ability has the opportunity to reject its own execution via condition checks.
 * <br><br>
 * Use this only as a way to cancel the ability's execution entirely.
 * @see PlayerItemAbilityEvent
 */
@Getter
public class PlayerPreItemAbilityEvent extends CustomCancellableEvent {

    private final Client client;
    private final Player player;
    private final ItemAbility ability;

    public PlayerPreItemAbilityEvent(Client client, ItemAbility ability) {
        this.client = client;
        this.player = client.getGamer().getPlayer();
        this.ability = ability;
    }
}
