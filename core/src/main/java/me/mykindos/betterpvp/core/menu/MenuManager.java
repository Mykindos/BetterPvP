package me.mykindos.betterpvp.core.menu;

import com.google.inject.Singleton;
import me.mykindos.betterpvp.core.framework.manager.Manager;
import org.bukkit.entity.Player;

import java.util.HashMap;
import java.util.Optional;

@Singleton
public class MenuManager extends Manager<HashMap<String, Menu>> {

    public boolean isMenu(String title) {
        return objects.entrySet().stream()
                .anyMatch(entry -> entry.getValue().entrySet().stream()
                        .anyMatch(menu -> menu.getValue().getTitle().equalsIgnoreCase(title)));
    }

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
        menusOptional.ifPresent(menus -> menus.put(menu.getTitle(), menu));
    }

    public void openMenu(Player player, Menu menu) {
        player.openInventory(menu.getInventory());
        addMenu(player, menu);
    }
}
