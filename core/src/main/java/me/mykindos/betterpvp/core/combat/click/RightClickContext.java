package me.mykindos.betterpvp.core.combat.click;

import lombok.AllArgsConstructor;
import lombok.Getter;
import me.mykindos.betterpvp.core.client.gamer.Gamer;
import me.mykindos.betterpvp.core.combat.click.events.RightClickEvent;
import org.bukkit.inventory.ItemStack;

@AllArgsConstructor
@Getter
public class RightClickContext {

    private final Gamer gamer;
    private final long time = System.currentTimeMillis();
    private final RightClickEvent event;
    private final ItemStack itemStack;

}
