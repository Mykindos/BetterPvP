package me.mykindos.betterpvp.core.menu.listeners;

import com.google.inject.Inject;
import me.mykindos.betterpvp.core.cooldowns.CooldownManager;
import me.mykindos.betterpvp.core.listener.BPvPListener;
import me.mykindos.betterpvp.core.menu.Button;
import me.mykindos.betterpvp.core.menu.Menu;
import me.mykindos.betterpvp.core.menu.MenuManager;
import me.mykindos.betterpvp.core.menu.events.ButtonClickEvent;
import net.kyori.adventure.text.serializer.plain.PlainTextComponentSerializer;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.player.PlayerQuitEvent;

import java.util.Optional;

@BPvPListener
public class MenuListener implements Listener {

    private final MenuManager menuManager;
    private final CooldownManager cooldownManager;

    @Inject
    public MenuListener(MenuManager menuManager, CooldownManager cooldownManager) {
        this.menuManager = menuManager;
        this.cooldownManager = cooldownManager;
    }

    @EventHandler
    public void onPlayerQuit(PlayerQuitEvent event) {
        menuManager.getObjects().remove(event.getPlayer().getUniqueId().toString());
    }

    @EventHandler
    public void onInventoryClick(InventoryClickEvent event) {

        if (event.getWhoClicked() instanceof Player player) {
            String menuTitle = PlainTextComponentSerializer.plainText().serialize(event.getView().title());
            Optional<Menu> menuOptional = menuManager.getMenu(player, menuTitle);
            menuOptional.ifPresent(menu -> {
                event.setCancelled(true);
                Button button = menu.getButton(event.getCurrentItem());
                if (button != null) {

                    if (cooldownManager.add(player, "Button Click", 0.05, false)) {
                        Bukkit.getPluginManager().callEvent(new ButtonClickEvent(player, menu, button, event.getClick(), event.getSlot()));

                    }

                }
            });

        }
    }

    @EventHandler (priority = EventPriority.MONITOR)
    public void onButtonClick(ButtonClickEvent event){
        if(event.isCancelled()) return;

        event.getButton().onClick(event.getPlayer());
    }

}
