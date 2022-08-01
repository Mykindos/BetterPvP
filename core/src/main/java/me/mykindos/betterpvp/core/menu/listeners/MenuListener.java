package me.mykindos.betterpvp.core.menu.listeners;

import com.google.inject.Inject;
import me.mykindos.betterpvp.core.cooldowns.CooldownManager;
import me.mykindos.betterpvp.core.listener.BPvPListener;
import me.mykindos.betterpvp.core.menu.Button;
import me.mykindos.betterpvp.core.menu.Menu;
import me.mykindos.betterpvp.core.menu.MenuManager;
import me.mykindos.betterpvp.core.menu.events.ButtonPostClickEvent;
import me.mykindos.betterpvp.core.menu.events.ButtonPreClickEvent;
import me.mykindos.betterpvp.core.menu.interfaces.IRefreshingMenu;
import me.mykindos.betterpvp.core.utilities.UtilServer;
import net.kyori.adventure.text.serializer.plain.PlainTextComponentSerializer;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
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

                    if (cooldownManager.add(player, "Button Click", button.getClickCooldown(), false)) {
                        ButtonPreClickEvent buttonClickEvent = new ButtonPreClickEvent(player, menu, button, event.getClick(), event.getSlot());
                        UtilServer.callEvent(buttonClickEvent);
                        if (!buttonClickEvent.isCancelled()) {
                            buttonClickEvent.getButton().onClick(player, event.getClick());
                            UtilServer.callEvent(new ButtonPostClickEvent(player, menu, button));

                        }

                    }

                }
            });

        }
    }

    @EventHandler
    public void handleRefreshingMenus(ButtonPostClickEvent event) {
        if(event.getMenu() instanceof IRefreshingMenu refreshingMenu){
            event.getMenu().getButtons().clear();
            refreshingMenu.refresh();
        }
    }

}
