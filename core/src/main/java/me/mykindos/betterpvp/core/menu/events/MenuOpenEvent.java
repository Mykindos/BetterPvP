package me.mykindos.betterpvp.core.menu.events;

import lombok.Data;
import lombok.EqualsAndHashCode;
import me.mykindos.betterpvp.core.framework.events.CustomEvent;
import me.mykindos.betterpvp.core.menu.Menu;
import org.bukkit.entity.Player;

@EqualsAndHashCode(callSuper = true)
@Data
public class MenuOpenEvent extends CustomEvent {

    private final Player player;
    private final Menu menu;

}