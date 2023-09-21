package me.mykindos.betterpvp.core.menu;

import com.google.inject.Singleton;
import me.mykindos.betterpvp.core.framework.manager.Manager;
import me.mykindos.betterpvp.core.menu.events.MenuOpenEvent;
import me.mykindos.betterpvp.core.utilities.UtilServer;
import net.kyori.adventure.text.serializer.plain.PlainTextComponentSerializer;
import org.bukkit.entity.Player;

import java.util.HashMap;
import java.util.Optional;

@Singleton
public class MenuManager extends Manager<HashMap<String, Menu>> {

    public Optional<Menu> getMenu(Player player, String title) {
        Optional<HashMap<String, Menu>> menusOptional = getObject(player.getUniqueId().toString());
        if (menusOptional.isPresent()) {
            HashMap<String, Menu> menus = menusOptional.get();
            return Optional.ofNullable(menus.getOrDefault(title, null));
        }
        return Optional.empty();
    }

    public void addMenu(Player player, Menu menu) {
        Optional<HashMap<String, Menu>> menusOptional = getObject(player.getUniqueId().toString()).or(() -> {
            HashMap<String, Menu> menus = new HashMap<>();
            objects.put(player.getUniqueId().toString(), menus);
            return Optional.of(menus);
        });
        menusOptional.ifPresent(menus -> menus.put(PlainTextComponentSerializer.plainText().serialize(menu.getTitle()), menu));
    }

    public static void openMenu(Player player, Menu menu) {
        MenuOpenEvent menuOpenEvent = new MenuOpenEvent(player, menu);
        UtilServer.callEvent(menuOpenEvent);
    }
}
