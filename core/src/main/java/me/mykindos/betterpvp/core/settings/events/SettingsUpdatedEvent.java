package me.mykindos.betterpvp.core.settings.events;

import lombok.Data;
import lombok.EqualsAndHashCode;
import me.mykindos.betterpvp.core.client.Client;
import me.mykindos.betterpvp.core.framework.events.CustomEvent;
import org.bukkit.entity.Player;

@EqualsAndHashCode(callSuper = true)
@Data
public class SettingsUpdatedEvent extends CustomEvent {

    private final Player player;
    private final Client client;
    private final String setting;

}
